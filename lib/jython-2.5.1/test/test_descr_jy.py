"""Test descriptors, binary ops, etc.

Made for Jython.
"""
import types
import unittest
from test import test_support

class Old:
    pass


class New(object):
    pass


old = Old()
new = New()


class TestDescrTestCase(unittest.TestCase):

    def test_class_dict_is_copy(self):
        class FooMeta(type):
            def __new__(meta, name, bases, class_dict):
                cls = type.__new__(meta, name, bases, class_dict)
                self.assert_('foo' not in class_dict)
                cls.foo = 'bar'
                self.assert_('foo' not in class_dict)
                return cls

        class Foo(object):
            __metaclass__ = FooMeta

    def test_descr___get__(self):
        class Foo(object):
            __slots__ = 'bar'
            def hello(self):
                pass
            def hi(self):
                pass
            hi = staticmethod(hi)
        foo = Foo()
        foo.bar = 'baz'

        self.assertEqual(Foo.bar.__get__(foo), 'baz')
        self.assertEqual(Foo.bar.__get__(None, Foo), Foo.bar)

        bound = Foo.hello.__get__(foo)
        self.assert_(isinstance(bound, types.MethodType))
        self.assert_(bound.im_self is foo)
        self.assertEqual(Foo.hello.__get__(None, Foo), Foo.hello)

        bound = Foo.hi.__get__(foo)
        self.assert_(isinstance(bound, types.MethodType))
        self.assert_(bound.im_self is foo)
        unbound = Foo.hi.__get__(None, foo)
        self.assert_(isinstance(unbound, types.MethodType))
        self.assert_(unbound.im_self is None)

    def test_ints(self):
        class C(int):
            pass
        try:
            foo = int(None)
        except TypeError:
            pass
        else:
            self.assert_(False, "should have raised TypeError")
        try:
            foo = C(None)
        except TypeError:
            pass
        else:
            self.assert_(False, "should have raised TypeError")

    def test_raising_custom_attribute_error(self):
        class RaisesCustomMsg(object):
            def __get__(self, instance, type):
                raise AttributeError("Custom message")


        class CustomAttributeError(AttributeError): pass

        class RaisesCustomErr(object):
            def __get__(self, instance, type):
                raise CustomAttributeError

        class Foo(object):
            custom_msg = RaisesCustomMsg()
            custom_err = RaisesCustomErr()

        self.assertRaises(CustomAttributeError, lambda: Foo().custom_err)
        try:
            Foo().custom_msg
            self.assert_(False) # Previous line should raise AttributteError
        except AttributeError, e:
            self.assertEquals("Custom message", str(e))


