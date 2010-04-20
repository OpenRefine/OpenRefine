# fixes PR#178 (sans the reload, which is only necessary w/o the patch
# outlined in PR#195).

from java.util import Date

class Foo(Date):
    pass

class Bar(Foo):
    pass
