"""Misc. class tests. These are more general class tests than CPython's
test_class which focuses on operators.

Made for Jython
"""
import __builtin__
import types
import unittest
from java.lang import Object
from test import test_support

class ClassGeneralTestCase(unittest.TestCase):

    TE_MSG = "can't set attributes of built-in/extension type 'str'"

    def test_dunder_module(self):
        self.assertEqual(str.__module__, '__builtin__')
        class Foo:
            pass
        Fu = types.ClassType('Fu', (), {})
        for cls in Foo, Fu:
            self.assert_('__module__' in cls.__dict__)
            self.assertEqual(cls.__module__, __name__)
            self.assertEqual(str(cls), '%s.%s' % (__name__, cls.__name__))
            self.assert_(repr(cls).startswith('<class %s.%s at' %
                                              (__name__, cls.__name__)))
            obj = cls()
            self.assert_(str(obj).startswith('<%s.%s instance at' %
                                             (__name__, cls.__name__)))

        class Bar(object):
            pass
        class Baz(Object):
            pass
        Bang = type('Bang', (), {})
        for cls in Bar, Baz, Bang:
            self.assert_('__module__' in cls.__dict__)
            self.assertEqual(cls.__module__, __name__)
            self.assertEqual(str(cls), "<class '%s.%s'>" % (__name__, cls.__name__))
            self.assertEqual(repr(cls), "<class '%s.%s'>" % (__name__, cls.__name__))
        self.assert_(str(Bar()).startswith('<%s.Bar object at' % __name__))
        self.assert_(str(Baz()).startswith("org.python.proxies.%s$Baz" % __name__))

    def test_builtin_attributes(self):
        for attr, val in dict(__name__='foo', __module__='bar', __dict__={},
                              __flags__=1, __base__=object,
                              __bases__=(unicode, object),
                              __mro__=(unicode, object)).iteritems():
            try:
                setattr(str, attr, val)
            except TypeError, te:
                self.assertEqual(str(te), self.TE_MSG)
            else:
                self.assert_(False,
                             'setattr str.%s expected a TypeError' % attr)
            try:
                delattr(str, attr)
            except TypeError, te:
                self.assertEqual(str(te), self.TE_MSG)
            else:
                self.assert_(False,
                             'delattr str.%s expected a TypeError' % attr)


    def test_attributes(self):
        class Foo(object):
            pass

        Foo.__name__ = 'Bar'
        self.assertEqual(Foo.__name__, 'Bar')
        try:
            del Foo.__name__
        except TypeError, te:
            self.assertEqual(str(te), "can't delete Bar.__name__")
        else:
            self.assert_(False, 'Expected a TypeError')

        Foo.__module__ = 'baz'
        self.assertEqual(Foo.__module__, 'baz')
        try:
            del Foo.__module__
        except TypeError, te:
            self.assertEqual(str(te), "can't delete Bar.__module__")
        else:
            self.assert_(False, 'Expected a TypeError')

        try:
            Foo.__dict__ = {}
        except AttributeError, ae:
            self.assertEqual(str(ae),
                             "attribute '__dict__' of 'type' objects is not "
                             "writable")
        else:
            self.assert_(False, 'Expected an AttributeError')
        try:
            del Foo.__dict__
        except AttributeError, ae:
            self.assertEqual(str(ae),
                             "attribute '__dict__' of 'type' objects is not "
                             "writable")
        else:
            self.assert_(False, 'Expected an AttributeError')

        for attr, val in dict(__flags__=1, __base__=object,
                              __bases__=(unicode, object),
                              __mro__=(unicode, object)).iteritems():
            try:
                setattr(str, attr, val)
            except TypeError, te:
                self.assertEqual(str(te), self.TE_MSG)
            else:
                self.assert_(False,
                             'setattr Foo.%s expected a TypeError' % attr)
            try:
                delattr(str, attr)
            except TypeError, te:
                self.assertEqual(str(te), self.TE_MSG)
            else:
                self.assert_(False,
                             'delattr Foo.%s expected a TypeError' % attr)

    def test_newstyle_new_classobj(self):
        # Ensure new.classobj can create new style classes
        class Foo(object):
            pass
        def hello(self):
            return 'hello'
        Bar = types.ClassType('Bar', (Foo,), dict(hello=hello))
        self.assert_(type(Bar), type)
        self.assert_(issubclass(Bar, Foo))
        self.assert_(hasattr(Bar, 'hello'))
        self.assertEquals(Bar().hello(), 'hello')

    def test_attribute_error_message(self):
        # Ensure that AttributeError matches the CPython message
        class Bar:
            pass
        try:
            Bar.bar
            self._assert(False) # The previous line should have raised
                                # AttributeError
        except AttributeError, e:
            self.assertEqual("class Bar has no attribute 'bar'", str(e))

        class Foo(object):
            pass
        try:
            Foo.bar
            self._assert(False) # The previous line should have raised
                                # AttributeError
        except AttributeError, e:
            self.assertEqual("type object 'Foo' has no attribute 'bar'",
                             str(e))

    def test_inner_class_dict(self):
        class z:
            class t:
                def moo(self):
                    pass
        # Printing this caused an NPE in Jython 2.1
        keys = list(z.t.__dict__)
        keys.sort()
        self.assertEqual(str(keys), "['__doc__', '__module__', 'moo']")

    def test_metaclass_and_slotted_base(self):
        class Meta(type):
            pass
        class SlottedBase(object):
            __slots__ = 'foo'
        # A regression up until 2.5a3: Defining Bar would cause a
        # TypeError "mro() returned base with unsuitable layout ('Bar')"
        class Bar(SlottedBase):
            __metaclass__ = Meta

    def test_slotted_diamond_problem_bug(self):
        class A(object):
            __slots__ = 'foo'
        class B(A):
            pass
        class C(A):
            pass
        # used to raise TypeError: multiple bases have instance lay-out
        # conflict
        class D(B, C):
            pass

    def test_getitem_exceptions(self):
        class A:
            def __getitem__(self, key):
                raise IndexError, "Fraid not"
        self.assertRaises(IndexError, A().__getitem__, 'b')


