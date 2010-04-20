"""
AMAK: 20050515: This module is a brand new test_select module, which gives much wider coverage.
"""

import errno
import time
from test import test_support
import unittest

import socket
import select

SERVER_ADDRESS = ("localhost", 54321)

DATA_CHUNK_SIZE = 1000
DATA_CHUNK = "." * DATA_CHUNK_SIZE

#
# The timing of these tests depends on the how the unerlying OS socket library
# handles buffering. These values may need tweaking for different platforms
#
# The fundamental problem is that there is no reliable way to fill a socket with bytes
#

if test_support.is_jython:
    SELECT_TIMEOUT = 0
else:
    # zero select timeout fails these tests on cpython (on windows 2003 anyway)
    SELECT_TIMEOUT = 0.001

READ_TIMEOUT = 5

class AsynchronousServer:

    def __init__(self):
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setblocking(0)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind(SERVER_ADDRESS)
        self.server_socket.listen(5)
        try:
            self.server_socket.accept()
        except socket.error:
            pass

    def select_acceptable(self):
        return select.select([self.server_socket], [self.server_socket], [], SELECT_TIMEOUT)[0]

    def verify_acceptable(self):
        start = time.time()
        while True:
            if self.select_acceptable():
                return
            elif (time.time() - start) > READ_TIMEOUT:
                raise Exception('Server socket did not accept in time')
            time.sleep(0.1)

    def verify_not_acceptable(self):
        assert not self.select_acceptable(), "Server socket should not be acceptable"

    def accept(self):
        self.verify_acceptable()
        new_socket, address = self.server_socket.accept()
        return AsynchronousHandler(new_socket)

    def close(self):
        self.server_socket.close()


class AsynchronousHandler:

    def __init__(self, new_socket):
        self.socket = new_socket
        self.socket.setblocking(0)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    def write(self):
        """
        Writes on this channel until select no longer reports it as writable.

        Returns the number of bytes written
        """
        total_bytes = 0
        while 1:
            try:
                if self.select_writable():
                    bytes_sent = self.socket.send(DATA_CHUNK)
                    total_bytes += bytes_sent
                else:
                    return total_bytes
            except socket.error, se:
                if se.value == 10035:
                    continue
                raise se

    def read(self, expected):
        """
        Reads expected bytes from this socket

        An Exception is raised if expected bytes aren't read before READ_TIMEOUT
        is reached.
        """
        results = ""
        start = time.time()
        while 1:
            if self.select_readable():
                recvd_bytes = self.socket.recv(expected - len(results))
                if len(recvd_bytes):
                    results += recvd_bytes
                if len(results) == expected:
                    return results
            else:
                stop = time.time()
                if (stop - start) > READ_TIMEOUT:
                    raise Exception("Got %d bytes but %d bytes were written."  %
                                    (len(results), expected))

    def select_readable(self):
        return select.select([self.socket], [], [], SELECT_TIMEOUT)[0]

    def verify_readable(self):
        assert self.select_readable(), "Socket should be ready for reading"

    def verify_not_readable(self):
        assert not self.select_readable(), "Socket should not be ready for reading"

    def select_writable(self):
        return select.select([], [self.socket], [], SELECT_TIMEOUT)[1]

    def verify_writable(self):
        assert self.select_writable(), "Socket should be ready for writing"

    def verify_not_writable(self):
        assert not self.select_writable(), "Socket should not be ready for writing"

    def verify_only_writable(self):
        self.verify_writable()
        self.verify_not_readable()

    def close(self):
        self.socket.close()

class AsynchronousClient(AsynchronousHandler):

    def __init__(self):
        AsynchronousHandler.__init__(self, socket.socket(socket.AF_INET, socket.SOCK_STREAM))
        self.connected = 0

    def start_connect(self):
        result = self.socket.connect_ex(SERVER_ADDRESS)
        if result == errno.EISCONN:
            self.connected = 1
        else:
            assert result == errno.EINPROGRESS

    def finish_connect(self):
        if self.connected:
            return
        start = time.time()
        while True:
            self.start_connect()
            if self.connected:
                break
            elif (time.time() - start) > READ_TIMEOUT:
                raise Exception('Client socket incomplete connect')
            time.sleep(0.1)

class TestSelectOnAccept(unittest.TestCase):
    def setUp(self):
        self.server = AsynchronousServer()
        self.client = AsynchronousClient()
        self.handler = None

    def testSelectOnAccept(self):
        self.server.verify_not_acceptable()
        self.client.start_connect()
        self.server.verify_acceptable()
        self.handler = self.server.accept()
        self.client.finish_connect()
        self.server.verify_not_acceptable()

    def tearDown(self):
        self.client.close()
        if self.handler:
            self.handler.close()
        self.server.close()

class TestSelect(unittest.TestCase):
    def setUp(self):
        self.server = AsynchronousServer()
        self.client = AsynchronousClient()
        self.client.start_connect()
        self.handler = self.server.accept()
        self.client.finish_connect()

    def tearDown(self):
        self.client.close()
        self.handler.close()
        self.server.close()

    def testClientOut(self):
        self.client.verify_only_writable()
        self.handler.verify_only_writable()

        written = self.client.write()
        self.handler.verify_readable()

        self.handler.read(written/2)
        self.handler.verify_readable()

        self.handler.read(written/2)
        self.handler.verify_not_readable()

    def testHandlerOut(self):
        written = self.handler.write()
        self.client.verify_readable()

        self.client.read(written/2)
        self.client.verify_readable()

        self.client.read(written/2)
        self.client.verify_not_readable()

    def testBothOut(self):
        client_written = self.client.write()
        handler_written = self.handler.write()
        self.client.verify_readable()
        self.handler.verify_readable()

        self.client.read(handler_written/2)
        self.handler.read(client_written/2)
        self.client.verify_readable()
        self.handler.verify_readable()

        self.client.read(handler_written/2)
        self.handler.read(client_written/2)
        self.client.verify_only_writable()
        self.handler.verify_only_writable()

def test_main():
    tests = [TestSelect, TestSelectOnAccept]
    suites = [unittest.makeSuite(klass, 'test') for klass in tests]
    test_support.run_suite(unittest.TestSuite(suites))    

if __name__ == "__main__":
    test_main()
