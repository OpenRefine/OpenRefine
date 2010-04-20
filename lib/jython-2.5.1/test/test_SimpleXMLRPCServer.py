#
# Matt Shelton <matt@mattshelton.com>
#

from SimpleXMLRPCServer import SimpleXMLRPCServer
import threading, xmlrpclib, unittest

HOST = "127.0.0.1"
PORT = 7218

def multiply(x, y):
    return x * y

class MyService:
    """This test class is going to be used to test an entire class being
    exposed via XML-RPC."""

    def _dispatch(self, method, params):
        """This method is called whenever a call is made to the
        service."""
        func = getattr(self, 'expose_' + method)
        return func(*params)

    def expose_squared(self, x):
        """Square"""
        return x * x

class ServerThread(threading.Thread):
    """A test harness for launching a SimpleXMLRPCServer instance in the
       background."""
    def __init__(self, server):
        threading.Thread.__init__(self)
        self.server = server

    def run(self):
        self.server.socket.settimeout(5)
        self.server.allow_reuse_address = 1
        self.server.handle_request()
        self.server.server_close()

class SimpleXMLRPCServerTestCase(unittest.TestCase):
    """Test case for the Python SimpleXMLRPCServer module."""
    def test_exposeLambda(self):
        """Expose a lambda function via XML-RPC."""
        # Create a server instance.
        server = SimpleXMLRPCServer((HOST, PORT))
        server.register_function(lambda x,y: x+y, 'add')
        ServerThread(server).start()

        # Access the exposed service.
        client = xmlrpclib.ServerProxy("http://%s:%d" % (HOST, PORT))
        self.assertEqual(client.add(10, 20), 30)

    def test_exposeFunction1(self):
        """Expose a function via XML-RPC."""
        server = SimpleXMLRPCServer((HOST, PORT + 1))
        server.register_function(multiply)
        ServerThread(server).start()

        # Access the exposed service.
        client = xmlrpclib.ServerProxy("http://%s:%d" % (HOST, PORT + 1))
        self.assertEqual(client.multiply(5, 10), 50)

    def test_exposeFunction2(self):
        """Expose a function using a different name via XML-RPC."""
        server = SimpleXMLRPCServer((HOST, PORT + 2))
        server.register_function(multiply, "mult")
        ServerThread(server).start()

        # Access the exposed service.
        client = xmlrpclib.ServerProxy("http://%s:%d" % (HOST, PORT + 2))
        self.assertEqual(client.mult(7, 11), 77)

    def test_exposeClass(self):
        """Expose an entire class and test the _dispatch method."""
        server = SimpleXMLRPCServer((HOST, PORT + 3))
        server.register_instance(MyService())
        ServerThread(server).start()

        # Access the exposed service.
        client = xmlrpclib.ServerProxy("http://%s:%d" % (HOST, PORT + 3))
        self.assertEqual(client.squared(10), 100)


if __name__ == "__main__":
    unittest.main()

# vim:et:ts=4:sw=4:
