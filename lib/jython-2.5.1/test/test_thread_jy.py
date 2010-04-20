import thread
import synchronize
import unittest
import test.test_support
from java.lang import Runnable, Thread
from java.util.concurrent import CountDownLatch

class AllocateLockTest(unittest.TestCase):

    def test_lock_type(self):
        "thread.LockType should exist"
        t = thread.LockType
        self.assertEquals(t, type(thread.allocate_lock()),
            "thread.LockType has wrong value")

class SynchronizeTest(unittest.TestCase):
    def test_make_synchronized(self):
        doneSignal = CountDownLatch(10)
        class SynchedRunnable(Runnable):
            i = 0
            def run(self):
                self.i += 1
                doneSignal.countDown()
            run = synchronize.make_synchronized(run)
        runner = SynchedRunnable()
        for _ in xrange(10):
            Thread(runner).start()
        doneSignal.await()
        self.assertEquals(10, runner.i)


def test_main():
    test.test_support.run_unittest(AllocateLockTest, SynchronizeTest)

if __name__ == "__main__":
    test_main()
