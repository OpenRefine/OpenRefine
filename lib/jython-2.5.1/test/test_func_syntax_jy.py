import unittest
import test.test_support

def parrot(**args): pass

class FuncSyntaxTest(unittest.TestCase):

    def test_keywords_before_normal(self):
        self.assertRaises(SyntaxError, eval,
                "parrot(voltage=.5, \'dead\')")

    def test_dup_keywords(self):
        self.assertRaises(TypeError, eval,
                "complex(imag=4, imag=2)")

def test_main():
    test.test_support.run_unittest(FuncSyntaxTest)

if __name__ == "__main__":
    test_main()
