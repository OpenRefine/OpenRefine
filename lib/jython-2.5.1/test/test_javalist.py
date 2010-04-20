from javatests import ListTest

class PyListTest(ListTest):

    def __init__(self):
        ListTest.__init__(self)

    def newInstance(self, coll):
        if coll is None:
            return list()
        else:
            return list(coll)

    def isReadOnly(self):
        return False


class PyTupleTest(ListTest):

    def __init__(self):
        ListTest.__init__(self)

    def newInstance(self, coll):
        if coll is None:
            return tuple()
        else:
            return tuple(coll)

    def isReadOnly(self):
        return True


# these first two tests just verify that we have a good unit test
print "ListTest.java driver (test_javalist.py)"
print "running test on ArrayList"
alt = ListTest.getArrayListTest(False)
alt.testAll()

print "running test on ArrayList (read-only)"
alt = ListTest.getArrayListTest(True)
alt.testAll()


# Now run the critical tests

print "running test on PyListTest"
plt = PyListTest()
plt.testAll()

print "running test on PyTupleTest"
ptt = PyTupleTest()
ptt.testAll()
