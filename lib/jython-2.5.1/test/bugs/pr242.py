# PR#242 re.VERBOSE flag not implemented

import re

cre = re.compile('''# a verbose regular expression
\d+
# this line is also ignored
hello''', re.VERBOSE)

# test it
if not cre.match('0123hello'):
    print 're.VERBOSE did not work'
