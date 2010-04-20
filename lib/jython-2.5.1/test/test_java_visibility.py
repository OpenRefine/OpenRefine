import array
import unittest
import subprocess
import sys
from test import test_support
from java.lang import Byte, Class, Integer
from java.util import ArrayList, Collections, HashMap, LinkedList, Observable, Observer
from org.python.tests import (Coercions, HiddenSuper, InterfaceCombination, Invisible, Matryoshka,
        OnlySubclassable, OtherSubVisible, SomePyMethods, SubVisible, Visible, VisibleOverride)
from org.python.tests import VisibilityResults as Results
from org.python.tests.RedundantInterfaceDeclarations import (Implementation, ExtraClass,
        ExtraString, ExtraStringAndClass, ExtraClassAndString)
from org.python.tests.multihidden import BaseConnection

class VisibilityTest(unittest.TestCase):
    def test_invisible(self):
        for item in dir(Invisible):
            self.assert_(not item.startswith("package"))
            self.assert_(not item.startswith("private"))
            self.assert_(not item.startswith("protected"))
        self.assertRaises(TypeError, Invisible,
                "Calling a Java class with package protected constructors should raise a TypeError")

    def test_protected_from_python_subclass(self):
        class PySubVisible(Visible):
            def __init__(self, publicValue=None):
                if publicValue is not None:
                    Visible.__init__(self, publicValue)
                else:
                    Visible.__init__(self)
        class SubPySubVisible(PySubVisible):
            pass
        # TODO - protectedStaticMethod, protectedStaticField, StaticInner, and protectedField should
        # be here
        for cls in PySubVisible, SubPySubVisible:
            s = cls()
            self.assertEquals(Results.PROTECTED_METHOD, s.protectedMethod(0))
            self.assertEquals(Results.OVERLOADED_PROTECTED_METHOD, s.protectedMethod('foo'))
            self.assertEquals(Results.UNUSED, PySubVisible(Results.UNUSED).visibleField)
            self.assertRaises(TypeError, OnlySubclassable,
                    "Calling a Java class with protected constructors should raise a TypeError")
        class SubSubclassable(OnlySubclassable):
            pass
        sub = SubSubclassable()
        self.assert_(not sub.filledInByConstructor == 0,
                '''Creating SubSubclassable should call OnlySubclassable's constructor to fill in
                filledInByConstructor''')

        # Check that the protected setChanged method on Observable is visible and propogates
        # properly from a python subclass
        class TestObservable(Observable):
            def __init__(self):
                self.props = {}
            def set(self, key, val):
                self.props[key] = val
                self.setChanged()
                self.notifyObservers()

        to = TestObservable()
        self.updated = False
        class TestObserver(Observer):
            def update(observerself, observable, arg):
                self.assertEquals(to, observable)
                self.assertEquals(None, arg)
                self.updated = True
        to.addObserver(TestObserver())
        to.set('k', 'v')
        self.assert_(self.updated, "Calling set should notify the added observer")

    def test_visible(self):
        v = Visible()
        self.assertEquals(Results.PUBLIC_FIELD, v.visibleField)
        self.assertEquals(Results.PUBLIC_STATIC_FIELD, Visible.visibleStaticField)
        Visible.visibleStaticField = Results.PUBLIC_STATIC_FIELD + 1
        self.assertEquals(Results.PUBLIC_STATIC_FIELD + 1, Visible.visibleStaticField)
        self.assertEquals(Results.PUBLIC_STATIC_FIELD + 1, Visible.getVisibleStaticField())
        Visible.setVisibleStaticField(Results.PUBLIC_STATIC_FIELD)
        self.assertEquals(Results.PUBLIC_STATIC_FIELD, Visible.visibleStaticField)
        self.assertEquals(Results.PUBLIC_METHOD, v.visibleInstance(0))
        self.assertEquals(Results.OVERLOADED_PUBLIC_METHOD, v.visibleInstance('a'))
        self.assertEquals(Results.EXTRA_ARG_PUBLIC_METHOD, v.visibleInstance(0, 'b'))
        self.assertEquals(Results.OVERLOADED_EXTRA_ARG_PUBLIC_METHOD,
                v.visibleInstance('a', 'b'))
        self.assertEquals(Results.PUBLIC_STATIC_METHOD, Visible.visibleStatic(0))
        self.assertEquals(Results.OVERLOADED_PUBLIC_STATIC_METHOD,
                v.visibleStatic('a'))
        self.assertEquals(Results.EXTRA_ARG_PUBLIC_STATIC_METHOD,
                v.visibleStatic(0, 'a'))
        self.assertEquals(Results.PUBLIC_STATIC_FIELD, Visible.StaticInner.visibleStaticField)

        # Ensure that the visibleInstance method from SubVisible that takes a double doesn't
        # leak through to the parent
        self.assertRaises(TypeError, v.visibleInstance, 0.0, 'b')
        # TODO - no way to access a field with the same name as a method
        #self.assertEquals(Results.PUBLIC_METHOD_FIELD, v.visibleInstance)
        #self.assertEquals(Results.PUBLIC_STATIC_METHOD_FIELD, Visible.visibleStatic)

    def test_java_subclass(self):
        s = SubVisible()
        self.assertEquals(Results.PUBLIC_FIELD, s.visibleField)
        self.assertEquals(Results.PUBLIC_STATIC_FIELD, SubVisible.visibleStaticField)
        self.assertEquals(Results.SUBCLASS_STATIC_OVERRIDE, SubVisible.visibleStatic(3))
        self.assertEquals(Results.SUBCLASS_STATIC_OVERLOAD, SubVisible.visibleStatic(3.0, 'a'))
        self.assertEquals(Results.SUBCLASS_OVERRIDE, s.visibleInstance(3))
        self.assertEquals(Results.SUBCLASS_OVERLOAD, s.visibleInstance(3.0, 'a'))
        self.assertEquals(Results.PACKAGE_METHOD, s.packageMethod())
        # Java methods don't allow direct calling of the superclass method, so it should
        # return the subclass value here.
        self.assertEquals(Results.SUBCLASS_OVERRIDE, Visible.visibleInstance(s, 3))
        self.assertEquals(Results.PUBLIC_STATIC_FIELD, SubVisible.StaticInner.visibleStaticField)

        self.assertEquals(Results.VISIBLE_SHARED_NAME_FIELD, Visible.sharedNameField)
        self.assertEquals(Results.SUBVISIBLE_SHARED_NAME_FIELD, SubVisible.sharedNameField)
        self.assertEquals(Results.VISIBLE_SHARED_NAME_FIELD * 10, Visible().sharedNameField)
        self.assertEquals(Results.SUBVISIBLE_SHARED_NAME_FIELD * 10, s.sharedNameField)


    def test_in_dict(self):
        for c in Visible, SubVisible, VisibleOverride:
            self.failUnless('visibleInstance' in c.__dict__,
                    'visibleInstance expected in %s __dict__' % c)

    def test_interface_combination(self):
        '''Checks that a private class that extends a public class and public interfaces has only the items
           from the public bases visible'''
        i = InterfaceCombination.newImplementation()
        self.assertEquals(InterfaceCombination.NO_ARG_RESULT, i.getValue(),
                "methods from IFace should be visible on Implementation")
        self.assertEquals(InterfaceCombination.ONE_ARG_RESULT, i.getValue("one arg"),
                "methods from IIFace should be visible on Implementation")
        self.assertEquals(InterfaceCombination.TWO_ARG_RESULT, i.getValue("one arg", "two arg"),
                "methods from Base should be visible on Implementation")
        self.assertRaises(TypeError, i.getValue, "one arg", "two arg", "three arg",
                "methods defined solely on Implementation shouldn't be visible")
        self.assertFalse(hasattr(i, "internalMethod"),
                "methods from private interfaces shouldn't be visible on a private class")

    def test_super_methods_visible(self):
        '''Bug #222847 - Can't access public member of package private base class'''
        self.assertEquals("hi", HiddenSuper().hi())

    def test_nested_classes(self):
        """Test deeply nested classes

        Bug #440660 - using nested java cls @ level >2 fails"""

        Matryoshka.Outermost.Middle.Innermost

    def test_inner_class_identity(self):
        """Bug #452947 - Class of innerclass inst <> innerclass"""
        self.assertEquals(id(Matryoshka.Outermost), id(Matryoshka.makeOutermost().__class__))

    def test_super_methods_merged(self):
        '''Checks that all signatures on a class' methods are found, not just the first for a name

        Bug #628315'''
        synchList = Collections.synchronizedList(ArrayList())
        synchList.add("a string")
        self.assertEquals("a string", synchList.remove(0))

    def test_interface_methods_merged(self):
        '''Checks that declaring an interface redundantly doesn't hide merged methods.

        Bug #1381'''
        for impl in Implementation, ExtraString, ExtraClass, ExtraStringAndClass, ExtraClassAndString:
            instance = impl()
            self.assertEquals("String", instance.call("string argument"))
            self.assertEquals("int", instance.call(7))
            self.assertEquals("Class", instance.call(LinkedList))

    def test_extending_multiple_hidden_classes(self):
        '''Tests multiple levels of non-public classes overriding public methods from superclasses

        Bug #1430'''
        conn = BaseConnection.newConnection()
        self.assertEquals("wrapper close", conn.close())
        self.assertEquals("special close", conn.close(7))


