"Tests for cmp() compatibility with CPython"
import UserDict
import unittest
from test import test_support

class CmpGeneralTestCase(unittest.TestCase):

    def test_type_crash(self):
        # Used to throw ArrayStoreException:
        # http://bugs.jython.org/issue1382
        class Configuration(object, UserDict.DictMixin):
            pass
        self.assertNotEqual(Configuration(), None)


class UnicodeDerivedCmp(unittest.TestCase):
    "Test for http://bugs.jython.org/issue1889394"
    def testCompareWithString(self):
        class Test(unicode):
            pass
        test = Test('{1:1}')
        self.assertNotEqual(test, {1:1})
    def testCompareEmptyDerived(self):
        class A(unicode): pass
        class B(unicode): pass
        self.assertEqual(A(), B())


class LongDerivedCmp(unittest.TestCase):
    def testCompareWithString(self):
        class Test(long):
            pass
        self.assertNotEqual(Test(0), 'foo')
        self.assertTrue('foo' in [Test(12), 'foo'])


class IntStrCmp(unittest.TestCase):
    def testIntStrCompares(self):
        assert not (-1 > 'a')
        assert (-1 < 'a')
        assert not (4 > 'a')
        assert (4 < 'a')
        assert not (-2 > 'a')
        assert (-2 < 'a')
        assert not (-1 == 'a')


class CustomCmp(unittest.TestCase):
    def test___cmp___returns(self):
        class Foo(object):
            def __int__(self):
                return 0
        class Bar(object):
            def __int__(self):
                raise ValueError('doh')
        class Baz(object):
            def __cmp__(self, other):
                return self.cmp(other)
        baz = Baz()
        baz.cmp = lambda other : Foo()
        self.assertEqual(cmp(100, baz), 0)
        baz.cmp = lambda other : NotImplemented
        self.assertEqual(cmp(100, baz), 1)
        baz.cmp = lambda other: Bar()
        self.assertRaises(ValueError, cmp, 100, baz)
        baz.cmp = lambda other: 1 / 0
        self.assertRaises(ZeroDivisionError, cmp, 100, baz)
        del Baz.__cmp__
        # CPython handles numbers differently than other types in
        # object.c:default_3way_compare, and gets 1 here. we don't care
        self.assert_(cmp(100, baz) in (-1, 1))

    def test_cmp_stops_short(self):
        class Foo(object):
            __eq__ = lambda self, other: False
        class Bar(object):
            __eq__ = lambda self, other: True
        self.assertEqual(cmp(Foo(), Bar()), 1)


def test_main():
    test_support.run_unittest(
            CmpGeneralTestCase,
            UnicodeDerivedCmp,
            LongDerivedCmp,
            IntStrCmp,
            CustomCmp
            )


if __name__ == '__main__':
    test_main()
