"""Test concat operations

Made for Jython.
"""
import unittest
import test.test_support

class StrUnicodeConcatTestCase(unittest.TestCase):

    def test_basic(self):
        class s(str):
            pass

        class u(unicode):
            pass

        for t1 in (str, unicode, s, u):
            for t2 in (str, unicode, s, u):
                a = t1('a')
                b = t2('b')
                resType = str
                if issubclass(t1, unicode) or issubclass(t2, unicode):
                    resType = unicode
                res = a.__add__(b)
                self.assertEquals(type(res), resType,
                                  '%r is a %s, not a %s' % (res, type(res),
                                                            resType))
                self.assertEquals(res, 'ab',
                                  '%r (%s) != %r (%s)' % (res, type(res), 'ab',
                                                 str))


class StrUnicodeConcatOverridesTestCase(unittest.TestCase):

    def test_str_concat(self):
        self._test_concat(str, str)

    def test_unicode_concat(self):
        self._test_concat(unicode, unicode)

    def test_str_unicode_concat(self):
        self._test_concat(str, unicode)

    def test_unicode_str_concat(self):
        self._test_concat(unicode, str)

    def check(self, value, expected):
        self.assertEqual(type(value), type(expected),
                         '%r is a %s, not a %s' % (value, type(value),
                                                   type(expected)))
        self.assertEqual(value, expected,
                         '%r (%s) != %r (%s)' % (value, type(value), expected,
                                                 type(expected)))

    def _test_concat(self, t1, t2):
        tprecedent = str
        if issubclass(t1, unicode) or issubclass(t2, unicode):
            tprecedent = unicode

        class SubclassB(t2):
            def __add__(self, other):
                return SubclassB(t2(self) + t2(other))
        self.check(SubclassB('py') + SubclassB('thon'), SubclassB('python'))
        self.check(t1('python') + SubclassB('3'), tprecedent('python3'))
        self.check(SubclassB('py') + t1('py'), SubclassB('pypy'))

        class SubclassC(t2):
            def __radd__(self, other):
                return SubclassC(t2(other) + t2(self))
        self.check(SubclassC('stack') + SubclassC('less'), t2('stackless'))
        self.check(t1('iron') + SubclassC('python'), SubclassC('ironpython'))
        self.check(SubclassC('tiny') + t1('py'), tprecedent('tinypy'))

        class SubclassD(t2):
            def __add__(self, other):
                return SubclassD(t2(self) + t2(other))

            def __radd__(self, other):
                return SubclassD(t2(other) + t2(self))
        self.check(SubclassD('di') + SubclassD('ct'), SubclassD('dict'))
        self.check(t1('list') + SubclassD(' comp'), SubclassD('list comp'))
        self.check(SubclassD('dun') + t1('der'), SubclassD('dunder'))


def test_main():
    test.test_support.run_unittest(StrUnicodeConcatTestCase,
                                   StrUnicodeConcatOverridesTestCase)

if __name__ == "__main__":
    test_main()
