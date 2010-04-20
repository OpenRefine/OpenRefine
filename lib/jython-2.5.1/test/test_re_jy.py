import re
import unittest
import test.test_support

class ReTest(unittest.TestCase):

    def test_bug_1140_addendum(self):
        result = re.sub('', lambda match : None, 'foo')
        self.assertEqual(result, 'foo')
        self.assert_(isinstance(result, str))

    def test_sub_with_subclasses(self):
        class Foo(unicode):
            def join(self, items):
                return Foo(unicode.join(self, items))
        result = re.sub('bar', 'baz', Foo('bar'))
        self.assertEqual(result, u'baz')
        self.assertEqual(type(result), unicode)

        class Foo2(unicode):
            def join(self, items):
                return Foo2(unicode.join(self, items))
            def __getslice__(self, start, stop):
                return Foo2(unicode.__getslice__(self, start, stop))
        result = re.sub('bar', 'baz', Foo2('bar'))
        self.assertEqual(result, Foo2('baz'))
        self.assert_(isinstance(result, Foo2))

    def test_unkown_groupname(self):
        self.assertRaises(IndexError,
                          re.match("(?P<int>\d+)\.(\d*)", '3.14').group,
                          "misspelled")

def test_main():
    test.test_support.run_unittest(ReTest)

if __name__ == "__main__":
    test_main()
