# a test for PR#241, writeable sys.stdout.softspace

import sys
#print 'hello',
sys.stdout.softspace = 0
#print 'there', 'mister'

try:
    sys.stdout.jimmy = 1
except AttributeError:
    pass

try:
    sys.stdout.closed = 1
except TypeError:
    pass
