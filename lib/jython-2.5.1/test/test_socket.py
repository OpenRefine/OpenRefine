"""
AMAK: 20050515: This module is the test_socket.py from cpython 2.4, ported to jython.
"""

import java

import unittest
from test import test_support

import errno
import Queue
import platform
import select
import socket
import struct
import sys
import time
import thread, threading
from weakref import proxy
from StringIO import StringIO

PORT = 50007
HOST = 'localhost'
MSG = 'Michael Gilfix was here\n'
EIGHT_BIT_MSG = 'Bh\xed Al\xe1in \xd3 Cinn\xe9ide anseo\n'
os_name = platform.java_ver()[3][0]
is_bsd = os_name == 'Mac OS X' or 'BSD' in os_name
is_solaris = os_name == 'SunOS'

try:
    True
except NameError:
    True, False = 1, 0

class SocketTCPTest(unittest.TestCase):

    HOST = HOST
    PORT = PORT

    def setUp(self):
        self.serv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.serv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.serv.bind((self.HOST, self.PORT))
        self.serv.listen(1)

    def tearDown(self):
        self.serv.close()
        self.serv = None

class SocketUDPTest(unittest.TestCase):

    HOST = HOST
    PORT = PORT

    def setUp(self):
        self.serv = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.serv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.serv.bind((self.HOST, self.PORT))

    def tearDown(self):
        self.serv.close()
        self.serv = None

class ThreadableTest:
    """Threadable Test class

    The ThreadableTest class makes it easy to create a threaded
    client/server pair from an existing unit test. To create a
    new threaded class from an existing unit test, use multiple
    inheritance:

        class NewClass (OldClass, ThreadableTest):
            pass

    This class defines two new fixture functions with obvious
    purposes for overriding:

        clientSetUp ()
        clientTearDown ()

    Any new test functions within the class must then define
    tests in pairs, where the test name is preceeded with a
    '_' to indicate the client portion of the test. Ex:

        def testFoo(self):
            # Server portion

        def _testFoo(self):
            # Client portion

    Any exceptions raised by the clients during their tests
    are caught and transferred to the main thread to alert
    the testing framework.

    Note, the server setup function cannot call any blocking
    functions that rely on the client thread during setup,
    unless serverExplicityReady() is called just before
    the blocking call (such as in setting up a client/server
    connection and performing the accept() in setUp().
    """

    def __init__(self):
        # Swap the true setup function
        self.__setUp = self.setUp
        self.__tearDown = self.tearDown
        self.setUp = self._setUp
        self.tearDown = self._tearDown

    def serverExplicitReady(self):
        """This method allows the server to explicitly indicate that
        it wants the client thread to proceed. This is useful if the
        server is about to execute a blocking routine that is
        dependent upon the client thread during its setup routine."""
        self.server_ready.set()

    def _setUp(self):
        self.server_ready = threading.Event()
        self.client_ready = threading.Event()
        self.done = threading.Event()
        self.queue = Queue.Queue(1)

        # Do some munging to start the client test.
        methodname = self.id()
        i = methodname.rfind('.')
        methodname = methodname[i+1:]
        self.test_method_name = methodname
        test_method = getattr(self, '_' + methodname)
        self.client_thread = thread.start_new_thread(
            self.clientRun, (test_method,))

        self.__setUp()
        if not self.server_ready.isSet():
            self.server_ready.set()
        self.client_ready.wait()

    def _tearDown(self):
        self.done.wait()
        self.__tearDown()

        if not self.queue.empty():
            msg = self.queue.get()
            self.fail(msg)

    def clientRun(self, test_func):
        self.server_ready.wait()
        self.client_ready.set()
        self.clientSetUp()
        if not callable(test_func):
            raise TypeError, "test_func must be a callable function"
        try:
            test_func()
        except Exception, strerror:
            self.queue.put(strerror)
        self.clientTearDown()

    def clientSetUp(self):
        raise NotImplementedError, "clientSetUp must be implemented."

    def clientTearDown(self):
        self.done.set()
        if sys.platform[:4] != 'java':
            # This causes the whole process to exit on jython
            # Probably related to problems with daemon status of threads
            thread.exit()

class ThreadedTCPSocketTest(SocketTCPTest, ThreadableTest):

    def __init__(self, methodName='runTest'):
        SocketTCPTest.__init__(self, methodName=methodName)
        ThreadableTest.__init__(self)

    def clientSetUp(self):
        self.cli = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.cli.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    def clientTearDown(self):
        self.cli.close()
        self.cli = None
        ThreadableTest.clientTearDown(self)

class ThreadedUDPSocketTest(SocketUDPTest, ThreadableTest):

    def __init__(self, methodName='runTest'):
        SocketUDPTest.__init__(self, methodName=methodName)
        ThreadableTest.__init__(self)

    def clientSetUp(self):
        self.cli = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.cli.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

class SocketConnectedTest(ThreadedTCPSocketTest):

    def __init__(self, methodName='runTest'):
        ThreadedTCPSocketTest.__init__(self, methodName=methodName)

    def setUp(self):
        ThreadedTCPSocketTest.setUp(self)
        # Indicate explicitly we're ready for the client thread to
        # proceed and then perform the blocking call to accept
        self.serverExplicitReady()
        conn, addr = self.serv.accept()
        self.cli_conn = conn

    def tearDown(self):
        self.cli_conn.close()
        self.cli_conn = None
        ThreadedTCPSocketTest.tearDown(self)

    def clientSetUp(self):
        ThreadedTCPSocketTest.clientSetUp(self)
        self.cli.connect((self.HOST, self.PORT))
        self.serv_conn = self.cli

    def clientTearDown(self):
        self.serv_conn.close()
        self.serv_conn = None
        ThreadedTCPSocketTest.clientTearDown(self)

class SocketPairTest(unittest.TestCase, ThreadableTest):

    def __init__(self, methodName='runTest'):
        unittest.TestCase.__init__(self, methodName=methodName)
        ThreadableTest.__init__(self)

    def setUp(self):
        self.serv, self.cli = socket.socketpair()

    def tearDown(self):
        self.serv.close()
        self.serv = None

    def clientSetUp(self):
        pass

    def clientTearDown(self):
        self.cli.close()
        self.cli = None
        ThreadableTest.clientTearDown(self)


#######################################################################
## Begin Tests

