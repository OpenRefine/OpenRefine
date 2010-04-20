import unittest
from test import test_support

class BadEncodingTest(unittest.TestCase):

    def test_invalid_default(self):
        self.assertRaises(SyntaxError, __import__, "test.latin1_no_encoding")

    def test_invalid_declared_encoding(self):
        self.assertRaises(SyntaxError, __import__, "test.invalid_utf_8_declared_encoding")

def test_main():
    test_support.run_unittest(BadEncodingTest)

if __name__=="__main__":
    test_main()
