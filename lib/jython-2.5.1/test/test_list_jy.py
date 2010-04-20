import unittest
import random
import threading
import time
from test import test_support

if test_support.is_jython:
    from java.util import ArrayList
    from java.lang import String

class ListTestCase(unittest.TestCase):

    def test_recursive_list_slices(self):
        x = [1,2,3,4,5]
        x[1:] = x
        self.assertEquals(x, [1, 1, 2, 3, 4, 5],
                          "Recursive assignment to list slices failed")

    def test_subclass_richcmp(self):
        # http://bugs.jython.org/issue1115
        class Foo(list):
            def __init__(self, dotstring):
                list.__init__(self, map(int, dotstring.split(".")))
        bar1 = Foo('1.2.3')
        bar2 = Foo('1.2.4')
        self.assert_(bar1 < bar2)
        self.assert_(bar1 <= bar2)
        self.assert_(bar2 > bar1)
        self.assert_(bar2 >= bar1)

    def test_setget_override(self):
        if not test_support.is_jython:
            return

        # http://bugs.jython.org/issue600790
        class GoofyListMapThing(ArrayList):
            def __init__(self):
                self.silly = "Nothing"

            def __setitem__(self, key, element):
                self.silly = "spam"

            def __getitem__(self, key):
                self.silly = "eggs"

        glmt = GoofyListMapThing()
        glmt['my-key'] = String('el1')
        self.assertEquals(glmt.silly, "spam")
        glmt['my-key']
        self.assertEquals(glmt.silly, "eggs")

    def test_tuple_equality(self):
        self.assertEqual([(1,), [1]].count([1]), 1) # http://bugs.jython.org/issue1317

class ThreadSafetyTestCase(unittest.TestCase):

    def run_threads(self, f, num=10):
        threads = []
        for i in xrange(num):
            t = threading.Thread(target=f)
            t.start()
            threads.append(t)
        timeout = 10. # be especially generous
        for t in threads:
            t.join(timeout)
            timeout = 0.
        for t in threads:
            self.assertFalse(t.isAlive())

    def test_append_remove(self):
        # derived from Itamar Shtull-Trauring's test for issue 521701
        lst = []
        def tester():
            ct = threading.currentThread()
            for i in range(1000):
                lst.append(ct)
                time.sleep(0.0001)
                lst.remove(ct)
        self.run_threads(tester)
        self.assertEqual(lst, [])

    def test_sort(self):
        lst = []
        def tester():
            ct = threading.currentThread()
            for i in range(1000):
                lst.append(ct)
                lst.sort()
                lst.remove(ct)
                time.sleep(0.0001)
        self.run_threads(tester)
        self.assertEqual(lst, [])

    def test_count_reverse(self):
        lst = [0,1,2,3,4,5,6,7,8,9,10,0]
        def tester():
            ct = threading.currentThread()
            for i in range(1000):
                self.assertEqual(lst[0], 0)
                if random.random() > 0.5:
                    time.sleep(0.0001)
                lst.reverse()
                self.assertEqual(lst.count(0), 2)
                self.assert_(lst[1] in (1,10))
        self.run_threads(tester)

def test_main():
    test_support.run_unittest(ListTestCase, ThreadSafetyTestCase)


if __name__ == "__main__":
    test_main()