class GeneralModuleTests(unittest.TestCase):

    def test_weakref(self):
        if sys.platform[:4] == 'java': return
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        p = proxy(s)
        self.assertEqual(p.fileno(), s.fileno())
        s.close()
        s = None
        try:
            p.fileno()
        except ReferenceError:
            pass
        else:
            self.fail('Socket proxy still exists')

    def testSocketError(self):
        # Testing socket module exceptions
        def raise_error(*args, **kwargs):
            raise socket.error
        def raise_herror(*args, **kwargs):
            raise socket.herror
        def raise_gaierror(*args, **kwargs):
            raise socket.gaierror
        self.failUnlessRaises(socket.error, raise_error,
                              "Error raising socket exception.")
        self.failUnlessRaises(socket.error, raise_herror,
                              "Error raising socket exception.")
        self.failUnlessRaises(socket.error, raise_gaierror,
                              "Error raising socket exception.")

    def testCrucialConstants(self):
        # Testing for mission critical constants
        socket.AF_INET
        socket.SOCK_STREAM
        socket.SOCK_DGRAM
        socket.SOCK_RAW
        socket.SOCK_RDM
        socket.SOCK_SEQPACKET
        socket.SOL_SOCKET
        socket.SO_REUSEADDR

    def testHostnameRes(self):
        # Testing hostname resolution mechanisms
        hostname = socket.gethostname()
        self.assert_(isinstance(hostname, str))
        try:
            ip = socket.gethostbyname(hostname)
            self.assert_(isinstance(ip, str))
        except socket.error:
            # Probably name lookup wasn't set up right; skip this test
            self.fail("Probably name lookup wasn't set up right; skip testHostnameRes.gethostbyname")
            return
        self.assert_(ip.find('.') >= 0, "Error resolving host to ip.")
        try:
            hname, aliases, ipaddrs = socket.gethostbyaddr(ip)
            self.assert_(isinstance(hname, str))
            for hosts in aliases, ipaddrs:
                self.assert_(all(isinstance(host, str) for host in hosts))
        except socket.error:
            # Probably a similar problem as above; skip this test
            self.fail("Probably name lookup wasn't set up right; skip testHostnameRes.gethostbyaddr")
            return
        all_host_names = [hostname, hname] + aliases
        fqhn = socket.getfqdn()
        self.assert_(isinstance(fqhn, str))
        if not fqhn in all_host_names:
            self.fail("Error testing host resolution mechanisms.")

    def testRefCountGetNameInfo(self):
        # Testing reference count for getnameinfo
        import sys
        if hasattr(sys, "getrefcount"):
            try:
                # On some versions, this loses a reference
                orig = sys.getrefcount(__name__)
                socket.getnameinfo(__name__,0)
            except SystemError:
                if sys.getrefcount(__name__) <> orig:
                    self.fail("socket.getnameinfo loses a reference")

    def testInterpreterCrash(self):
        if sys.platform[:4] == 'java': return
        # Making sure getnameinfo doesn't crash the interpreter
        try:
            # On some versions, this crashes the interpreter.
            socket.getnameinfo(('x', 0, 0, 0), 0)
        except socket.error:
            pass

# Need to implement binary AND for ints and longs

    def testNtoH(self):
        if sys.platform[:4] == 'java': return # problems with int & long
        # This just checks that htons etc. are their own inverse,
        # when looking at the lower 16 or 32 bits.
        sizes = {socket.htonl: 32, socket.ntohl: 32,
                 socket.htons: 16, socket.ntohs: 16}
        for func, size in sizes.items():
            mask = (1L<<size) - 1
            for i in (0, 1, 0xffff, ~0xffff, 2, 0x01234567, 0x76543210):
                self.assertEqual(i & mask, func(func(i&mask)) & mask)

            swapped = func(mask)
            self.assertEqual(swapped & mask, mask)
            self.assertRaises(OverflowError, func, 1L<<34)

    def testGetServBy(self):
        if sys.platform[:4] == 'java': return # not implemented on java
        eq = self.assertEqual
        # Find one service that exists, then check all the related interfaces.
        # I've ordered this by protocols that have both a tcp and udp
        # protocol, at least for modern Linuxes.
        if sys.platform in ('linux2', 'freebsd4', 'freebsd5', 'freebsd6',
                            'darwin'):
            # avoid the 'echo' service on this platform, as there is an
            # assumption breaking non-standard port/protocol entry
            services = ('daytime', 'qotd', 'domain')
        else:
            services = ('echo', 'daytime', 'domain')
        for service in services:
            try:
                port = socket.getservbyname(service, 'tcp')
                break
            except socket.error:
                pass
        else:
            raise socket.error
        # Try same call with optional protocol omitted
        port2 = socket.getservbyname(service)
        eq(port, port2)
        # Try udp, but don't barf it it doesn't exist
        try:
            udpport = socket.getservbyname(service, 'udp')
        except socket.error:
            udpport = None
        else:
            eq(udpport, port)
        # Now make sure the lookup by port returns the same service name
        eq(socket.getservbyport(port2), service)
        eq(socket.getservbyport(port, 'tcp'), service)
        if udpport is not None:
            eq(socket.getservbyport(udpport, 'udp'), service)

    def testDefaultTimeout(self):
        # Testing default timeout
        # The default timeout should initially be None
        self.assertEqual(socket.getdefaulttimeout(), None)
        s = socket.socket()
        self.assertEqual(s.gettimeout(), None)
        s.close()

        # Set the default timeout to 10, and see if it propagates
        socket.setdefaulttimeout(10)
        self.assertEqual(socket.getdefaulttimeout(), 10)
        s = socket.socket()
        self.assertEqual(s.gettimeout(), 10)
        s.close()

        # Reset the default timeout to None, and see if it propagates
        socket.setdefaulttimeout(None)
        self.assertEqual(socket.getdefaulttimeout(), None)
        s = socket.socket()
        self.assertEqual(s.gettimeout(), None)
        s.close()

        # Check that setting it to an invalid value raises ValueError
        self.assertRaises(ValueError, socket.setdefaulttimeout, -1)

        # Check that setting it to an invalid type raises TypeError
        self.assertRaises(TypeError, socket.setdefaulttimeout, "spam")

    def testIPv4toString(self):
        if not hasattr(socket, 'inet_pton'):
            return # No inet_pton() on this platform
        from socket import inet_aton as f, inet_pton, AF_INET
        g = lambda a: inet_pton(AF_INET, a)

        self.assertEquals('\x00\x00\x00\x00', f('0.0.0.0'))
        self.assertEquals('\xff\x00\xff\x00', f('255.0.255.0'))
        self.assertEquals('\xaa\xaa\xaa\xaa', f('170.170.170.170'))
        self.assertEquals('\x01\x02\x03\x04', f('1.2.3.4'))

        self.assertEquals('\x00\x00\x00\x00', g('0.0.0.0'))
        self.assertEquals('\xff\x00\xff\x00', g('255.0.255.0'))
        self.assertEquals('\xaa\xaa\xaa\xaa', g('170.170.170.170'))

    def testIPv6toString(self):
        if not hasattr(socket, 'inet_pton'):
            return # No inet_pton() on this platform
        try:
            from socket import inet_pton, AF_INET6, has_ipv6
            if not has_ipv6:
                return
        except ImportError:
            return
        f = lambda a: inet_pton(AF_INET6, a)

        self.assertEquals('\x00' * 16, f('::'))
        self.assertEquals('\x00' * 16, f('0::0'))
        self.assertEquals('\x00\x01' + '\x00' * 14, f('1::'))
        self.assertEquals(
            '\x45\xef\x76\xcb\x00\x1a\x56\xef\xaf\xeb\x0b\xac\x19\x24\xae\xae',
            f('45ef:76cb:1a:56ef:afeb:bac:1924:aeae')
        )

    def testStringToIPv4(self):
        if not hasattr(socket, 'inet_ntop'):
            return # No inet_ntop() on this platform
        from socket import inet_ntoa as f, inet_ntop, AF_INET
        g = lambda a: inet_ntop(AF_INET, a)

        self.assertEquals('1.0.1.0', f('\x01\x00\x01\x00'))
        self.assertEquals('170.85.170.85', f('\xaa\x55\xaa\x55'))
        self.assertEquals('255.255.255.255', f('\xff\xff\xff\xff'))
        self.assertEquals('1.2.3.4', f('\x01\x02\x03\x04'))

        self.assertEquals('1.0.1.0', g('\x01\x00\x01\x00'))
        self.assertEquals('170.85.170.85', g('\xaa\x55\xaa\x55'))
        self.assertEquals('255.255.255.255', g('\xff\xff\xff\xff'))

    def testStringToIPv6(self):
        if not hasattr(socket, 'inet_ntop'):
            return # No inet_ntop() on this platform
        try:
            from socket import inet_ntop, AF_INET6, has_ipv6
            if not has_ipv6:
                return
        except ImportError:
            return
        f = lambda a: inet_ntop(AF_INET6, a)

