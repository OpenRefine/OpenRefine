from test import test_support
import java
import unittest

class DictInitTest(unittest.TestCase):
    def testInternalSetitemInInit(self):
        """Test for http://jython.org/bugs/1816134

        CPython's dict uses an internal setitem method to initialize itself
        rather than the one on its subclasses, and this tests that Jython does
        as well.
        """
        class Subdict(dict):
            def __init__(self):
                super(Subdict, self).__init__([('a',1)])
                self.createdInInit = 1

            def __setitem__(self, key, value):
                super(Subdict, self).__setitem__(key, value)
                assert hasattr(self, 'createdInInit')
                self.createdInInit = value

        s = Subdict()
        s[7] = 'called'
        self.assertEquals('called', s.createdInInit)

    def testUnhashableKeys(self):
        try:
            a = {[1]:2}
        except TypeError:
            pass
        else:
            self.fail("list as dict key should raise TypeError")

        try:
            a = {{1:2}:3}
        except TypeError:
            pass
        else:
            self.fail("dict as dict key should raise TypeError")

class DictCmpTest(unittest.TestCase):
    "Test for http://bugs.jython.org/issue1031"
    def testDictCmp(self):
        # 'Implicit' comparision of dicts against other types instances
        # shouldn't raise exception:
        self.assertNotEqual({}, '')
        # The same, but explicitly calling __cmp__ should raise TypeError:
        self.assertRaises(TypeError, {}.__cmp__, '')
    def testDictDerivedCmp(self):
        # With derived classes that doesn't override __cmp__, the behaviour
        # should be the same that with dicts:
        class derived_dict(dict): pass
        self.assertEqual(derived_dict(), {})
        self.assertNotEqual(derived_dict(), '')
        self.assertRaises(TypeError, derived_dict().__cmp__, '')
        # But, if they *override* __cmp__ and raise TypeError from there, we
        # have exception raised when checking for equality...
        class non_comparable_dict(dict):
            def __cmp__(self, other):
                raise TypeError, "I always raise TypeError"
        self.assertRaises(TypeError, lambda: non_comparable_dict() == '')
        self.assertRaises(TypeError, non_comparable_dict().__cmp__, '')
        # ...unless you compare it with other dicts:
        # self.assertEqual(non_comparable_dict(), {})

        # The same happens even if the overridden __cmp__ doesn't nothing apart
        # from calling super:
        class dummy_dict_with_cmp(dict):
            def __cmp__(self, other):
                return super(dummy_dict_with_cmp, self).__cmp__(other)

        self.assertEqual(dummy_dict_with_cmp(), {})
        # But TypeError is raised when comparing against other types
        self.assertRaises(TypeError, lambda: dummy_dict_with_cmp() == '')
        self.assertRaises(TypeError, dummy_dict_with_cmp().__cmp__, '')
        # Finally, the Python implementation shouldn't be tricked by not
        # implementing __cmp__ on the actual type of the dict-derived instance,
        # but implementing it on a superclass.
        class derived_dict_with_custom_cmp(dict):
            def __cmp__(self, other):
                return 0
        class yet_another_dict(derived_dict_with_custom_cmp): pass
        self.assertEqual(derived_dict_with_custom_cmp(), '')
        self.assertEqual(yet_another_dict(), '')

class DerivedDictTest(unittest.TestCase):
    "Tests for derived dict behaviour"
    def test_raising_custom_key_error(self):
        class CustomKeyError(KeyError):
            pass
        class DerivedDict(dict):
            def __getitem__(self, key):
                raise CustomKeyError("custom message")
        self.assertRaises(CustomKeyError, lambda: DerivedDict()['foo'])

class JavaIntegrationTest(unittest.TestCase):
    "Tests for instantiating dicts from Java maps and hashtables"
    def test_hashmap(self):
        x = java.util.HashMap()
        x.put('a', 1)
        x.put('b', 2)
        x.put('c', 3)
        x.put((1,2), "xyz")
        y = dict(x)
        self.assertEqual(set(y.items()), set([('a', 1), ('b', 2), ('c', 3), ((1,2), "xyz")]))
    def test_hashtable(self):
        x = java.util.Hashtable()
        x.put('a', 1)
        x.put('b', 2)
        x.put('c', 3)
        x.put((1,2), "xyz")
        y = dict(x)
        self.assertEqual(set(y.items()), set([('a', 1), ('b', 2), ('c', 3), ((1,2), "xyz")]))



def test_main():
    test_support.run_unittest(DictInitTest, DictCmpTest, DerivedDictTest, JavaIntegrationTest)

if __name__ == '__main__':
    test_main()
