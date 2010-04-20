"""Misc math module tests

Made for Jython.
"""
import math
import unittest
from test import test_support

inf = float('inf')
nan = float('nan')

class MathTestCase(unittest.TestCase):

    def test_frexp(self):
        self.assertEqual(math.frexp(inf), (inf, 0))
        mantissa, exponent = math.frexp(nan)
        self.assertNotEqual(mantissa, mantissa)
        self.assertEqual(exponent, 0)


def test_main():
    test_support.run_unittest(MathTestCase)


if __name__ == '__main__':
    test_main()