#        self.assertEquals('::', f('\x00' * 16))
#        self.assertEquals('::1', f('\x00' * 15 + '\x01'))
        # java.net.InetAddress always return the full unabbreviated form
        self.assertEquals('0:0:0:0:0:0:0:0', f('\x00' * 16))
        self.assertEquals('0:0:0:0:0:0:0:1', f('\x00' * 15 + '\x01'))
        self.assertEquals(
            'aef:b01:506:1001:ffff:9997:55:170',
            f('\x0a\xef\x0b\x01\x05\x06\x10\x01\xff\xff\x99\x97\x00\x55\x01\x70')
        )

    # XXX The following don't test module-level functionality...

    def testSockName(self):
        # Testing getsockname()
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.bind(("0.0.0.0", PORT+1))
        name = sock.getsockname()
        self.assertEqual(name, ("0.0.0.0", PORT+1))

    def testGetSockOpt(self):
        # Testing getsockopt()
        # We know a socket should start without reuse==0
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        reuse = sock.getsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR)
        self.failIf(reuse != 0, "initial mode is reuse")

    def testSetSockOpt(self):
        # Testing setsockopt()
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        reuse = sock.getsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR)
        self.failIf(reuse == 0, "failed to set reuse mode")

    def testSendAfterClose(self):
        # testing send() after close() with timeout
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(1)
        sock.close()
        self.assertRaises(socket.error, sock.send, "spam")

class TestSocketOptions(unittest.TestCase):

    def setUp(self):
        self.test_udp = self.test_tcp_client = self.test_tcp_server = 0

    def _testSetAndGetOption(self, sock, level, option, values):
        for expected_value in values:
            sock.setsockopt(level, option, expected_value)
            retrieved_value = sock.getsockopt(level, option)
            msg = "Retrieved option(%s, %s) value %s != %s(value set)" % (level, option, retrieved_value, expected_value)
            if is_solaris and option == socket.SO_RCVBUF:
                self.assert_(retrieved_value >= expected_value, msg)
            else:
                self.failUnlessEqual(retrieved_value, expected_value, msg)

    def _testUDPOption(self, level, option, values):
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self._testSetAndGetOption(sock, level, option, values)
            # now bind the socket i.e. cause the implementation socket to be created
            sock.bind( (HOST, PORT) )
            self.failUnlessEqual(sock.getsockopt(level, option), values[-1], \
                 "Option value '(%s, %s)'='%s' did not propagate to implementation socket" % (level, option, values[-1]) )
            self._testSetAndGetOption(sock, level, option, values)
        finally:
            sock.close()

    def _testTCPClientOption(self, level, option, values):
        sock = None
        try:
            # First listen on a server socket, so that the connection won't be refused.
            server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            server_sock.bind( (HOST, PORT) )
            server_sock.listen(50)
            # Now do the tests
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._testSetAndGetOption(sock, level, option, values)
            # now connect the socket i.e. cause the implementation socket to be created
            # First bind, so that the SO_REUSEADDR setting propagates
            sock.bind( (HOST, PORT+1) )
            sock.connect( (HOST, PORT) )
            msg = "Option value '%s'='%s' did not propagate to implementation socket" % (option, values[-1])
            if ((is_bsd or is_solaris) and option in (socket.SO_RCVBUF, socket.SO_SNDBUF)):
                # NOTE: there's no guarantee that bufsize will be the
                # exact setsockopt value, particularly after
                # establishing a connection. seems it will be *at least*
                # the values we test (which are rather small) on
                # BSDs. may need to relax this on other platforms also
                self.assert_(sock.getsockopt(level, option) >= values[-1], msg)
            else:
                self.failUnlessEqual(sock.getsockopt(level, option), values[-1], msg)
            self._testSetAndGetOption(sock, level, option, values)
        finally:
            server_sock.close()
            if sock:
                sock.close()

    def _testTCPServerOption(self, level, option, values):
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self._testSetAndGetOption(sock, level, option, values)
            # now bind and listen on the socket i.e. cause the implementation socket to be created
            sock.bind( (HOST, PORT) )
            sock.listen(50)
            msg = "Option value '(%s,%s)'='%s' did not propagate to implementation socket" % (level, option, values[-1])
            if is_solaris and option == socket.SO_RCVBUF:
                # NOTE: see similar bsd/solaris workaround above
                self.assert_(sock.getsockopt(level, option) >= values[-1], msg)
            else:
                self.failUnlessEqual(sock.getsockopt(level, option), values[-1], msg)
            self._testSetAndGetOption(sock, level, option, values)
        finally:
            sock.close()

    def _testOption(self, level, option, values):
        for flag, func in [
            (self.test_udp,        self._testUDPOption),
            (self.test_tcp_server, self._testTCPServerOption),
            (self.test_tcp_client, self._testTCPClientOption),
        ]:
            if flag:
                func(level, option, values)
            else:
                try:
                    func(level, option, values)
                except socket.error, se:
                    self.failUnlessEqual(se[0], errno.ENOPROTOOPT, "Wrong errno from unsupported option exception: %d" % se[0])
                except Exception, x:
                    self.fail("Wrong exception raised from unsupported option: %s" % str(x))
                else:
                    self.fail("Setting unsupported option should have raised an exception")

class TestSupportedOptions(TestSocketOptions):

    def testSO_BROADCAST(self):
        self.test_udp = 1
        self._testOption(socket.SOL_SOCKET, socket.SO_BROADCAST, [0, 1])

    def testSO_KEEPALIVE(self):
        self.test_tcp_client = 1
        self._testOption(socket.SOL_SOCKET, socket.SO_KEEPALIVE, [0, 1])

    def testSO_LINGER(self):
        self.test_tcp_client = 1
        off = struct.pack('ii', 0, 0)
        on_2_seconds = struct.pack('ii', 1, 2)
        self._testOption(socket.SOL_SOCKET, socket.SO_LINGER, [off, on_2_seconds])

    def testSO_OOBINLINE(self):
        self.test_tcp_client = 1
        self._testOption(socket.SOL_SOCKET, socket.SO_OOBINLINE, [0, 1])

    def testSO_RCVBUF(self):
        self.test_udp = 1
        self.test_tcp_client = 1
        self.test_tcp_server = 1
        self._testOption(socket.SOL_SOCKET, socket.SO_RCVBUF, [1024, 4096, 16384])

    def testSO_REUSEADDR(self):
        self.test_udp = 1
        self.test_tcp_client = 1
        self.test_tcp_server = 1
        self._testOption(socket.SOL_SOCKET, socket.SO_REUSEADDR, [0, 1])

    def testSO_SNDBUF(self):
        self.test_udp = 1
        self.test_tcp_client = 1
        self._testOption(socket.SOL_SOCKET, socket.SO_SNDBUF, [1024, 4096, 16384])

    def testSO_TIMEOUT(self):
        self.test_udp = 1
        self.test_tcp_client = 1
        self.test_tcp_server = 1
        self._testOption(socket.SOL_SOCKET, socket.SO_TIMEOUT, [0, 1, 1000])

    def testTCP_NODELAY(self):
        self.test_tcp_client = 1
        self._testOption(socket.IPPROTO_TCP, socket.TCP_NODELAY, [0, 1])

