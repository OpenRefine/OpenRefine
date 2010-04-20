#For Jython, removed co_names, co_consts since these are implementation details
# that may never get implemented, and removed flags since there are implementation
# differences that may never line up.  Still failing on one test for varnames
# because I think it is possible that the order of varnames might be useful (in
# order declared) and Jython doesn't quite get that right.
"""This module includes tests of the code object representation.

>>> def f(x):
...     def g(y):
...         return x + y
...     return g
...

>>> dump(f.func_code)
name: f
argcount: 1
varnames: ('x', 'g')
cellvars: ('x',)
freevars: ()
nlocals: 2

>>> dump(f(4).func_code)
name: g
argcount: 1
varnames: ('y',)
cellvars: ()
freevars: ('x',)
nlocals: 1

>>> def h(x, y):
...     a = x + y
...     b = x - y
...     c = a * b
...     return c
...
>>> dump(h.func_code)
name: h
argcount: 2
varnames: ('x', 'y', 'a', 'b', 'c')
cellvars: ()
freevars: ()
nlocals: 5

>>> def attrs(obj):
...     print obj.attr1
...     print obj.attr2
...     print obj.attr3

>>> dump(attrs.func_code)
name: attrs
argcount: 1
varnames: ('obj',)
cellvars: ()
freevars: ()
nlocals: 1

>>> def optimize_away():
...     'doc string'
...     'not a docstring'
...     53
...     53L

>>> dump(optimize_away.func_code)
name: optimize_away
argcount: 0
varnames: ()
cellvars: ()
freevars: ()
nlocals: 0

"""

def consts(t):
    """Yield a doctest-safe sequence of object reprs."""
    for elt in t:
        r = repr(elt)
        if r.startswith("<code object"):
            yield "<code object %s>" % elt.co_name
        else:
            yield r

def dump(co):
    """Print out a text representation of a code object."""
    for attr in ["name", "argcount", "varnames", "cellvars",
                 "freevars", "nlocals"]:
        print "%s: %s" % (attr, getattr(co, "co_" + attr))

def test_main(verbose=None):
    from test.test_support import run_doctest
    from test import test_code
    run_doctest(test_code, verbose)
