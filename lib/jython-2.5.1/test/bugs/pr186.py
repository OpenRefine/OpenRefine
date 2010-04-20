# boiled down test case for PR#186

a = []
b = []

ameth = a.index
bmeth = b.index

if ameth.__self__ is bmeth.__self__:
    print 'method selves are bogus!'
