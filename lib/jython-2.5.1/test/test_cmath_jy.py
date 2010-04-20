#! /usr/bin/env python
""" Simple test script for cmathmodule.c
    Roger E. Masse
"""
import cmath
import unittest
from test import test_support
from test.test_support import verbose

p = cmath.pi
e = cmath.e
if verbose:
    print 'PI = ', abs(p)
    print 'E = ', abs(e)


class CmathTestCase(unittest.TestCase):
    def assertAlmostEqual(self, x, y, places=5, msg=None):
        unittest.TestCase.assertAlmostEqual(self, x.real, y.real, places, msg)
        unittest.TestCase.assertAlmostEqual(self, x.imag, y.imag, places, msg)

    def test_acos(self):
        self.assertAlmostEqual(complex(0.936812, -2.30551),
                               cmath.acos(complex(3, 4)))

    def test_acosh(self):
        self.assertAlmostEqual(complex(2.30551, 0.93681),
                               cmath.acosh(complex(3, 4)))

    def test_asin(self):
        self.assertAlmostEqual(complex(0.633984, 2.30551),
                               cmath.asin(complex(3, 4)))

    def test_asinh(self):
        self.assertAlmostEqual(complex(2.29991, 0.917617),
                               cmath.asinh(complex(3, 4)))

    def test_atan(self):
        self.assertAlmostEqual(complex(1.44831, 0.158997),
                               cmath.atan(complex(3, 4)))

    def test_atanh(self):
        self.assertAlmostEqual(complex(0.11750, 1.40992),
                               cmath.atanh(complex(3, 4)))

    def test_cos(self):
        self.assertAlmostEqual(complex(-27.03495, -3.851153),
                               cmath.cos(complex(3, 4)))

    def test_cosh(self):
        self.assertAlmostEqual(complex(-6.58066, -7.58155),
                               cmath.cosh(complex(3, 4)))

    def test_exp(self):
        self.assertAlmostEqual(complex(-13.12878, -15.20078),
                               cmath.exp(complex(3, 4)))

    def test_log(self):
        self.assertAlmostEqual(complex(1.60944, 0.927295),
                               cmath.log(complex(3, 4)))

    def test_log10(self):
        self.assertAlmostEqual(complex(0.69897, 0.40272),
                               cmath.log10(complex(3, 4)))

    def test_sin(self):
        self.assertAlmostEqual(complex(3.853738, -27.01681),
                               cmath.sin(complex(3, 4)))

    def test_sinh(self):
        self.assertAlmostEqual(complex(-6.54812, -7.61923),
                               cmath.sinh(complex(3, 4)))

    def test_sqrt_real_positive(self):
        self.assertAlmostEqual(complex(2, 1),
                               cmath.sqrt(complex(3, 4)))

    def test_sqrt_real_zero(self):
        self.assertAlmostEqual(complex(1.41421, 1.41421),
                               cmath.sqrt(complex(0, 4)))

    def test_sqrt_real_negative(self):
        self.assertAlmostEqual(complex(1, 2),
                               cmath.sqrt(complex(-3, 4)))

    def test_sqrt_imaginary_zero(self):
        self.assertAlmostEqual(complex(0.0, 1.73205),
                               cmath.sqrt(complex(-3, 0)))

    def test_sqrt_imaginary_negative(self):
        self.assertAlmostEqual(complex(1.0, -2.0),
                               cmath.sqrt(complex(-3, -4)))

    def test_tan(self):
        self.assertAlmostEqual(complex(-0.000187346, 0.999356),
                               cmath.tan(complex(3, 4)))

    def test_tanh(self):
        self.assertAlmostEqual(complex(1.00071, 0.00490826),
                               cmath.tanh(complex(3, 4)))

def test_main():
    test_support.run_unittest(CmathTestCase)

if __name__ == "__main__":
    test_main()
