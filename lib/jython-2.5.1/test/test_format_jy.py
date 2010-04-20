"""String foramtting tests

Made for Jython.
"""
from test import test_support
import unittest

class FormatTestCase(unittest.TestCase):
    # Tests that %d converts values for custom classes implementing __int__
    def test_int_conversion_support(self):
        class Foo(object):
            def __init__(self, x): self.x = x
            def __int__(self): return self. x
        self.assertEqual('1', '%d' % Foo(1))
        self.assertEqual('1', '%d' % Foo(1L)) # __int__ can return a long, but
                                              # it should be accepted too

    def test_float_conversion_support(self):
        class Foo(object):
            def __init__(self, x): self.x = x
            def __float__(self): return self. x
        self.assertEqual('1.0', '%.1f' % Foo(1.0))


def test_main():
    test_support.run_unittest(FormatTestCase)

if __name__ == '__main__':
    test_main()
