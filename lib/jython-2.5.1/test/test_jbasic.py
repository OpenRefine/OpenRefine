from test_support import *

print 'Basic Java Integration (test_jbasic.py)'

print 'type conversions'
print 'numbers'

from java.lang.Math import abs
assert abs(-2.) == 2., 'Python float to Java double'
assert abs(-2) == 2l, 'Python int to Java long'
assert abs(-2l) == 2l, 'Python long to Java long'

try: abs(-123456789123456789123l)
except TypeError: pass

print 'strings'
from java.lang import Integer, String

assert Integer.valueOf('42') == 42, 'Python string to Java string'

print 'arrays'
chars = ['a', 'b', 'c']
assert String.valueOf(chars) == 'abc', 'char array'

print 'Enumerations'
from java.util import Vector

vec = Vector()
items = range(10)
for i in items:
    vec.addElement(i)

expected = 0
for i in vec:
    assert i == expected, 'testing __iter__ on java.util.Vector'
    expected = expected+1

expected = 0
for i in iter(vec):
    assert i == expected, 'testing iter(java.util.Vector)'
    expected = expected+1


print 'create java objects'

from java.math import BigInteger

assert BigInteger('1234', 10).intValue() == 1234, 'BigInteger(string)'
assert BigInteger([0x11, 0x11, 0x11]).intValue() == 0x111111, 'BigInteger(byte[])'
assert BigInteger(-1, [0x11, 0x11, 0x11]).intValue() == -0x111111, 'BigInteger(int, byte[])'

print 'call static methods'
s1 = String.valueOf(['1', '2', '3'])
s2 = String.valueOf('123')
s3 = String.valueOf(123)
s4 = String.valueOf(123l)
s5 = String.valueOf(['0', '1', '2', '3', 'a', 'b'], 1, 3)
assert s1 == s2 == s3 == s4 == s5, 'String.valueOf method with different arguments'

print 'call instance methods'
s = String('hello')
assert s.regionMatches(1, 1, 'ell', 0, 3), 'method call with boolean true'
assert s.regionMatches(0, 1, 'ell', 0, 3), 'method call with boolean false'
assert s.regionMatches(1, 'ell', 0, 3), 'method call no boolean'

assert s.regionMatches(1, 1, 'eLl', 0, 3), 'method call ignore case'
assert not s.regionMatches(1, 'eLl', 0, 3), 'should ignore case'

from java.awt import Dimension

print 'get/set fields'
d = Dimension(3,9)
assert d.width == 3 and d.height == 9, 'getting fields'
d.width = 42
assert d.width == 42 and d.height == 9, 'setting fields'

#Make sure non-existent fields fail
try:
    print d.foo
except AttributeError:
    pass
else:
    raise AssertionError, 'd.foo should throw type error'

print 'get/set bean properties'

from javax import swing
b1 = swing.JButton()
b1.label = 'foo'
b2 = swing.JButton(label='foo')
assert b1.label == b2.label == 'foo', 'Button label bean property'

print 'bean event properties'
# Test bean event properties - single and multiple
flag = 0
def testAction(event):
    global flag
    flag = flag + 1

from java.awt.event import ActionEvent

doit = ActionEvent(b1, ActionEvent.ACTION_PERFORMED, "")

b1.actionPerformed = testAction
flag = 0
b1.doClick()
assert flag == 1, 'expected one action per event but got %s' % flag

b1.actionPerformed.append(testAction)
flag = 0
b1.doClick()
assert flag == 2, 'two actions per event'

b1.actionPerformed = testAction
flag = 0
b1.doClick()
assert flag == 1, 'one actions per event - again'

# TBD: Jython does not properly exit after this code!
