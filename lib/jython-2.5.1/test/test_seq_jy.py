"""Additional seq_tests

Made for Jython.
"""
import unittest
from collections import deque
from test import test_support

class SeqTestCase(unittest.TestCase):

    types2test = list, tuple, deque

    def test_seq_item_equality(self):
        eq_called = []
        class Foo(object):
            def __eq__(self, other):
                eq_called.append(other)
                return False
        for type2test in self.types2test:
            foo = Foo()
            seq1 = type2test([foo])
            self.assertEqual(seq1, seq1)
            self.assertEqual(cmp(seq1, seq1), 0)
            seq2 = type2test([foo])
            self.assertEqual(seq1, seq2)
            self.assertEqual(cmp(seq1, seq2), 0)
            self.assertTrue(foo in seq1)
            self.assertFalse(eq_called)

    def test_seq_equality(self):
        class Foo(object):
            def __eq__(self, other):
                return True
        foo = [Foo()]
        for type2test in self.types2test:
            self.assertTrue(type2test() in foo)

    def test_seq_subclass_equality(self):
        # Various combinations of PyObject._eq, overriden Object.equals,
        # and cmp implementations
        for type2test in self.types2test:
            class Foo(type2test):
                def __eq__(self, other):
                    return False
            l = type2test(['bar', 'baz'])
            foo = Foo(l)
            self.assertNotEqual(l, foo)
            self.assertEqual(cmp(l, foo), 1)
            self.assertEqual(cmp(foo, foo), 0)

            seqs1 = type2test([l, foo])
            seqs2 = type2test([l, foo])
            self.assertEqual(seqs1, seqs1)
            self.assertEqual(seqs1, seqs2)
            self.assertEqual(cmp(seqs1, seqs2), 0)
            self.assertTrue(foo in seqs1)
            if hasattr(seqs1, 'count'):
                self.assertTrue(seqs1.count(foo), 1)
            if hasattr(seqs1, 'index'):
                self.assertEqual(seqs1.index(foo), 1)


def test_main():
    test_support.run_unittest(SeqTestCase)


if __name__ == "__main__":
    test_main()
