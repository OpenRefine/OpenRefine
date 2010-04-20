"""
AMAK: 20050515: This module is the test_select.py from cpython 2.4, ported to jython + unittest
"""
import errno
import os
import select
import socket
import sys
import test_socket
import unittest
from test import test_support

HOST = test_socket.HOST
PORT = test_socket.PORT + 100

class SelectWrapper:

    def __init__(self):
        self.read_fds = []
        self.write_fds = []
        self.oob_fds = []
        self.timeout = None

    def add_read_fd(self, fd):
        self.read_fds.append(fd)

    def add_write_fd(self, fd):
        self.write_fds.append(fd)

    def add_oob_fd(self, fd):
        self.oob_fds.append(fd)

    def set_timeout(self, timeout):
        self.timeout = timeout

class PollWrapper:

    def __init__(self):
        self.timeout = None
        self.poll_object = select.poll()

    def add_read_fd(self, fd):
        self.poll_object.register(fd, select.POLL_IN)

    def add_write_fd(self, fd):
        self.poll_object.register(fd, select.POLL_OUT)

    def add_oob_fd(self, fd):
        self.poll_object.register(fd, select.POLL_PRI)

class TestSelectInvalidParameters(unittest.TestCase):

    def testBadSelectSetTypes(self):
        # Test some known error conditions
        for bad_select_set in [None, 1,]:
            for pos in range(2): # OOB not supported on Java
                args = [[], [], []]
                args[pos] = bad_select_set
                try:
                    timeout = 0 # Can't wait forever
                    rfd, wfd, xfd = select.select(args[0], args[1], args[2], timeout)
                except (select.error, TypeError):
                    pass
                except Exception, x:
                    self.fail("Selecting on '%s' raised wrong exception %s" % (str(bad_select_set), str(x)))
                else:
                    self.fail("Selecting on '%s' should have raised TypeError" % str(bad_select_set))

    def testBadSelectableTypes(self):
        class Nope: pass

        class Almost1:
            def fileno(self):
                return 'fileno'

        class Almost2:
            def fileno(self):
                return 'fileno'

        # Test some known error conditions
        for bad_selectable in [None, 1, object(), Nope(), Almost1(), Almost2()]:
            try:
                timeout = 0 # Can't wait forever
                rfd, wfd, xfd = select.select([bad_selectable], [], [], timeout)
            except (TypeError, select.error), x:
                pass
            else:
                self.fail("Selecting on '%s' should have raised TypeError or select.error" % str(bad_selectable))

    def testInvalidTimeoutTypes(self):
        for invalid_timeout in ['not a number']:
            try:
                rfd, wfd, xfd = select.select([], [], [], invalid_timeout)
            except TypeError:
                pass
            else:
                self.fail("Invalid timeout value '%s' should have raised TypeError" % invalid_timeout)

    def testInvalidTimeoutValues(self):
        for invalid_timeout in [-1]:
            try:
                rfd, wfd, xfd = select.select([], [], [], invalid_timeout)
            except (ValueError, select.error):
                pass
            else:
                self.fail("Invalid timeout value '%s' should have raised ValueError or select.error" % invalid_timeout)

class TestSelectClientSocket(unittest.TestCase):

    def testUnconnectedSocket(self):
        sockets = [socket.socket(socket.AF_INET, socket.SOCK_STREAM) for x in range(5)]
        for pos in range(2): # OOB not supported on Java
            args = [[], [], []]
            args[pos] = sockets
            timeout = 0 # Can't wait forever
            rfd, wfd, xfd = select.select(args[0], args[1], args[2], timeout)
            for s in sockets:
                self.failIf(s in rfd)
                self.failIf(s in wfd)

