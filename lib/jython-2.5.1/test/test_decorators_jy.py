"""Misc decorator related tests

Made for Jython.
"""
from test import test_support
import unittest

class TestDecorators(unittest.TestCase):

    def test_lookup_order(self):
        class Foo(object):
            foo = 'bar'
            @property
            def property(self):
                return self.foo
        self.assertEqual(Foo().property, 'bar')


def test_main():
    test_support.run_unittest(TestDecorators)


if __name__ == '__main__':
    test_main()
