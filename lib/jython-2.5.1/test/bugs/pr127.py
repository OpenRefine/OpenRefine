class foo:
    def bar():
        return 'bar'

try:
    foo.bar()
except TypeError:
    pass
else:
    print 'unbound method must be called with class instance 1st argument'
