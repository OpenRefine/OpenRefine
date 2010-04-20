import sys
from test_support import TestSkipped

REPS = 65580

#XXX: Is there a way around the method length limit in Jython?
if sys.platform.startswith('java'):
    raise TestSkipped, 'The 65535 byte limit on methods in the JVM is a problem for jython.'

if sys.platform == 'mac':
    import gestalt
    if gestalt.gestalt('sysv') > 0x9ff:
        raise TestSkipped, 'Triggers pathological malloc slowdown on OSX MacPython'

l = eval("[" + "2," * REPS + "]")
print len(l)
