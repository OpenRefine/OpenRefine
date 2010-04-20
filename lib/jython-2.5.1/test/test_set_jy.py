import unittest
from test import test_support

if test_support.is_jython:
    from java.util import Random
    from javatests import PySetInJavaTest

class SetTestCase(unittest.TestCase):

    def test_binops(self):
        class Foo(object):
            __rsub__ = lambda self, other: 'rsub'
            __ror__ = lambda self, other: 'ror'
            __rand__ = lambda self, other: 'rand'
            __rxor__ = lambda self, other: 'rxor'
        foo = Foo()
        s = set()
        self.assertEqual(s - foo, 'rsub')
        self.assertEqual(s | foo, 'ror')
        self.assertEqual(s & foo, 'rand')
        self.assertEqual(s ^ foo, 'rxor')


class SetInJavaTestCase(unittest.TestCase):

    """Tests for derived dict behaviour"""

    def test_using_PySet_as_Java_Set(self):
        PySetInJavaTest.testPySetAsJavaSet()

    def test_accessing_items_added_in_java(self):
        s = PySetInJavaTest.createPySetContainingJavaObjects()
        for v in s:
            self.assert_(v in s)
            if isinstance(v, unicode):
                self.assertEquals("value", v)
            else:
                # Should be a java.util.Random; ensure we can call it
                v.nextInt()

    def test_java_accessing_items_added_in_python(self):
        # Test a type that should be coerced into a Java type, a Java
        # instance that should be wrapped, and a Python instance that
        # should pass through as itself with str, Random and tuple
        # respectively.
        s = set(["value", Random(), ("tuple", "of", "stuff")])
        PySetInJavaTest.accessAndRemovePySetItems(s)
        # Check that the Java removal affected the underlying set
        self.assertEquals(0, len(s))


def test_main():
    tests = [SetTestCase]
    if test_support.is_jython:
        tests.append(SetInJavaTestCase)
    test_support.run_unittest(*tests)


if __name__ == '__main__':
    test_main()
