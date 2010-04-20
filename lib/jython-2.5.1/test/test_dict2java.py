from javatests import Dict2JavaTest
import unittest, test.test_support

# Test the java.util.Map interface of org.python.core.PyDictionary.
# This tests the functionality of being able to pass a dictionaries
# created in Jython to a java method, and the ability to manipulate
# the dictionary object once in Java code.  The Java Dict2JavaTest is
# used to run some tests in Java code since they cannot be done on
# the Jython side.

class JythonMapInJavaTest(unittest.TestCase):

    def checkcontains(self, keys):
        for k in keys:
            self.failUnless(k in self.testdict)
            self.failUnless(self.testmap.containsKey(k))

    def checkdoesntcontain(self, keys):
        for k in keys:
            self.failIf(k in self.testdict)
            self.failIf(self.testmap.containsKey(k))

    def checkvalues(self, *keyvalues):
        for k, v in keyvalues:
            self.assertEquals(v, self.testdict[k])

    def checksize(self, correctsize):
        self.assertEquals(self.testmap.size(), len(self.testdict))
        self.assertEquals(self.testmap.size(), correctsize)

    def maketestdict(self, base):
        self.testdict = base
        self.testmap = Dict2JavaTest(self.testdict)

    def test_basic_map_operations(self):
        self.maketestdict({"a":"x", "b":"y", "c":"z", "d": None, None: "foo"})

        # Make sure we see it on the java side
        self.assertEquals(len(self.testdict), self.testmap.size())
        self.checkcontains('abcd')

        # Add {"e":"1", "f":null, "g":"2"} using the Map.putAll method
        oldlen = len(self.testdict)
        self.failUnless(self.testmap.test_putAll_efg())
        self.checksize(oldlen + 3)
        self.checkvalues(('e', '1'), ('f', None), ('g', '2'))

        # test Map.get method, get "g" and "d" test will throw an exception if fail
        self.failUnless(self.testmap.test_get_gd())

        # remove elements with keys "a" and "c" with the Map.remove method
        oldlen = len(self.testdict)
        self.failUnless(self.testmap.test_remove_ac())
        self.checksize(oldlen - 2)
        self.checkdoesntcontain('ac')

        # test Map.put method, adds {"h":null} and {"i": Integer(3)} and {"g": "3"}
        # "g" replaces a previous value of "2"
        oldlen = len(self.testdict)
        self.failUnless(self.testmap.test_put_hig())
        self.checksize(oldlen + 2)
        self.checkvalues(('h', None), ('i', 3), ('g', '3'))

        self.failUnless(self.testmap.test_java_mapentry())

    def test_entryset(self):
        self.maketestdict({"h":"x", "b":"y", "g":"z", "e": None, None: "foo", "d":7})
        set = self.testmap.entrySet()
        self.checksize(set.size())

        # Make sure the set is consistent with the self.testdictionary
        for entry in set:
            self.failUnless(self.testdict.has_key(entry.getKey()))
            self.assertEquals(self.testdict[entry.getKey()], entry.getValue())
            self.failUnless(set.contains(entry))

        # make sure changes in the set are reflected in the self.testdictionary
        for entry in set:
            if entry.getKey() == "h":
                hentry = entry
            if entry.getKey() == "e":
                eentry = entry

        # Make sure nulls and non Map.Entry object do not match anything in the set
        self.failUnless(self.testmap.test_entry_set_nulls())

        self.failUnless(set.remove(eentry))
        self.failIf(set.contains(eentry))
        self.failIf("e" in self.testdict)
        self.failUnless(set.remove(hentry))
        self.failIf(set.contains(hentry))
        self.failIf("h" in self.testdict)
        self.checksize(set.size())
        oldlen = set.size()
        self.failIf(set.remove(eentry))
        self.checksize(oldlen)

        # test Set.removeAll method
        oldlen = len(self.testdict)
        elist = [ entry for entry in set if entry.key in ["b", "g", "d", None]]
        self.assertEqual(len(elist), 4)
        self.failUnless(set.removeAll(elist))
        self.checkdoesntcontain('bdg')
        # can't check for None in self.testmap, so do it just for testdict
        self.failIf(None in self.testdict)
        self.checksize(oldlen - 4)

        itr = set.iterator()
        while (itr.hasNext()):
            val = itr.next()
            itr.remove()
        self.failUnless(set.isEmpty())
        self.checksize(0)

    def test_keyset(self):
        self.maketestdict({})
        self.testmap.put("foo", "bar")
        self.testmap.put("num", 5)
        self.testmap.put(None, 4.3)
        self.testmap.put(34, None)
        keyset = self.testmap.keySet()
        self.checksize(4)

        self.failUnless(keyset.remove(None))
        self.checksize(3)
        self.failIf(keyset.contains(None))
        self.failUnless(keyset.remove(34))
        self.checksize(2)
        self.failIf(keyset.contains(34))
        itr = keyset.iterator()
        while itr.hasNext():
            key = itr.next()
            if key == "num":
                itr.remove()
        self.checksize(1)

    def test_values(self):
        self.maketestdict({})
        self.testmap.put("foo", "bar")
        self.testmap.put("num", "bar")
        self.testmap.put(None, 3.2)
        self.testmap.put(34, None)
        values = self.testmap.values()
        self.assertEquals(values.size(), len(self.testdict))
        self.checksize(4)

        self.failUnless(values.remove(None))
        self.checksize(3)
        self.assertEquals(values.size(), len(self.testdict))

        itr = values.iterator()
        while itr.hasNext():
            val = itr.next()
            if val == "bar":
                itr.remove()
        self.checksize(1)
        self.assertEquals(values.size(), len(self.testdict))

        values.clear()
        self.failUnless(values.isEmpty())
        self.checksize(0)

def test_main():
    test.test_support.run_unittest(JythonMapInJavaTest)

if __name__ == '__main__':
    test_main()
