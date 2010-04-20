from java.io import Serializable
from java.lang import Object

class A:
    def __init__(self, a):
        self.a = a
    def __eq__(self, other):
        return self.__class__ == other.__class__ and self.__dict__ == other.__dict__
    def __ne__(self, other):
        return not (self == other)

class AJ(Object, Serializable):
    def __init__(self, a):
        self.a = a
    def __eq__(self, other):
        return self.__class__ == other.__class__ and self.__dict__ == other.__dict__
    def __ne__(self, other):
        return not (self == other)

class N(object):
    def __init__(self, a):
        self.a = a
    def __eq__(self, other):
        return self.__class__ == other.__class__ and self.__dict__ == other.__dict__
    def __ne__(self, other):
        return not (self == other)

class NL(list):
    def __init__(self, a, *x):
        list.__init__(self, x)
        self.a = a
    def __eq__(self, other):
        return (self.__class__ == other.__class__ and list.__eq__(self, other) and
                self.__dict__ == other.__dict__)
    def __ne__(self, other):
        return not (self == other)

class NT(tuple):
    def __new__(typ, a, *x):
        nt = tuple.__new__(typ, x)
        nt.a = a
        return nt
    def __eq__(self, other):
        return (self.__class__ == other.__class__ and tuple.__eq__(self, other) and
                self.__dict__ == other.__dict__)
    def __ne__(self, other):
        return not (self == other)