class TestUnsupportedOptions(TestSocketOptions):

    def testSO_ACCEPTCONN(self):
        self.failUnless(hasattr(socket, 'SO_ACCEPTCONN'))

    def testSO_DEBUG(self):
        self.failUnless(hasattr(socket, 'SO_DEBUG'))

    def testSO_DONTROUTE(self):
        self.failUnless(hasattr(socket, 'SO_DONTROUTE'))

    def testSO_ERROR(self):
        self.failUnless(hasattr(socket, 'SO_ERROR'))

    def testSO_EXCLUSIVEADDRUSE(self):
        # this is an MS specific option that will not be appearing on java
        # http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6421091
        # http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6402335
        self.failUnless(hasattr(socket, 'SO_EXCLUSIVEADDRUSE'))

    def testSO_RCVLOWAT(self):
        self.failUnless(hasattr(socket, 'SO_RCVLOWAT'))

    def testSO_RCVTIMEO(self):
        self.failUnless(hasattr(socket, 'SO_RCVTIMEO'))

    def testSO_REUSEPORT(self):
        # not yet supported on java
        # http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6432031
        self.failUnless(hasattr(socket, 'SO_REUSEPORT'))

    def testSO_SNDLOWAT(self):
        self.failUnless(hasattr(socket, 'SO_SNDLOWAT'))

    def testSO_SNDTIMEO(self):
        self.failUnless(hasattr(socket, 'SO_SNDTIMEO'))

    def testSO_TYPE(self):
        self.failUnless(hasattr(socket, 'SO_TYPE'))

    def testSO_USELOOPBACK(self):
        self.failUnless(hasattr(socket, 'SO_USELOOPBACK'))

class BasicTCPTest(SocketConnectedTest):

    def __init__(self, methodName='runTest'):
        SocketConnectedTest.__init__(self, methodName=methodName)

    def testRecv(self):
        # Testing large receive over TCP
        msg = self.cli_conn.recv(1024)
        self.assertEqual(msg, MSG)

    def _testRecv(self):
        self.serv_conn.send(MSG)

    def testRecvTimeoutMode(self):
        # Do this test in timeout mode, because the code path is different
        self.cli_conn.settimeout(10)
        msg = self.cli_conn.recv(1024)
        self.assertEqual(msg, MSG)

    def _testRecvTimeoutMode(self):
        self.serv_conn.settimeout(10)
        self.serv_conn.send(MSG)

    def testOverFlowRecv(self):
        # Testing receive in chunks over TCP
        seg1 = self.cli_conn.recv(len(MSG) - 3)
        seg2 = self.cli_conn.recv(1024)
        msg = seg1 + seg2
        self.assertEqual(msg, MSG)

    def _testOverFlowRecv(self):
        self.serv_conn.send(MSG)

    def testRecvFrom(self):
        # Testing large recvfrom() over TCP
        msg, addr = self.cli_conn.recvfrom(1024)
        self.assertEqual(msg, MSG)

    def _testRecvFrom(self):
        self.serv_conn.send(MSG)

    def testOverFlowRecvFrom(self):
        # Testing recvfrom() in chunks over TCP
        seg1, addr = self.cli_conn.recvfrom(len(MSG)-3)
        seg2, addr = self.cli_conn.recvfrom(1024)
        msg = seg1 + seg2
        self.assertEqual(msg, MSG)

    def _testOverFlowRecvFrom(self):
        self.serv_conn.send(MSG)

    def testSendAll(self):
        # Testing sendall() with a 2048 byte string over TCP
        msg = ''
        while 1:
            read = self.cli_conn.recv(1024)
            if not read:
                break
            msg += read
        self.assertEqual(msg, 'f' * 2048)

    def _testSendAll(self):
        big_chunk = 'f' * 2048
        self.serv_conn.sendall(big_chunk)

    def testFromFd(self):
        # Testing fromfd()
        if not hasattr(socket, "fromfd"):
            return # On Windows, this doesn't exist
        fd = self.cli_conn.fileno()
        sock = socket.fromfd(fd, socket.AF_INET, socket.SOCK_STREAM)
        msg = sock.recv(1024)
        self.assertEqual(msg, MSG)

    def _testFromFd(self):
        self.serv_conn.send(MSG)

    def testShutdown(self):
        # Testing shutdown()
        msg = self.cli_conn.recv(1024)
        self.assertEqual(msg, MSG)

    def _testShutdown(self):
        self.serv_conn.send(MSG)
        self.serv_conn.shutdown(2)

    def testSendAfterRemoteClose(self):
        self.cli_conn.close()

    def _testSendAfterRemoteClose(self):
        for x in range(5):
            try:
                self.serv_conn.send("spam")
            except socket.error, se:
                self.failUnlessEqual(se[0], errno.ECONNRESET)
                return
            except Exception, x:
                self.fail("Sending on remotely closed socket raised wrong exception: %s" % x)
            time.sleep(0.5)
        self.fail("Sending on remotely closed socket should have raised exception")

    def testDup(self):
        msg = self.cli_conn.recv(len(MSG))
        self.assertEqual(msg, MSG)

        dup_conn = self.cli_conn.dup()
        msg = dup_conn.recv(len('and ' + MSG))
        self.assertEqual(msg, 'and ' +  MSG)

    def _testDup(self):
        self.serv_conn.send(MSG)
        self.serv_conn.send('and ' + MSG)

class UDPBindTest(unittest.TestCase):

    HOST = HOST
    PORT = PORT

    def setUp(self):
        self.sock = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)

    def testBindSpecific(self):
        self.sock.bind( (self.HOST, self.PORT) ) # Use a specific port
        actual_port = self.sock.getsockname()[1]
        self.failUnless(actual_port == self.PORT,
            "Binding to specific port number should have returned same number: %d != %d" % (actual_port, self.PORT))

    def testBindEphemeral(self):
        self.sock.bind( (self.HOST, 0) ) # let system choose a free port
        self.failUnless(self.sock.getsockname()[1] != 0, "Binding to port zero should have allocated an ephemeral port number")

    def testShutdown(self):
        self.sock.bind( (self.HOST, self.PORT) )
        self.sock.shutdown(socket.SHUT_RDWR)

    def tearDown(self):
        self.sock.close()

