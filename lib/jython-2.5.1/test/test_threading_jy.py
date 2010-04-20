"""Misc threading module tests

Made for Jython.
"""
import unittest
from test import test_support
import threading
import time
import random
from threading import Thread

class ThreadingTestCase(unittest.TestCase):

    def test_str_name(self):
        t = Thread(name=1)
        self.assertEqual(t.getName(), '1')
        t.setName(2)
        self.assertEqual(t.getName(), '2')

    # make sure activeCount() gets decremented (see issue 1348)
    def test_activeCount(self):
        activeBefore = threading.activeCount()
        activeCount = 10
        for i in range(activeCount):
            t = Thread(target=self._sleep, args=(i,))
            t.setDaemon(0)
            t.start()
        polls = activeCount
        while activeCount > activeBefore and polls > 0:
            time.sleep(1)
            activeCount = threading.activeCount()
            polls -= 1
        self.assertTrue(activeCount <= activeBefore, 'activeCount should to be <= %s, instead of %s' % (activeBefore, activeCount))

    def _sleep(self, n):
        time.sleep(random.random())


class TwistedTestCase(unittest.TestCase):
    
    def test_needs_underscored_versions(self):
        self.assertEqual(threading.Lock, threading._Lock)
        self.assertEqual(threading.RLock, threading._RLock)

def test_main():
    test_support.run_unittest(ThreadingTestCase, TwistedTestCase)


if __name__ == "__main__":
    test_main()
