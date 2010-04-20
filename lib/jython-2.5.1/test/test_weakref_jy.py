"""Misc weakref tests

Made for Jython.
"""
import unittest
import weakref
from test import test_support

class ReferencesTestCase(unittest.TestCase):

    def test___eq__(self):
        class Foo(object):
            def __eq__(self, other):
                return True
            def __hash__(self):
                return hash('foo')
        foo1, foo2 = Foo(), Foo()
        ref1, ref2 = weakref.ref(foo1), weakref.ref(foo2)
        self.assertTrue(ref1() is foo1)
        self.assertTrue(ref2() is foo2)

    def test___hash__call(self):
        hash_called = []
        class Bar(object):
            def __hash__(self):
                hash = object.__hash__(self)
                hash_called.append(hash)
                return hash
        bar = Bar()
        ref = weakref.ref(bar)
        self.assertFalse(hash_called)

        hash(ref)
        self.assertEqual(len(hash_called), 1)
        hash(ref)
        self.assertEqual(len(hash_called), 1)
        self.assertEqual(hash(bar), hash(ref))
        self.assertEqual(len(hash_called), 2)


def test_main():
    test_support.run_unittest(ReferencesTestCase)


if __name__ == '__main__':
    test_main()
