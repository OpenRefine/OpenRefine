"""Misc cPickle tests.

Made for Jython.
"""
import cPickle
import pickle
import unittest
from test import test_support

class MyClass(object):
    pass


class CPickleTestCase(unittest.TestCase):

    def test_zero_long(self):
        self.assertEqual(cPickle.loads(cPickle.dumps(0L, 2)), 0L)
        self.assertEqual(cPickle.dumps(0L, 2), pickle.dumps(0L, 2))

    def test_cyclic_memoize(self):
        # http://bugs.python.org/issue998998 - cPickle shouldn't fail
        # this, though pickle.py still does
        m = MyClass()
        m2 = MyClass()

        s = set([m])
        m.foo = set([m2])
        m2.foo = s

        s2 = cPickle.loads(cPickle.dumps(s))
        self.assertEqual(len(s2), 1)
        m3 = iter(s2).next()
        self.assertEqual(len(m3.foo), 1)
        m4 = iter(m3.foo).next()
        self.assertEqual(m4.foo, s2)


def test_main():
    test_support.run_unittest(CPickleTestCase)


if __name__ == '__main__':
    test_main()
