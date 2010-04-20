d = {}
l = [1]
try:
    d[l] = 2
except TypeError:
    pass
else:
    print 'Should not be able to use a list as a dict key!'

a = {}
try:
    d[a] = 2
except TypeError:
    pass
else:
    print 'Should not be able to use a dict as a dict key!'
