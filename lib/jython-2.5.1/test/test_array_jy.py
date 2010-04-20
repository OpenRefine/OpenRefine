# The jarray module is being phased out, with all functionality
# now available in the array module.
from __future__ import with_statement
import os
import unittest
from test import test_support
from array import array, zeros
from java.lang import String
from java.lang.reflect import Array
from java.util import Arrays

class ArrayJyTestCase(unittest.TestCase):

    def test_jarray(self): # until it is fully formally removed
        # While jarray is still being phased out, just flex the initializers.
        # The rest of the test for array will catch all the big problems.
        import jarray
        jarray.array(range(5), 'i')
        jarray.array([String("a"), String("b"), String("c")], String)
        jarray.zeros(5, 'i')
        jarray.zeros(5, String)

    def test_java_object_arrays(self):
        jStringArr = array(String, [String("a"), String("b"), String("c")])
        self.assert_(
            Arrays.equals(jStringArr.typecode, 'java.lang.String'),
               "String array typecode of wrong type, expected %s, found %s" %
               (jStringArr.typecode, str(String)))
        self.assertEqual(zeros(String, 5), Array.newInstance(String, 5))

        import java.lang.String # require for eval to work
        self.assertEqual(jStringArr, eval(str(jStringArr)))

    def test_java_compat(self):
        from java.awt import Color
        hsb = Color.RGBtoHSB(0,255,255, None)
        self.assertEqual(hsb, array('f', [0.5,1,1]),
                         "output hsb float array does not correspond to input rgb values")

        rgb = apply(Color.HSBtoRGB, tuple(hsb))
        self.assertEqual(rgb, -0xff0001,
                         "output rgb bytes don't match input hsb floats")
        hsb1 = zeros('f', 3)
        Color.RGBtoHSB(0, 255, 255, hsb1)
        self.assertEqual(hsb, hsb1, "hsb float arrays were not equal")


class ToFromfileTestCase(unittest.TestCase):

    def tearDown(self):
        if os.path.exists(test_support.TESTFN):
            os.remove(test_support.TESTFN)

    def test_tofromfile(self):
        # http://bugs.jython.org/issue1457
        x = array('i', range(5))
        with open(test_support.TESTFN, 'wb') as f:
            x.tofile(f)

        x = array('i', [])
        with open(test_support.TESTFN, 'r+b') as f:
            x.fromfile(f, 5)
            x *= 2
            x.tofile(f)

        with open(test_support.TESTFN, 'rb') as f:
            x.fromfile(f, 10)
            self.assertEqual(x, array('i', range(5) * 4))


def test_main():
    test_support.run_unittest(ArrayJyTestCase,
                              ToFromfileTestCase)


if __name__ == "__main__":
    test_main()