class ClassNamelessModuleTestCase(unittest.TestCase):

    def setUp(self):
        global __name__
        self.name = __name__
        del __name__

    def tearDown(self):
        global __name__
        __name__ = self.name

    def test_nameless_module(self):
        class Foo:
            pass
        self.assertEqual(Foo.__module__, '__builtin__')
        self.assertEqual(str(Foo), '__builtin__.Foo')
        self.assert_(repr(Foo).startswith('<class __builtin__.Foo at'))
        foo = Foo()
        self.assert_(str(foo).startswith('<__builtin__.Foo instance at'))

        class Bar(object):
            pass
        self.assertEqual(Bar.__module__, '__builtin__')
        self.assertEqual(str(Bar), "<class 'Bar'>")
        self.assertEqual(repr(Bar), "<class 'Bar'>")
        bar = Bar()
        self.assert_(str(bar).startswith('<Bar '))
        self.assert_(repr(bar).startswith('<Bar object at'))


class BrokenNameTestCase(unittest.TestCase):

    def setUp(self):
        global __name__
        self.name = __name__
        self.builtin_name = __builtin__.__name__
        del __name__
        del __builtin__.__name__

    def tearDown(self):
        global __name__
        __builtin__.__name__ = self.builtin_name
        __name__ = self.name

    def test_broken_name(self):
        try:
            class Foobar:
                pass
        except NameError:
            pass
        else:
            self.assert_(False, "Expected a NameError")


