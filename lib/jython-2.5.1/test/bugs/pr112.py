# test for PR#112 -- functions should not have __module__ attributes

def f():
    pass

if hasattr(f, '__module__'):
    print 'functions should not have __module__ attributes'

# but make sure classes still do have __module__ attributes
class F:
    pass

if not hasattr(F, '__module__'):
    print 'classes should still have __module__ attributes'