class BasicUDPTest(ThreadedUDPSocketTest):

    def __init__(self, methodName='runTest'):
        ThreadedUDPSocketTest.__init__(self, methodName=methodName)

    def testSendtoAndRecv(self):
        # Testing sendto() and recv() over UDP
        msg = self.serv.recv(len(MSG))
        self.assertEqual(msg, MSG)

    def _testSendtoAndRecv(self):
        self.cli.sendto(MSG, 0, (self.HOST, self.PORT))

    def testSendtoAndRecvTimeoutMode(self):
        # Need to test again in timeout mode, which follows
        # a different code path
        self.serv.settimeout(10)
        msg = self.serv.recv(len(MSG))
        self.assertEqual(msg, MSG)

    def _testSendtoAndRecvTimeoutMode(self):
        self.cli.settimeout(10)
        self.cli.sendto(MSG, 0, (self.HOST, self.PORT))

    def testSendAndRecv(self):
        # Testing send() and recv() over connect'ed UDP
        msg = self.serv.recv(len(MSG))
        self.assertEqual(msg, MSG)

    def _testSendAndRecv(self):
        self.cli.connect( (self.HOST, self.PORT) )
        self.cli.send(MSG, 0)

    def testSendAndRecvTimeoutMode(self):
        # Need to test again in timeout mode, which follows
        # a different code path
        self.serv.settimeout(10)
        # Testing send() and recv() over connect'ed UDP
        msg = self.serv.recv(len(MSG))
        self.assertEqual(msg, MSG)

    def _testSendAndRecvTimeoutMode(self):
        self.cli.connect( (self.HOST, self.PORT) )
        self.cli.settimeout(10)
        self.cli.send(MSG, 0)

    def testRecvFrom(self):
        # Testing recvfrom() over UDP
        msg, addr = self.serv.recvfrom(len(MSG))
        self.assertEqual(msg, MSG)

    def _testRecvFrom(self):
        self.cli.sendto(MSG, 0, (self.HOST, self.PORT))

    def testRecvFromTimeoutMode(self):
        # Need to test again in timeout mode, which follows
        # a different code path
        self.serv.settimeout(10)
        msg, addr = self.serv.recvfrom(len(MSG))
        self.assertEqual(msg, MSG)

    def _testRecvFromTimeoutMode(self):
        self.cli.settimeout(10)
        self.cli.sendto(MSG, 0, (self.HOST, self.PORT))

    def testSendtoEightBitSafe(self):
        # This test is necessary because java only supports signed bytes
        msg = self.serv.recv(len(EIGHT_BIT_MSG))
        self.assertEqual(msg, EIGHT_BIT_MSG)

    def _testSendtoEightBitSafe(self):
        self.cli.sendto(EIGHT_BIT_MSG, 0, (self.HOST, self.PORT))

    def testSendtoEightBitSafeTimeoutMode(self):
        # Need to test again in timeout mode, which follows
        # a different code path
        self.serv.settimeout(10)
        msg = self.serv.recv(len(EIGHT_BIT_MSG))
        self.assertEqual(msg, EIGHT_BIT_MSG)

    def _testSendtoEightBitSafeTimeoutMode(self):
        self.cli.settimeout(10)
        self.cli.sendto(EIGHT_BIT_MSG, 0, (self.HOST, self.PORT))

class UDPBroadcastTest(ThreadedUDPSocketTest):

    def setUp(self):
        self.serv = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.serv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    def testBroadcast(self):
        self.serv.bind( ("", self.PORT) )
        msg = self.serv.recv(len(EIGHT_BIT_MSG))
        self.assertEqual(msg, EIGHT_BIT_MSG)

    def _testBroadcast(self):
        self.cli.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.cli.sendto(EIGHT_BIT_MSG, ("<broadcast>", self.PORT) )

class BasicSocketPairTest(SocketPairTest):

    def __init__(self, methodName='runTest'):
        SocketPairTest.__init__(self, methodName=methodName)

    def testRecv(self):
        msg = self.serv.recv(1024)
        self.assertEqual(msg, MSG)

    def _testRecv(self):
        self.cli.send(MSG)

    def testSend(self):
        self.serv.send(MSG)

    def _testSend(self):
        msg = self.cli.recv(1024)
        self.assertEqual(msg, MSG)

class NonBlockingTCPServerTests(SocketTCPTest):

    def testSetBlocking(self):
        # Testing whether set blocking works
        self.serv.setblocking(0)
        start = time.time()
        try:
            self.serv.accept()
        except socket.error:
            pass
        end = time.time()
        self.assert_((end - start) < 1.0, "Error setting non-blocking mode.")

    def testGetBlocking(self):
        # Testing whether set blocking works
        self.serv.setblocking(0)
        self.failUnless(not self.serv.getblocking(), "Getblocking return true instead of false")
        self.serv.setblocking(1)
        self.failUnless(self.serv.getblocking(), "Getblocking return false instead of true")

    def testAcceptNoConnection(self):
        # Testing non-blocking accept returns immediately when no connection
        self.serv.setblocking(0)
        try:
            conn, addr = self.serv.accept()
        except socket.error:
            pass
        else:
            self.fail("Error trying to do non-blocking accept.")

class NonBlockingTCPTests(ThreadedTCPSocketTest):

    def __init__(self, methodName='runTest'):
        ThreadedTCPSocketTest.__init__(self, methodName=methodName)

    def testAcceptConnection(self):
        # Testing non-blocking accept works when connection present
        self.serv.setblocking(0)
        read, write, err = select.select([self.serv], [], [])
        if self.serv in read:
            conn, addr = self.serv.accept()
        else:
            self.fail("Error trying to do accept after select: server socket was not in 'read'able list")

    def _testAcceptConnection(self):
        # Make a connection to the server
        self.cli.connect((self.HOST, self.PORT))

    #
    # AMAK: 20070311
    # Introduced a new test for non-blocking connect
    # Renamed old testConnect to testBlockingConnect
    #

    def testBlockingConnect(self):
        # Testing blocking connect
        conn, addr = self.serv.accept()

    def _testBlockingConnect(self):
        # Testing blocking connect
        self.cli.settimeout(10)
        self.cli.connect((self.HOST, self.PORT))

    def testNonBlockingConnect(self):
        # Testing non-blocking connect
        conn, addr = self.serv.accept()

    def _testNonBlockingConnect(self):
        # Testing non-blocking connect
        self.cli.setblocking(0)
        result = self.cli.connect_ex((self.HOST, self.PORT))
        rfds, wfds, xfds = select.select([], [self.cli], [])
        self.failUnless(self.cli in wfds)
        try:
            self.cli.send(MSG)
        except socket.error:
            self.fail("Sending on connected socket should not have raised socket.error")

    #
    # AMAK: 20070518
    # Introduced a new test for connect with bind to specific local address
    #

    def testConnectWithLocalBind(self):
        # Test blocking connect
        conn, addr = self.serv.accept()

    def _testConnectWithLocalBind(self):
        # Testing blocking connect with local bind
        cli_port = self.PORT - 1
        while True:
            # Keep trying until a local port is available
            self.cli.settimeout(1)
            self.cli.bind( (self.HOST, cli_port) )
            try:
                self.cli.connect((self.HOST, self.PORT))
                break
            except socket.error, se:
                # cli_port is in use (maybe in TIME_WAIT state from a
                # previous test run). reset the client socket and try
                # again
                self.failUnlessEqual(se[0], errno.EADDRINUSE)
                try:
                    self.cli.close()
                except socket.error:
                    pass
                self.clientSetUp()
                cli_port -= 1
        bound_host, bound_port = self.cli.getsockname()
        self.failUnlessEqual(bound_port, cli_port)

    def testRecvData(self):
        # Testing non-blocking recv
        conn, addr = self.serv.accept()
        conn.setblocking(0)
        rfds, wfds, xfds = select.select([conn], [], [])
        if conn in rfds:
            msg = conn.recv(len(MSG))
            self.assertEqual(msg, MSG)
        else:
            self.fail("Non-blocking socket with data should been in read list.")

    def _testRecvData(self):
        self.cli.connect((self.HOST, self.PORT))
        self.cli.send(MSG)

    def testRecvNoData(self):
        # Testing non-blocking recv
        conn, addr = self.serv.accept()
        conn.setblocking(0)
        try:
            msg = conn.recv(len(MSG))
        except socket.error:
            pass
        else:
            self.fail("Non-blocking recv of no data should have raised socket.error.")

    def _testRecvNoData(self):
        self.cli.connect((self.HOST, self.PORT))
        time.sleep(0.1)