class SubclassDescrTestCase(unittest.TestCase):

    def test_subclass_cmp_right_op(self):
        # Case 1: subclass of int

        class B(int):
            def __ge__(self, other):
                return "B.__ge__"
            def __le__(self, other):
                return "B.__le__"

        self.assertEqual(B(1) >= 1, "B.__ge__")
        self.assertEqual(1 >= B(1), "B.__le__")

        # Case 2: subclass of object

        class C(object):
            def __ge__(self, other):
                return "C.__ge__"
            def __le__(self, other):
                return "C.__le__"

        self.assertEqual(C() >= 1, "C.__ge__")
        self.assertEqual(1 >= C(), "C.__le__")

        # Case 3: subclass of new-style class; here it gets interesting

        class D(C):
            def __ge__(self, other):
                return "D.__ge__"
            def __le__(self, other):
                return "D.__le__"

        self.assertEqual(D() >= C(), "D.__ge__")
        self.assertEqual(C() >= D(), "D.__le__")

        # Case 4: comparison is different than other binops

        class E(C):
            pass

        self.assertEqual(E.__le__, C.__le__)

        self.assertEqual(E() >= 1, "C.__ge__")
        self.assertEqual(1 >= E(), "C.__le__")
        self.assertEqual(E() >= C(), "C.__ge__")
        self.assertEqual(C() >= E(), "C.__le__") # different

    def test_subclass_binop(self):
        def raises(exc, expected, callable, *args):
            try:
                callable(*args)
            except exc, msg:
                if str(msg) != expected:
                    self.assert_(False, "Message %r, expected %r" % (str(msg),
                                                                     expected))
            else:
                self.assert_(False, "Expected %s" % exc)

        class B(object):
            pass

        class C(object):
            def __radd__(self, o):
                return '%r + C()' % (o,)

            def __rmul__(self, o):
                return '%r * C()' % (o,)

        # Test strs, unicode, lists and tuples
        mapping = []

        # + binop
        mapping.append((lambda o: 'foo' + o,
                        TypeError, "cannot concatenate 'str' and 'B' objects",
                        "'foo' + C()"))
        # XXX: There's probably work to be done here besides just emulating this
        # message
        if test_support.is_jython:
            mapping.append((lambda o: u'foo' + o,
                            TypeError, "cannot concatenate 'unicode' and 'B' objects",
                            "u'foo' + C()"))
        else:
            mapping.append((lambda o: u'foo' + o,
                            TypeError,
                            'coercing to Unicode: need string or buffer, B found',
                            "u'foo' + C()"))
        mapping.append((lambda o: [1, 2] + o,
                        TypeError, 'can only concatenate list (not "B") to list',
                        '[1, 2] + C()'))
        mapping.append((lambda o: ('foo', 'bar') + o,
                        TypeError, 'can only concatenate tuple (not "B") to tuple',
                        "('foo', 'bar') + C()"))

        # * binop
        mapping.append((lambda o: 'foo' * o,
                        TypeError, "can't multiply sequence by non-int of type 'B'",
                        "'foo' * C()"))
        mapping.append((lambda o: u'foo' * o,
                        TypeError, "can't multiply sequence by non-int of type 'B'",
                        "u'foo' * C()"))
        mapping.append((lambda o: [1, 2] * o,
                        TypeError, "can't multiply sequence by non-int of type 'B'",
                        '[1, 2] * C()'))
        mapping.append((lambda o: ('foo', 'bar') * o,
                        TypeError, "can't multiply sequence by non-int of type 'B'",
                        "('foo', 'bar') * C()"))

        for func, bexc, bexc_msg, cresult in mapping:
            raises(bexc, bexc_msg, lambda : func(B()))
            self.assertEqual(func(C()), cresult)

    def test_overriding_base_binop(self):
        class MulBase(object):
            def __init__(self, value):
                self.value = value
            def __mul__(self, other):
                return self.value * other.value
            def __rmul__(self, other):
                return other.value * self.value
        class DoublerBase(MulBase):
            def __mul__(self, other):
                return 2 * (self.value * other.value)
        class AnotherDoubler(DoublerBase):
            pass
        self.assertEquals(DoublerBase(2) * AnotherDoubler(3), 12)

    def test_oldstyle_binop_notimplemented(self):
        class Foo:
            pass
        class Bar(object):
            def __radd__(self, other):
                return 3
        self.assertEqual(Foo() + Bar(), 3)

    def test_int_mul(self):
        # http://bugs.jython.org/issue1332
        class Foo(tuple):
            def __rmul__(self, other):
                return 'foo'
        foo = Foo()
        self.assertEqual(3.0 * foo, 'foo')
        self.assertEqual(4 * foo, 'foo')


class InPlaceTestCase(unittest.TestCase):

    def test_iadd(self):
        class Foo(object):
            def __add__(self, other):
                return 1
            def __radd__(self, other):
                return 2
        class Bar(object):
            pass
        class Baz(object):
            def __iadd__(self, other):
                return NotImplemented
        foo = Foo()
        foo += Bar()
        self.assertEqual(foo, 1)
        bar = Bar()
        bar += Foo()
        self.assertEqual(bar, 2)
        baz = Baz()
        baz += Foo()
        self.assertEqual(baz, 2)

    def test_imul(self):
        class FooInplace(list):
            def __imul__(self, other):
                return [1]
        class Bar(FooInplace):
            def __mul__(self, other):
                return [2]
        foo = FooInplace()
        foo *= 3
        self.assertEqual(foo, [1])
        foo = Bar([3])
        foo *= 3
        self.assertEqual(foo, [1])

        class Baz(FooInplace):
            def __mul__(self, other):
                return [3]
        baz = Baz()
        baz *= 3
        self.assertEqual(baz, [1])

    def test_list(self):
        class Foo(list):
            def __mul__(self, other):
                return [1]
        foo = Foo([2])
        foo *= 3
        if test_support.is_jython:
            self.assertEqual(foo, [2, 2, 2])
        else:
            # CPython ignores list.__imul__ on a subclass with __mul__
            # (unlike Jython and PyPy)
            self.assertEqual(foo, [1])

        class Bar(object):
            def __radd__(self, other):
                return 1
            def __rmul__(self, other):
                return 2
        l = []
        l += Bar()
        self.assertEqual(l, 1)
        l = []
        l *= Bar()
        self.assertEqual(l, 2)

    def test_iand(self):
        # Jython's set __iand__ (as well as isub, ixor, etc) was
        # previously broken
        class Foo(set):
            def __and__(self, other):
                return set([1])
        foo = Foo()
        foo &= 3
        self.assertEqual(foo, set([1]))


