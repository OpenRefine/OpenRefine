import unittest
from test import test_support

class ListCompTestCase(unittest.TestCase):

    #http://bugs.jython.org/issue1205
    def test_long_listcomp(self):
        #for a long list comp, we compute the Hardy-Ramanujan number
        #http://en.wikipedia.org/wiki/1729_(number)
        res = [(x1**3+x2**3,(x1,x2),(y1,y2))
              for x1 in range(20) for x2 in range(20) if x1 < x2 # x-Paare
              for y1 in range(20) for y2 in range(20) if y1 < y2 # y-Paare
              if x1**3+x2**3 == y1**3+y2**3 # gleiche Summe
              if (x1,x2) < (y1,y2)
              ]
        self.assertEquals(1729, min(res)[0])
        self.assertEquals(len(res), 2)

def test_main():
    test_support.run_unittest(ListCompTestCase)

if __name__ == '__main__':
    test_main()