class ClassLocalsTestCase(unittest.TestCase):

    def test_class_locals(self):
        class Foo(object):
            pass

        class Bar(object):
            foo = Foo()
        self.assert_(not hasattr(Bar, 'Foo'))

        class Bar2(object):
            foo = Foo()
            locals()
        # Observer effect: Bar2 differs because we looked at
        # locals. This might be considered 'buggy' behavior; but it
        # matches CPython and Pypy. see below for an example
        self.assert_(hasattr(Bar2, 'Foo'))

    def test_class_locals_realworld(self):
        # A more real world test of the above situation, for reference
        class FieldGathererMeta(type):
            def __new__(meta, name, bases, class_dict):
                cls = type.__new__(meta, name, bases, class_dict)
                cls.fields = [field.upper() for field in class_dict.iterkeys() \
                                  if not field.startswith('_')]
                cls.fields.sort()
                return cls

        class SomeClass(object):
            pass

        class MyFields(object):
            __metaclass__ = FieldGathererMeta
            jython = 'foo'
            java = ('bar', SomeClass())
        # Technically SomeClass and FieldGathererMeta are actually
        # locals in the MyFields' class definition scope, but we expect
        # them to be omitted from its class_dict
        self.assertEqual(MyFields.fields, ['JAVA', 'JYTHON'])

        class MyFields2(object):
            __metaclass__ = FieldGathererMeta
            jython = 'foo'
            java = ('bar', SomeClass())
            locals()
        # Oops, locals() updates f_locals. Hilarity ensues
        self.assertEqual(MyFields2.fields, ['FIELDGATHERERMETA', 'JAVA',
                                            'JYTHON', 'SOMECLASS'])


class IsDescendentTestCase(unittest.TestCase):

    def test_newstyle_descendent_of_oldstyle(self):
        class NewStyle(object):
            pass
        class OldStyle:
            pass
        class Retro(NewStyle, OldStyle):
            pass
        self.assert_(issubclass(Retro, NewStyle))
        self.assert_(issubclass(Retro, OldStyle))
        retro = Retro()
        self.assert_(isinstance(retro, NewStyle))
        self.assert_(isinstance(retro, OldStyle))


class JavaClassNamingTestCase(unittest.TestCase):

    """Tests for PyJavaClass naming."""

    def test_java_class_name(self):
        # The __name__ and __module__ attributes of Java classes should
        # be set according to the same convention that Python uses.
        from java.lang import String
        self.assertEqual(String.__name__, "String")
        self.assertEqual(String.__module__, "java.lang")


module_name = __name__

class ClassDefinesDunderModule(unittest.TestCase):

    """Verifies http://bugs.jython.org/issue1022 is fixed"""

    def test_dundermodule_in_classdef(self):
        class Foo:
            self.assertEqual(__module__, module_name)
        class Bar(object):
            self.assertEqual(__module__, module_name)

    def test_dundermodule_in_class_dict_copy(self):
        class_dict = {'a': 'this is a', 'b': 'this is b'}
        Foo = type.__new__(type, 'Foo', (object,), class_dict)
        Foo.keys = class_dict.keys
        assert sorted(Foo().keys()) == sorted(['a', 'b']), sorted(Foo().keys())


class ClassMetaclassRepr(unittest.TestCase):

    def test_repr_with_metaclass(self):
        # http://bugs.jython.org/issue1131
        class FooMetaclass(type):
            def __new__(cls, name, bases, attrs):
                return super(FooMetaclass, cls).__new__(cls, name, bases, attrs)

        class Foo(object):
            __metaclass__ = FooMetaclass
        self.assertEqual("<class '%s.Foo'>" % __name__, repr(Foo))

    def test_metaclass_str(self):
        class Foo(type):
            def __repr__(cls):
                return 'foo'
        class Bar(object):
            __metaclass__ = Foo
        self.assertEqual(repr(Bar), 'foo')
        # type.__str__ previously broke this
        self.assertEqual(str(Bar), 'foo')


def test_main():
    test_support.run_unittest(
        ClassGeneralTestCase,
        ClassNamelessModuleTestCase,
        BrokenNameTestCase,
        ClassLocalsTestCase,
        IsDescendentTestCase,
        JavaClassNamingTestCase,
        ClassDefinesDunderModule,
        ClassMetaclassRepr)


if __name__ == "__main__":
    test_main()
