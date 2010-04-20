"""Jython bug with cell variables and yield"""
from __future__ import generators

def single_closure_single_value():
    value = 0
    a_closure = lambda : value
    yield a_closure()
    yield a_closure()

def single_closure_multiple_values():
    value = 0
    a_closure = lambda : value
    yield a_closure()
    value = 1
    yield a_closure()

def multiple_closures_single_value():
    value = 0
    a_closure = lambda : value
    yield a_closure()
    a_closure = lambda : value
    yield a_closure()

def multiple_closures_multiple_values():
    value = 0
    a_closure = lambda : value
    yield a_closure()
    value = 1
    a_closure = lambda : value
    yield a_closure()

tests={}
for name in dir():
    if 'closure' in name:
        test = eval(name)
        if name.endswith('single_value'):
            expected = [0,0]
        else:
            expected = [0,1]
        tests[test] = expected

def test_main(verbose=None):
    from test.test_support import verify
    import sys
    for func in tests:
        expected = tests[func]
        result = list(func())
        verify(result == expected, "%s: expected %s, got %s" % (
                func.__name__, expected, result))

if __name__ == '__main__': test_main(1)
