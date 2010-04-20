import sys
import threading
import time
import unittest

from test import test_support

class UntracedThread(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

    def run(self):
        sys.settrace(None)
        for i in range(10):
            self.untracedcall()

    def untracedcall(self):
        pass

class TracedThread(threading.Thread):
    def __init__(self, on_trace):
        threading.Thread.__init__(self)
        self.on_trace = on_trace

    def trace(self, frame, event, arg):
        self.on_trace(frame.f_code.co_name)

    def tracedcall(self):
        pass

    def run(self):
        sys.settrace(self.trace)
        for i in range(10):
            self.tracedcall()

class TracePerThreadTest(unittest.TestCase):
    def testTracePerThread(self):
        called = []
        def ontrace(co_name):
            called.append(str(co_name))

        untraced = UntracedThread()
        traced = TracedThread(ontrace)
        untraced.start()
        traced.start()
        untraced.join()
        traced.join()

        self.assertEquals(10, called.count('tracedcall'),
                "10 tracedcall should be in %s" % called)
        self.assert_('untracedcall' not in called,
                "untracedcall shouldn't be in %s" % called)

def test_main():
    test_support.run_unittest(TracePerThreadTest)

if __name__ == "__main__":
    test_main()
