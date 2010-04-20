# PR#192, dir(func) and dir(method) returning []

def test1():
    'Test function 1'
    pass

def test2(a, b=2, c=3):
    pass

attrs = dir(test1)[:]
for attr in ['__doc__', '__name__', 'func_code', 'func_defaults',
             'func_doc', 'func_globals', 'func_name']:
    attrs.remove(attr)
assert not attrs

assert test1.__doc__ == test1.func_doc == 'Test function 1'
assert test1.__name__ == test1.func_name == 'test1'
assert test1.func_code
assert test1.func_defaults is None
assert test1.func_globals == globals()
assert test2.func_defaults == (2, 3)

co = test2.func_code
attrs = dir(co)[:]
for attr in ['co_name', 'co_argcount', 'co_varnames', 'co_filename',
             'co_firstlineno', 'co_flags']:
    attrs.remove(attr)
##assert not attrs

flags = 0x4 | 0x8

assert co.co_name == 'test2'
assert co.co_argcount == 3
assert co.co_varnames == ('a', 'b', 'c')
assert co.co_filename
assert co.co_firstlineno
assert (co.co_flags & flags) == 0

def test3(a, *args, **kw):
    pass

assert (test3.func_code.co_flags & flags) == flags

class Foo:
    def method(self):
        """This is a method"""
        pass

attrs = dir(Foo.method)[:]
for attr in ['im_self', 'im_func', 'im_class', '__doc__', '__name__']:
    attrs.remove(attr)
assert not attrs

assert Foo.method.im_self is None
assert Foo.method.im_class == Foo
assert Foo.method.im_func
assert Foo.method.im_func.__name__ == Foo.method.__name__
assert Foo.method.im_func.__doc__ == Foo.method.__doc__

f = Foo()
m = f.method
assert m.im_self == f
assert m.im_class == Foo
assert m.im_func == Foo.method.im_func
assert m.__name__ == Foo.method.__name__
assert m.__doc__ == Foo.method.__doc__

class Baz:
    pass

try:
    m.im_class = Baz
    assert 0
except TypeError:
    pass

try:
    m.im_stuff = 7
    assert 0
except AttributeError:
    pass
