from test_support import *

print 'Java Serialization (test_jser.py)'

from java import io, awt
import os, sys

object1 = 42
object2 = ['a', 1, 1.0]
class Foo:
    def bar(self):
        return 'bar'

object3 = Foo()
object3.baz     = 99

object4 = awt.Color(1,2,3)

print 'writing'

sername = os.path.join(sys.prefix, "test.ser")
fout = io.ObjectOutputStream(io.FileOutputStream(sername))
print 'Python int'
fout.writeObject(object1)
print 'Python list'
fout.writeObject(object2)
print 'Python instance'
fout.writeObject(object3)
print 'Java instance'
fout.writeObject(object4)
fout.close()

fin     = io.ObjectInputStream(io.FileInputStream(sername))
print 'reading'
iobject1 = fin.readObject()
iobject2 = fin.readObject()
iobject3 = fin.readObject()
iobject4 = fin.readObject()
fin.close()

#print iobject1, iobject2, iobject3, iobject3.__class__, iobject4

print 'Python int'
assert iobject1 == object1

print 'Python list'
assert iobject2 == object2

print 'Python instance'
assert iobject3.baz     == 99
assert iobject3.bar() == 'bar'
assert iobject3.__class__ == Foo

print 'Java instance'
assert iobject4 == object4

os.remove(sername)