class NonBlockingUDPTests(ThreadedUDPSocketTest): pass

#
# TODO: Write some non-blocking UDP tests
#

class TCPFileObjectClassOpenCloseTests(SocketConnectedTest):

    def testCloseFileDoesNotCloseSocket(self):
        # This test is necessary on java/jython
        msg = self.cli_conn.recv(1024)
        self.assertEqual(msg, MSG)

    def _testCloseFileDoesNotCloseSocket(self):
        self.cli_file = self.serv_conn.makefile('wb')
        self.cli_file.close()
        try:
            self.serv_conn.send(MSG)
        except Exception, x:
            self.fail("Closing file wrapper appears to have closed underlying socket: %s" % str(x))

    def testCloseSocketDoesNotCloseFile(self):
        msg = self.cli_conn.recv(1024)
        self.assertEqual(msg, MSG)

    def _testCloseSocketDoesNotCloseFile(self):
        self.cli_file = self.serv_conn.makefile('wb')
        self.serv_conn.close()
        try:
            self.cli_file.write(MSG)
            self.cli_file.flush()
        except Exception, x:
            self.fail("Closing socket appears to have closed file wrapper: %s" % str(x))

class UDPFileObjectClassOpenCloseTests(ThreadedUDPSocketTest):

    def testCloseFileDoesNotCloseSocket(self):
        # This test is necessary on java/jython
        msg = self.serv.recv(1024)
        self.assertEqual(msg, MSG)

    def _testCloseFileDoesNotCloseSocket(self):
        self.cli_file = self.cli.makefile('wb')
        self.cli_file.close()
        try:
            self.cli.sendto(MSG, 0, (self.HOST, self.PORT))
        except Exception, x:
            self.fail("Closing file wrapper appears to have closed underlying socket: %s" % str(x))

    def testCloseSocketDoesNotCloseFile(self):
        self.serv_file = self.serv.makefile('rb')
        self.serv.close()
        msg = self.serv_file.readline()
        self.assertEqual(msg, MSG)

    def _testCloseSocketDoesNotCloseFile(self):
        try:
            self.cli.sendto(MSG, 0, (self.HOST, self.PORT))
        except Exception, x:
            self.fail("Closing file wrapper appears to have closed underlying socket: %s" % str(x))

class FileAndDupOpenCloseTests(SocketConnectedTest):

    def testCloseDoesNotCloseOthers(self):
        msg = self.cli_conn.recv(len(MSG))
        self.assertEqual(msg, MSG)

        msg = self.cli_conn.recv(len('and ' + MSG))
        self.assertEqual(msg, 'and ' + MSG)

    def _testCloseDoesNotCloseOthers(self):
        self.dup_conn1 = self.serv_conn.dup()
        self.dup_conn2 = self.serv_conn.dup()
        self.cli_file = self.serv_conn.makefile('wb')
        self.serv_conn.close()
        self.dup_conn1.close()

        try:
            self.serv_conn.send(MSG)
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.EBADF)
        else:
            self.fail("Original socket did not close")
        try:
            self.dup_conn1.send(MSG)
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.EBADF)
        else:
            self.fail("Duplicate socket 1 did not close")

        self.dup_conn2.send(MSG)
        self.dup_conn2.close()

        try:
            self.cli_file.write('and ' + MSG)
        except Exception, x:
            self.fail("Closing others appears to have closed the socket file: %s" % str(x))
        self.cli_file.close()

class FileObjectClassTestCase(SocketConnectedTest):

    bufsize = -1 # Use default buffer size

    def __init__(self, methodName='runTest'):
        SocketConnectedTest.__init__(self, methodName=methodName)

    def setUp(self):
        SocketConnectedTest.setUp(self)
        self.serv_file = self.cli_conn.makefile('rb', self.bufsize)

    def tearDown(self):
        self.serv_file.close()
        self.assert_(self.serv_file.closed)
        self.serv_file = None
        SocketConnectedTest.tearDown(self)

    def clientSetUp(self):
        SocketConnectedTest.clientSetUp(self)
        self.cli_file = self.serv_conn.makefile('wb')

    def clientTearDown(self):
        self.cli_file.close()
        self.assert_(self.cli_file.closed)
        self.cli_file = None
        SocketConnectedTest.clientTearDown(self)

    def testSmallRead(self):
        # Performing small file read test
        first_seg = self.serv_file.read(len(MSG)-3)
        second_seg = self.serv_file.read(3)
        msg = first_seg + second_seg
        self.assertEqual(msg, MSG)

    def _testSmallRead(self):
        self.cli_file.write(MSG)
        self.cli_file.flush()

    def testFullRead(self):
        # read until EOF
        msg = self.serv_file.read()
        self.assertEqual(msg, MSG)

    def _testFullRead(self):
        self.cli_file.write(MSG)
        self.cli_file.flush()

    def testUnbufferedRead(self):
        # Performing unbuffered file read test
        buf = ''
        while 1:
            char = self.serv_file.read(1)
            if not char:
                break
            buf += char
        self.assertEqual(buf, MSG)

    def _testUnbufferedRead(self):
        self.cli_file.write(MSG)
        self.cli_file.flush()

    def testReadline(self):
        # Performing file readline test
        line = self.serv_file.readline()
        self.assertEqual(line, MSG)

    def _testReadline(self):
        self.cli_file.write(MSG)
        self.cli_file.flush()

    def testClosedAttr(self):
        self.assert_(not self.serv_file.closed)

    def _testClosedAttr(self):
        self.assert_(not self.cli_file.closed)

class PrivateFileObjectTestCase(unittest.TestCase):

    """Test usage of socket._fileobject with an arbitrary socket-like
    object.

    E.g. urllib2 wraps an httplib.HTTPResponse object with _fileobject.
    """

    def setUp(self):
        self.socket_like = StringIO()
        self.socket_like.recv = self.socket_like.read
        self.socket_like.sendall = self.socket_like.write

    def testPrivateFileObject(self):
        fileobject = socket._fileobject(self.socket_like, 'rb')
        fileobject.write('hello jython')
        fileobject.flush()
        self.socket_like.seek(0)
        self.assertEqual(fileobject.read(), 'hello jython')

class UnbufferedFileObjectClassTestCase(FileObjectClassTestCase):

    """Repeat the tests from FileObjectClassTestCase with bufsize==0.

    In this case (and in this case only), it should be possible to
    create a file object, read a line from it, create another file
    object, read another line from it, without loss of data in the
    first file object's buffer.  Note that httplib relies on this
    when reading multiple requests from the same socket."""

    bufsize = 0 # Use unbuffered mode

    def testUnbufferedReadline(self):
        # Read a line, create a new file object, read another line with it
        line = self.serv_file.readline() # first line
        self.assertEqual(line, "A. " + MSG) # first line
        self.serv_file = self.cli_conn.makefile('rb', 0)
        line = self.serv_file.readline() # second line
        self.assertEqual(line, "B. " + MSG) # second line

    def _testUnbufferedReadline(self):
        self.cli_file.write("A. " + MSG)
        self.cli_file.write("B. " + MSG)
        self.cli_file.flush()

