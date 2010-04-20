"""Misc iterator tests.

Made for Jython.
"""
import itertools
from test import test_support
import unittest

class IterTestCase(unittest.TestCase):

    def test_fastiter(self):
        class MyList(list):
            def __getitem__(self, index):
                return str(index) + '!'
        class MyTuple(tuple):
            def __getitem__(self, index):
                return str(index) + '!'
        self.assertEqual(iter(MyList(['a', 'b'])).next(), 'a')
        self.assertEqual(iter(MyTuple(['a', 'b'])).next(), 'a')

    def test_slowiter(self):
        class MyStr(str):
            def __getitem__(self, index):
                return str(index) + '!'
        self.assertEqual(iter(MyStr('ab')).next(), '0!')

    def test_chain(self):
        self.assertEqual(list(itertools.chain([], [], ['foo'])), ['foo'])


def test_main():
    test_support.run_unittest(IterTestCase)


if __name__ == '__main__':
    test_main()
