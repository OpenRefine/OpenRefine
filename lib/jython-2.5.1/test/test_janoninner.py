# test of PR#201

import sys
from test_support import *

print 'Java Anonymous Inner Classes (test_janoninner.py)'

print 'importing'
import javatests.AnonInner

print 'instantiating'
x = javatests.AnonInner()

print 'invoking'
assert x.doit() == 2000