class LineBufferedFileObjectClassTestCase(FileObjectClassTestCase):

    bufsize = 1 # Default-buffered for reading; line-buffered for writing


class SmallBufferedFileObjectClassTestCase(FileObjectClassTestCase):

    bufsize = 2 # Exercise the buffering code

class TCPServerTimeoutTest(SocketTCPTest):

    def testAcceptTimeout(self):
        def raise_timeout(*args, **kwargs):
            self.serv.settimeout(1.0)
            self.serv.accept()
        self.failUnlessRaises(socket.timeout, raise_timeout,
                              "TCP socket accept failed to generate a timeout exception (TCP)")

    def testTimeoutZero(self):
        ok = False
        try:
            self.serv.settimeout(0.0)
            foo = self.serv.accept()
        except socket.timeout:
            self.fail("caught timeout instead of error (TCP)")
        except socket.error:
            ok = True
        except Exception, x:
            self.fail("caught unexpected exception (TCP): %s" % str(x))
        if not ok:
            self.fail("accept() returned success when we did not expect it")

class TCPClientTimeoutTest(SocketTCPTest):

    def testConnectTimeout(self):
        cli = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        cli.settimeout(0.1)
        host = '192.168.192.168'
        try:
            cli.connect((host, 5000))
        except socket.timeout, st:
            pass
        except Exception, x:
            self.fail("Client socket timeout should have raised socket.timeout, not %s" % str(x))
        else:
            self.fail('''Client socket timeout should have raised
socket.timeout.  This tries to connect to %s in the assumption that it isn't
used, but if it is on your network this failure is bogus.''' % host)

    def testConnectDefaultTimeout(self):
        _saved_timeout = socket.getdefaulttimeout()
        socket.setdefaulttimeout(0.1)
        cli = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        host = '192.168.192.168'
        try:
            cli.connect((host, 5000))
        except socket.timeout, st:
            pass
        except Exception, x:
            self.fail("Client socket timeout should have raised socket.timeout, not %s" % str(x))
        else:
            self.fail('''Client socket timeout should have raised
socket.timeout.  This tries to connect to %s in the assumption that it isn't
used, but if it is on your network this failure is bogus.''' % host)
        socket.setdefaulttimeout(_saved_timeout)

    def testRecvTimeout(self):
        def raise_timeout(*args, **kwargs):
            cli_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            cli_sock.connect( (HOST, PORT) )
            cli_sock.settimeout(1)
            cli_sock.recv(1024)
        self.failUnlessRaises(socket.timeout, raise_timeout,
                              "TCP socket recv failed to generate a timeout exception (TCP)")

    # Disable this test, but leave it present for documentation purposes
    # socket timeouts only work for read and accept, not for write
    # http://java.sun.com/j2se/1.4.2/docs/api/java/net/SocketTimeoutException.html
    def estSendTimeout(self):
        def raise_timeout(*args, **kwargs):
            cli_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            cli_sock.connect( (HOST, PORT) )
            # First fill the socket
            cli_sock.settimeout(1)
            sent = 0
            while True:
                bytes_sent = cli_sock.send(MSG)
                sent += bytes_sent
        self.failUnlessRaises(socket.timeout, raise_timeout,
                              "TCP socket send failed to generate a timeout exception (TCP)")

    def testSwitchModes(self):
        cli_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        cli_sock.connect( (HOST, PORT) )
        # set non-blocking mode
        cli_sock.setblocking(0)
        # then set timeout mode
        cli_sock.settimeout(1)
        try:
            cli_sock.send(MSG)
        except Exception, x:
            self.fail("Switching mode from non-blocking to timeout raised exception: %s" % x)
        else:
            pass

#
# AMAK: 20070307
# Corrected the superclass of UDPTimeoutTest
#

class UDPTimeoutTest(SocketUDPTest):

    def testUDPTimeout(self):
        def raise_timeout(*args, **kwargs):
            self.serv.settimeout(1.0)
            self.serv.recv(1024)
        self.failUnlessRaises(socket.timeout, raise_timeout,
                              "Error generating a timeout exception (UDP)")

    def testTimeoutZero(self):
        ok = False
        try:
            self.serv.settimeout(0.0)
            foo = self.serv.recv(1024)
        except socket.timeout:
            self.fail("caught timeout instead of error (UDP)")
        except socket.error:
            ok = True
        except Exception, x:
            self.fail("caught unexpected exception (UDP): %s" % str(x))
        if not ok:
            self.fail("recv() returned success when we did not expect it")

class TestGetAddrInfo(unittest.TestCase):

    def testBadFamily(self):
        try:
            socket.getaddrinfo(HOST, PORT, 9999)
        except socket.gaierror, gaix:
            self.failUnlessEqual(gaix[0], errno.EIO)
        except Exception, x:
            self.fail("getaddrinfo with bad family raised wrong exception: %s" % x)
        else:
            self.fail("getaddrinfo with bad family should have raised exception")

    def testReturnsAreStrings(self):
        addrinfos = socket.getaddrinfo(HOST, PORT)
        for addrinfo in addrinfos:
            family, socktype, proto, canonname, sockaddr = addrinfo
            self.assert_(isinstance(canonname, str))
            self.assert_(isinstance(sockaddr[0], str))

    def testAI_PASSIVE(self):
        IPV4_LOOPBACK = "127.0.0.1"
        local_hostname = java.net.InetAddress.getLocalHost().getHostName()
        local_ip_address = java.net.InetAddress.getLocalHost().getHostAddress()
        for flags, host_param, expected_canonname, expected_sockaddr in [
            # First passive flag
            (socket.AI_PASSIVE, None, "", socket.INADDR_ANY),
            (socket.AI_PASSIVE, "", "", local_ip_address),
            (socket.AI_PASSIVE, "localhost", "", IPV4_LOOPBACK),
            (socket.AI_PASSIVE, local_hostname, "", local_ip_address),
            # Now passive flag AND canonname flag
            # Commenting out all AI_CANONNAME tests, results too dependent on system config
            #(socket.AI_PASSIVE|socket.AI_CANONNAME, None, "127.0.0.1", "127.0.0.1"),
            #(socket.AI_PASSIVE|socket.AI_CANONNAME, "", local_hostname, local_ip_address),
            # The following gives varying results across platforms and configurations: commenting out for now.
            # Javadoc: http://java.sun.com/j2se/1.5.0/docs/api/java/net/InetAddress.html#getCanonicalHostName()
            #(socket.AI_PASSIVE|socket.AI_CANONNAME, "localhost", local_hostname, IPV4_LOOPBACK),
            #(socket.AI_PASSIVE|socket.AI_CANONNAME, local_hostname, local_hostname, local_ip_address),
        ]:
            addrinfos = socket.getaddrinfo(host_param, 0, socket.AF_INET, socket.SOCK_STREAM, 0, flags)
            for family, socktype, proto, canonname, sockaddr in addrinfos:
                self.failUnlessEqual(expected_canonname, canonname, "For hostname '%s' and flags %d, canonname '%s' != '%s'" % (host_param, flags, expected_canonname, canonname) )
                self.failUnlessEqual(expected_sockaddr, sockaddr[0], "For hostname '%s' and flags %d, sockaddr '%s' != '%s'" % (host_param, flags, expected_sockaddr, sockaddr[0]) )

