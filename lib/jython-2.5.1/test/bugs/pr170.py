# This flexes PR#170 which is caused by a suspected JIT bug.  Said bug for
# some reason breaks exception matching.  There is now a workaround that seems
# to do the trick most of the time.
try:
    int("foo")
except ValueError:
    pass