class DescrExceptionsTestCase(unittest.TestCase):

    def test_hex(self):
        self._test(hex)

    def test_oct(self):
        self._test(oct)

    def test_other(self):
        for op in '-', '+', '~':
            try:
                eval('%s(old)' % op)
            except AttributeError:
                pass
            else:
                self._assert(False, 'Expected an AttributeError, op: %s' % op)
            try:
                eval('%s(new)' % op)
            except TypeError:
                pass
            else:
                self._assert(False, 'Expected a TypeError, op: %s' % op)

    def _test(self, func):
        self.assertRaises(AttributeError, func, old)
        self.assertRaises(TypeError, func, new)

    def test_eq(self):
        class A(object):
            def __eq__(self, other):
                return self.value == other.value
        self.assertRaises(AttributeError, lambda: A() == A())


class GetAttrTestCase(unittest.TestCase):
    def test_raising_custom_attribute_error(self):
        # Very similar to
        # test_descr_jy.TestDescrTestCase.test_raising_custom_attribute_error
        class BarAttributeError(AttributeError): pass

        class Bar(object):
            def __getattr__(self, name):
                raise BarAttributeError

        class BarClassic:
            def __getattr__(self, name):
                raise BarAttributeError

        class Foo(object):
            def __getattr__(self, name):
                raise AttributeError("Custom message")

        class FooClassic:
            def __getattr__(self, name):
                raise AttributeError("Custom message")

        self.assertRaises(BarAttributeError, lambda: Bar().x)
        self.assertRaises(BarAttributeError, lambda: BarClassic().x)

        try:
            Foo().x
            self.assert_(False) # Previous line should raise AttributteError
        except AttributeError, e:
            self.assertEquals("Custom message", str(e))

        try:
            FooClassic().x
            self.assert_(False) # Previous line should raise AttributteError
        except AttributeError, e:
            self.assertEquals("Custom message", str(e))


class Base(object):
    def __init__(self, name):
        self.name = name


def lookup_where(obj, name):
    mro = type(obj).__mro__
    for t in mro:
        if name in t.__dict__:
            return t.__dict__[name], t
    return None, None


def refop(x, y, opname, ropname):
    # this has been validated by running the tests on top of cpython
    # so for the space of possibilities that the tests touch it is known
    # to behave like cpython as long as the latter doesn't change its own
    # algorithm
    t1 = type(x)
    t2 = type(y)
    op, where1 = lookup_where(x, opname)
    rop, where2 = lookup_where(y, ropname)
    if op is None and rop is not None:
        return rop(y, x)
    if rop and where1 is not where2:
        if (issubclass(t2, t1) and not issubclass(where1, where2) and
            not issubclass(t1, where2)):
            return rop(y, x)
    if op is None:
        return "TypeError"
    return op(x,y)


def do_test(X, Y, name, impl):
    x = X('x')
    y = Y('y')
    opname = '__%s__' % name
    ropname = '__r%s__' % name

    count = [0]
    fail = []

    def check(z1, z2):
        ref = refop(z1, z2, opname, ropname)
        try:
            v = impl(z1, z2)
        except TypeError:
            v = "TypeError"
        if v != ref:
            fail.append(count[0])

    def override_in_hier(n=6):
        if n == 0:
            count[0] += 1
            check(x, y)
            check(y, x)
            return

        f = lambda self, other: (n, self.name, other.name)
        if n % 2 == 0:
            name = opname
        else:
            name = ropname

        for C in Y.__mro__:
            if name in C.__dict__:
                continue
            if C is not object:
                setattr(C, name, f)
            override_in_hier(n - 1)
            if C is not object:
                delattr(C, name)

    override_in_hier()
    #print count[0]
    return fail


class BinopCombinationsTestCase(unittest.TestCase):

    """Try to test more exhaustively binop overriding combination
    cases"""

    def test_binop_combinations_mul(self):
        class X(Base):
            pass
        class Y(X):
            pass

        fail = do_test(X, Y, 'mul', lambda x, y: x*y)
        #print len(fail)
        self.assert_(not fail)

    def test_binop_combinations_sub(self):
        class X(Base):
            pass
        class Y(X):
            pass

        fail = do_test(X, Y, 'sub', lambda x, y: x-y)
        #print len(fail)
        self.assert_(not fail)

    def test_binop_combinations_pow(self):
        class X(Base):
            pass
        class Y(X):
            pass

        fail = do_test(X, Y, 'pow', lambda x, y: x**y)
        #print len(fail)
        self.assert_(not fail)

    def test_binop_combinations_more_exhaustive(self):
        class X(Base):
            pass

        class B1(object):
            pass

        class B2(object):
            pass

        class X1(B1, X, B2):
            pass

        class C1(object):
            pass

        class C2(object):
            pass

        class Y(C1, X1, C2):
            pass

        fail = do_test(X, Y, 'sub', lambda x, y: x - y)
        #print len(fail)
        self.assert_(not fail)


def test_main():
    test_support.run_unittest(TestDescrTestCase,
                              SubclassDescrTestCase,
                              InPlaceTestCase,
                              DescrExceptionsTestCase,
                              GetAttrTestCase,
                              BinopCombinationsTestCase)


if __name__ == '__main__':
    test_main()
