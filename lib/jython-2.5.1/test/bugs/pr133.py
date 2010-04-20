import pr133.test

name = pr133.test.__name__
reload(pr133.test)

if name <> pr133.test.__name__:
    print 'Name changed after reload'

reload(pr133.test)

if name <> pr133.test.__name__:
    print 'Name changed after reload'