class TestExceptions(unittest.TestCase):

    def testExceptionTree(self):
        self.assert_(issubclass(socket.error, Exception))
        self.assert_(issubclass(socket.herror, socket.error))
        self.assert_(issubclass(socket.gaierror, socket.error))
        self.assert_(issubclass(socket.timeout, socket.error))

class TestJythonExceptionsShared:

    def tearDown(self):
        self.s.close()
        self.s = None

    def testHostNotFound(self):
        try:
            socket.gethostbyname("doesnotexist")
        except socket.gaierror, gaix:
            self.failUnlessEqual(gaix[0], errno.EGETADDRINFOFAILED)
        except Exception, x:
            self.fail("Get host name for non-existent host raised wrong exception: %s" % x)

    def testUnresolvedAddress(self):
        try:
            self.s.connect( ('non.existent.server', PORT) )
        except socket.gaierror, gaix:
            self.failUnlessEqual(gaix[0], errno.EGETADDRINFOFAILED)
        except Exception, x:
            self.fail("Get host name for non-existent host raised wrong exception: %s" % x)
        else:
            self.fail("Get host name for non-existent host should have raised exception")

    def testSocketNotConnected(self):
        try:
            self.s.send(MSG)
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.ENOTCONN)
        except Exception, x:
            self.fail("Send on unconnected socket raised wrong exception: %s" % x)
        else:
            self.fail("Send on unconnected socket raised exception")

    def testSocketNotBound(self):
        try:
            result = self.s.recv(1024)
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.ENOTCONN)
        except Exception, x:
            self.fail("Receive on unbound socket raised wrong exception: %s" % x)
        else:
            self.fail("Receive on unbound socket raised exception")

    def testClosedSocket(self):
        self.s.close()
        try:
            self.s.send(MSG)
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.EBADF)

        dup = self.s.dup()
        try:
            dup.send(MSG)
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.EBADF)

        fp = self.s.makefile()
        try:
            fp.write(MSG)
            fp.flush()
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.EBADF)

class TestJythonTCPExceptions(TestJythonExceptionsShared, unittest.TestCase):

    def setUp(self):
        self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    def testConnectionRefused(self):
        try:
            # This port should not be open at this time
            self.s.connect( (HOST, PORT) )
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.ECONNREFUSED)
        except Exception, x:
            self.fail("Connection to non-existent host/port raised wrong exception: %s" % x)
        else:
            self.fail("Socket (%s,%s) should not have been listening at this time" % (HOST, PORT))

    def testBindException(self):
        # First bind to the target port
        self.s.bind( (HOST, PORT) )
        self.s.listen(50)
        try:
            # And then try to bind again
            t = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            t.bind( (HOST, PORT) )
            t.listen(50)
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.EADDRINUSE)
        except Exception, x:
            self.fail("Binding to already bound host/port raised wrong exception: %s" % x)
        else:
            self.fail("Binding to already bound host/port should have raised exception")

class TestJythonUDPExceptions(TestJythonExceptionsShared, unittest.TestCase):

    def setUp(self):
        self.s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    def testBindException(self):
        # First bind to the target port
        self.s.bind( (HOST, PORT) )
        try:
            # And then try to bind again
            t = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            t.bind( (HOST, PORT) )
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.EADDRINUSE)
        except Exception, x:
            self.fail("Binding to already bound host/port raised wrong exception: %s" % x)
        else:
            self.fail("Binding to already bound host/port should have raised exception")

class TestAddressParameters:

    def testBindNonTupleEndpointRaisesTypeError(self):
        try:
            self.socket.bind(HOST, PORT)
        except TypeError:
            pass
        else:
            self.fail("Illegal non-tuple bind address did not raise TypeError")

    def testConnectNonTupleEndpointRaisesTypeError(self):
        try:
            self.socket.connect(HOST, PORT)
        except TypeError:
            pass
        else:
            self.fail("Illegal non-tuple connect address did not raise TypeError")

    def testConnectExNonTupleEndpointRaisesTypeError(self):
        try:
            self.socket.connect_ex(HOST, PORT)
        except TypeError:
            pass
        else:
            self.fail("Illegal non-tuple connect address did not raise TypeError")

class TestTCPAddressParameters(unittest.TestCase, TestAddressParameters):

    def setUp(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

class TestUDPAddressParameters(unittest.TestCase, TestAddressParameters):

    def setUp(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

class UnicodeTest(ThreadedTCPSocketTest):

    def testUnicodeHostname(self):
        pass

    def _testUnicodeHostname(self):
        self.cli.connect((unicode(self.HOST), self.PORT))

class TestInvalidUsage(unittest.TestCase):

    def setUp(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    def testShutdownIOOnListener(self):
        self.socket.listen(50) # socket is now a server socket
        try:
            self.socket.shutdown(socket.SHUT_RDWR)
        except Exception, x:
            self.fail("Shutdown on listening socket should not have raised socket exception, not %s" % str(x))
        else:
            pass

    def testShutdownOnUnconnectedSocket(self):
        try:
            self.socket.shutdown(socket.SHUT_RDWR)
        except socket.error, se:
            self.failUnlessEqual(se[0], errno.ENOTCONN, "Shutdown on unconnected socket should have raised errno.ENOTCONN, not %s" % str(se[0]))
        except Exception, x:
            self.fail("Shutdown on unconnected socket should have raised socket exception, not %s" % str(x))
        else:
            self.fail("Shutdown on unconnected socket should have raised socket exception")

def test_main():
    tests = [
        GeneralModuleTests,
        TestSupportedOptions,
        TestUnsupportedOptions,
        BasicTCPTest,
        TCPServerTimeoutTest,
        TCPClientTimeoutTest,
        TestExceptions,
        TestInvalidUsage,
        TestGetAddrInfo,
        TestTCPAddressParameters,
        TestUDPAddressParameters,
        UDPBindTest,
        BasicUDPTest,
        UDPTimeoutTest,
        NonBlockingTCPTests,
        NonBlockingUDPTests,
        TCPFileObjectClassOpenCloseTests,
        UDPFileObjectClassOpenCloseTests,
        FileAndDupOpenCloseTests,
        FileObjectClassTestCase,
        PrivateFileObjectTestCase,
        UnbufferedFileObjectClassTestCase,
        LineBufferedFileObjectClassTestCase,
        SmallBufferedFileObjectClassTestCase,
        UnicodeTest,
    ]
    if hasattr(socket, "socketpair"):
        tests.append(BasicSocketPairTest)
    if sys.platform[:4] == 'java':
        tests.append(TestJythonTCPExceptions)
        tests.append(TestJythonUDPExceptions)
    # TODO: Broadcast requires permission, and is blocked by some firewalls
    # Need some way to discover the network setup on the test machine
    if False:
        tests.append(UDPBroadcastTest)
    suites = [unittest.makeSuite(klass, 'test') for klass in tests]
    test_support.run_suite(unittest.TestSuite(suites))

if __name__ == "__main__":
    test_main()
