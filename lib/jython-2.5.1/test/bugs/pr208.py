# PR#208, calling apply with bogus 3rd argument

def test(x):
    return x

assert 7 == apply(test, (7,))
assert 7 == apply(test, (), {'x': 7})

try:
    apply(test, (1,), 7)
    print 'TypeError expected'
except TypeError:
    pass

try:
    apply(test, (1,), {7:3})
    print 'TypeError expected'
except TypeError:
    pass

try:
    apply(test, (1,), None)
    print 'TypeError expected'
except TypeError:
    pass

