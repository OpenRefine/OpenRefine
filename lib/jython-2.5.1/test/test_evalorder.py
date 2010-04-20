from unittest import TestCase
from test import test_support

class Bucket(object):
    def __init__(self, value):
        self.__value = value
    def _get(self):
        return self.__value
    def _set(self, value):
        assert self.__value == value, "Value changed!"
    value = property(_get,_set)

class PropBucket(object):
    def __init__(self):
        self.__dict__['_d'] = {}
    def __getattr__(self, attr):
        value = self._d.setdefault(attr, 0)
        self._d[attr] = value + 1
        return Bucket(value)
    def __setattr__(self, attr, value):
        value.append(attr)

class EvaluationOrder(TestCase):
    def test_TestFunctionality(self):
        bucket = PropBucket()
        try:
            bucket.prop.value = bucket.prop.value + 0
        except AssertionError:
            pass
        else:
            assert False, "PropBucket is not working"
    def test_augassign(self):
        bucket = PropBucket()
        bucket.prop.value += 0
    def test_AssignOrder(self):
        bucket = PropBucket()
        expected = ['one','two','three']
        result = []
        bucket.one = bucket.two = bucket.three = result
        assert result == expected, "expected %s, got %s" % (expected, result)
    def test_operands(self):
        m = [(2,), (1,)].pop
        assert m() + m() == (1,2), "faulty operand order"
    def test_arguments(self):
        def one(a,b,c,d,*extra):
            return reduce(lambda r,x: r+x,extra,a+b+c+d)
        m = list((x,) for x in xrange(100,0,-1)).pop
        value = one(m(),m(),m(),m())
        assert value == (1,2,3,4), "simple call, got: %s " % (value,)
        value = one(m(),m(),d=m(),c=m())
        assert value == (5,6,8,7), "call with keywords, got: %s" % (value,)
        value = one(m(),m(),m(),m(),m(),m())
        assert value == (9,10,11,12,13,14), "long call, got: %s" % (value,)
        value = one(m(),m(),*[m(),m(),m(),m()])
        assert value == (15,16,17,18,19,20), "varcalls, got: %s" % (value,)
        value = one(m(),m(),**dict(c=m(),d=m()))
        assert value == (21,22,23,24), "varkeywordcall, got: %s" % (value,)
        value = one(*[m(),m()],**dict(c=m(),d=m()))
        assert value == (25,26,27,28), "bothvarcalls, got: %s" % (value,)

def test_main():
    test_support.run_unittest(EvaluationOrder)

if __name__ == '__main__':
    test_main()
