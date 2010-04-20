from org.python.tests.RespectJavaAccessibility import Banana, Pear

p = Pear()
b = Banana()
assert b.amethod() == 'Banana.amethod()'
assert p.amethod() == 'Banana.amethod()'
assert b.amethod(1,2) == 'Banana.amethod(x,y)'
assert p.amethod(1,2) == 'Pear.amethod(x,y)'
assert b.privBanana() == 'Banana.privBanana()'
assert p.privPear() == 'Pear.privPear()'
assert b.protMethod() == 'Banana.protMethod()'
assert p.protMethod() == 'Banana.protMethod()'
assert b.protMethod(1,2) == 'Banana.protMethod(x,y)'
assert p.protMethod(1,2) == 'Pear.protMethod(x,y)'