class TestPollClientSocket(unittest.TestCase):

    def testEventConstants(self):
        for event_name in ['IN', 'OUT', 'PRI', 'ERR', 'HUP', 'NVAL', ]:
            self.failUnless(hasattr(select, 'POLL%s' % event_name))

    def testUnregisterRaisesKeyError(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        poll_object = select.poll()
        try:
            poll_object.unregister(s)
        except KeyError:
            pass
        else:
            self.fail("Unregistering socket that is not registered should have raised KeyError")

#
# using the test_socket thread based server/client management, for convenience.
#
class ThreadedPollClientSocket(test_socket.ThreadedTCPSocketTest):

    HOST = HOST
    PORT = PORT

    def testSocketRegisteredBeforeConnected(self):
        self.cli_conn = self.serv.accept()

    def _testSocketRegisteredBeforeConnected(self):
        timeout = 1000 # milliseconds
        poll_object = select.poll()
        # Register the socket before it is connected
        poll_object.register(self.cli, select.POLLOUT)
        result_list = poll_object.poll(timeout)
        result_sockets = [r[0] for r in result_list]
        self.failIf(self.cli in result_sockets, "Unconnected client socket should not have been selectable")
        # Now connect the socket, but DO NOT register it again
        self.cli.setblocking(0)
        self.cli.connect( (self.HOST, self.PORT) )
        # Now poll again, to check that the poll object has recognised that the socket is now connected
        result_list = poll_object.poll(timeout)
        result_sockets = [r[0] for r in result_list]
        self.failUnless(self.cli in result_sockets, "Connected client socket should have been selectable")

    def testSocketMustBeNonBlocking(self):
        self.cli_conn = self.serv.accept()

    def _testSocketMustBeNonBlocking(self):
        self.cli.setblocking(1)
        self.cli.connect( (self.HOST, self.PORT) )
        timeout = 1000 # milliseconds
        poll_object = select.poll()
        try:
            poll_object.register(self.cli)
        except select.error, se:
            self.failUnlessEqual(se[0], errno.ESOCKISBLOCKING)
        except Exception, x:
            self.fail("Registering blocking socket should have raised select.error, not %s" % str(x))
        else:
            self.fail("Registering blocking socket should have raised select.error")

    def testSelectOnSocketFileno(self):
        self.cli_conn = self.serv.accept()

    def _testSelectOnSocketFileno(self):
        self.cli.setblocking(0)
        self.cli.connect( (self.HOST, self.PORT) )
        return
        try:
            rfd, wfd, xfd = select.select([self.cli.fileno()], [], [], 1)
        except Exception, x:
            self.fail("Selecting on socket.fileno() should not have raised exception: %s" % str(x))

class TestPipes(unittest.TestCase):

    verbose = 1

    def test(self):
        import sys
        from test.test_support import verbose
        if sys.platform[:3] in ('win', 'mac', 'os2', 'riscos'):
            if verbose:
                print "Can't test select easily on", sys.platform
            return
        cmd = 'for i in 0 1 2 3 4 5 6 7 8 9; do echo testing...; sleep 1; done'
        p = os.popen(cmd, 'r')
        for tout in (0, 1, 2, 4, 8, 16) + (None,)*10:
            if verbose:
                print 'timeout =', tout
            rfd, wfd, xfd = select.select([p], [], [], tout)
            if (rfd, wfd, xfd) == ([], [], []):
                continue
            if (rfd, wfd, xfd) == ([p], [], []):
                line = p.readline()
                if verbose:
                    print repr(line)
                if not line:
                    if verbose:
                        print 'EOF'
                    break
                continue
            self.fail('Unexpected return values from select(): %s' % str(rfd, wfd, xfd))
        p.close()

def test_main():
    tests = [
        TestSelectInvalidParameters,
        TestSelectClientSocket,
        TestPollClientSocket,
        ThreadedPollClientSocket,
    ]
    if sys.platform[:4] != 'java':
        tests.append(TestPipes)
    suites = [unittest.makeSuite(klass, 'test') for klass in tests]
    test_support.run_suite(unittest.TestSuite(suites))

if __name__ == "__main__":
    test_main()
