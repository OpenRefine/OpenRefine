# Python test set -- part 7, bound and unbound methods

from test_support import *

print 'Bound and unbound methods (test_methods.py)'

class A:
    def one(self): return 'one'

class B(A):
    def two(self): return 'two'

class C(A):
    def one(self): return 'another one'

a = A()
b = B()
c = C()

print 'unbound method equality'
assert A.one == B.one
assert A.one <> C.one

print 'method attributes'
assert A.one.im_func == a.one.im_func
assert a.one.im_self == a
assert a.one.im_class == A
assert b.one.im_self == b
assert b.one.im_class == B

print 'unbound method invocation w/ explicit self'
assert A.one(b) == 'one'
assert B.two(b) == 'two'
assert B.one(b) == 'one'

assert A.one(c) == 'one'
assert C.one(c) == 'another one'

assert A.one(a) == 'one'
try:
    B.one(a)
    assert 0
except TypeError:
    pass
try:
    C.one(a)
    assert 0
except TypeError:
    pass

print '"unbound" methods of builtin types'
w = [1,2,3].append
x = [4,5,6].append
assert w <> x
assert w.__self__ <> x.__self__

y = w.__self__[:]
z = x.__self__[:]

assert y.append.__self__ <> w
z.append(7)
assert z == (x.__self__+[7])