class JavaClassTest(unittest.TestCase):
    def test_class_methods_visible(self):
        self.assertFalse(HashMap.isInterface(),
                'java.lang.Class methods should be visible on Class instances')
        self.assertFalse(HashMap.interface,
                'java.lang.Class bean methods should be visible on instances')
        self.assertEquals(3, len(HashMap.getInterfaces()))

    def test_python_fields(self):
        self.assertEquals('java.util', HashMap.__module__)
        self.assertEquals(Class, HashMap.__class__)
        self.assertEquals(None, HashMap.__doc__)
        self.assertEquals(list(HashMap.__mro__), HashMap.mro())

    def test_python_methods(self):
        s = SomePyMethods()
        self.assertEquals(6, s[3])
        self.assertEquals(2, s.a, "Undefined attributes should go through to __getattr__")
        self.assertEquals(3, s.b, "Defined fields should take precedence")

class CoercionTest(unittest.TestCase):
    def test_int_coercion(self):
        c = Coercions()
        self.assertEquals("5", c.takeInt(5))
        self.assertEquals("15", c.takeInteger(15))
        self.assertEquals("150", c.takeNumber(150))
        self.assertEquals("take with byte arg: 10", Coercions.take(Byte(10)))

    def test_array_coercion(self):
        self.assertEquals("double", Coercions.takeArray(array.zeros('d', 2)))
        self.assertEquals("float", Coercions.takeArray(array.zeros('f', 2)))
        self.assertEquals("4", Coercions.takePyObj(1, 2, 3, 4))
        c = Coercions()
        self.assertEquals("5", c.takePyObjInst(1, 2, 3, 4, 5))
        self.assertEquals("OtherSubVisible[]", c.takeArray([OtherSubVisible()]))
        self.assertEquals("SubVisible[]", c.takeArray([SubVisible()]))

    def test_iterable_coercion(self):
        def simple_gen():
            yield 1
            yield 2
            yield 3
        self.assertEquals(6, Coercions.takeIterable(simple_gen()))

    def test_class_coercion(self):
        c = Coercions()
        from java.util import Hashtable, HashMap
        ht = Hashtable()
        hm = HashMap()
        ht['one'] = 'uno'
        hm['zwei'] = 'two'
        for obj, cls in ((ht, "java.util.Hashtable"), (hm, "java.util.HashMap"), ("abc", "java.lang.String"),
                (1, "java.lang.Integer"), (1.2, "java.lang.Double"), (Hashtable, "java.lang.Class")):
            self.assertEquals(c.tellClassNameSerializable(obj), "class " + cls)
        self.assertEquals(c.tellClassNameObject(ht), "class java.util.Hashtable")

