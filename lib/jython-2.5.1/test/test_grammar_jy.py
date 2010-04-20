""" Extra grammar tests for Jython.
"""

from test import test_support
import unittest

class GrammarTest(unittest.TestCase):
    def test_triple_quote_len(self):
        s1 = r"""
        \""" 1.triple-quote
        \""" 2.triple-quote
        """

        s2 = r'''
        \""" 1.triple-quote
        \""" 2.triple-quote
        '''
        self.assert_(not '\r' in s1)
        self.assertEquals(len(s1), len(s2))

    def testStringPrefixes(self):
        self.assertEquals(u"spam",U"spam")
        self.assertEquals(r"spam", R"spam")
        self.assertEquals(uR"spam", Ur"spam")
        self.assertEquals(ur"spam", UR"spam")

    def testKeywordOperations(self):
        def foo(a=1, b=2 + 4):
            return b
        self.assertEquals(6, foo())
        self.assertEquals(6, foo(1))
        self.assertEquals(7, foo(1, 7))
        self.assertEquals(10, foo(b=10))

def test_main():
    test_support.run_unittest(GrammarTest)

if __name__ == '__main__':
    test_main()
