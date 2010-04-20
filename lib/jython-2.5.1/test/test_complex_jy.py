"""Misc complex tests

Made for Jython.
"""
import unittest
from test import test_support

class ComplexTest(unittest.TestCase):

    def test_dunder_coerce(self):
        self.assertEqual(complex.__coerce__(1+1j, None), NotImplemented)
        self.assertRaises(TypeError, complex.__coerce__, None, 1+2j)

    def test_pow(self):
        class Foo(object):
            def __rpow__(self, other):
                return other ** 2
        # regression in 2.5 alphas
        self.assertEqual((4+0j) ** Foo(), (16+0j))

    def test___nonzero__(self):
        self.assertTrue(0.25+0j)
        self.assertTrue(25j)


def test_main():
    test_support.run_unittest(ComplexTest)

if __name__ == "__main__":
    test_main()
