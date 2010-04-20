"""Misc zlib tests

Made for Jython.
"""
import unittest
import zlib
from array import array
from test import test_support

class ArrayTestCase(unittest.TestCase):

    def test_array(self):
        self._test_array(zlib.compress, zlib.decompress)

    def test_array_compressobj(self):
        def compress(value):
            co = zlib.compressobj()
            return co.compress(value) + co.flush()
        def decompress(value):
            dco = zlib.decompressobj()
            return dco.decompress(value) +  dco.flush()
        self._test_array(compress, decompress)

    def _test_array(self, compress, decompress):
        self.assertEqual(compress(array('c', 'jython')), compress('jython'))
        intarray = array('i', range(5))
        self.assertEqual(compress(intarray), compress(intarray.tostring()))
        compressed = array('c', compress('jython'))
        self.assertEqual('jython', decompress(compressed))


def test_main():
    test_support.run_unittest(ArrayTestCase)


if __name__ == '__main__':
    test_main()
