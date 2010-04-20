# This test is temporary until we can import test_decorators from CPython 3.x
# The reason for not doing that already is that in Python 3.x the name of a
# function is stored in func.__name__, in 2.x it's func.func_name
import unittest
from test import test_support

class TestClassDecorators(unittest.TestCase):

    def test_simple(self):
        def plain(x):
            x.extra = 'Hello'
            return x
        @plain
        class C(object): pass
        self.assertEqual(C.extra, 'Hello')

    def test_double(self):
        def ten(x):
            x.extra = 10
            return x
        def add_five(x):
            x.extra += 5
            return x

        @add_five
        @ten
        class C(object): pass
        self.assertEqual(C.extra, 15)

    def test_order(self):
        def applied_first(x):
            x.extra = 'first'
            return x
        def applied_second(x):
            x.extra = 'second'
            return x
        @applied_second
        @applied_first
        class C(object): pass
        self.assertEqual(C.extra, 'second')

def test_main():
    test_support.run_unittest(TestClassDecorators)

if __name__ == '__main__':
    test_main()