class RespectJavaAccessibilityTest(unittest.TestCase):
    def run_accessibility_script(self, script, error=AttributeError):
        fn = test_support.findfile(script)
        self.assertRaises(error, execfile, fn)
        self.assertEquals(subprocess.call([sys.executable, "-J-Dpython.cachedir.skip=true",
            "-J-Dpython.security.respectJavaAccessibility=false", fn]),
            0)

    def test_method_access(self):
        self.run_accessibility_script("call_protected_method.py")

    def test_field_access(self):
        self.run_accessibility_script("access_protected_field.py")

    def test_protected_class(self):
        self.run_accessibility_script("access_protected_class.py", TypeError)

    def test_overriding(self):
        self.run_accessibility_script("call_overridden_method.py")

class ClassloaderTest(unittest.TestCase):
    def test_loading_classes_without_import(self):
        cl = test_support.make_jar_classloader("../callbacker_test.jar")
        X = cl.loadClass("org.python.tests.Callbacker")
        called = []
        class Blah(X.Callback):
            def call(self, arg=None):
                called.append(arg)
        X().callNoArg(Blah())
        self.assertEquals(None, called[0])

def test_main():
    test_support.run_unittest(VisibilityTest,
            JavaClassTest,
            CoercionTest,
            RespectJavaAccessibilityTest,
            ClassloaderTest)

if __name__ == "__main__":
    test_main()
