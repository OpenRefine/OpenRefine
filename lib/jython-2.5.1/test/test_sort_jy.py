from test import test_support
import unittest

class SortTest(unittest.TestCase):

    def test_bug1835099(self):
        a = [21469, 0, 25093, 21992, 26488, 21392, 21998, 22387, 30011, 18382, 23114, 24329, 29505, 24637, 22922, 24258, 19705, 17497, 16693, 20602, 24780, 14618, 18200, 18468, 24491, 20448, 16797, 25276, 27262, 134009, 132609, 135000, 133027, 133957, 134209, 136300, 135505, 137629, 137364, 136698, 136705, 135020, 138258, 136820, 136502, 140408, 140861, 152317, 150993, 144857, 137562, 138705, 138811, 137456, 138393, 138521, 140876, 140271, 141384, 139595, 141839, 141237, 140742, 140514, 141127, 141411, 141501]
        a_set = set(a)
        a_sorted = sorted(a)
        a_sorted_set = set(a_sorted)

        if a_sorted_set != a_set:
            print 'list elements changed during sort:'
            print 'removed', tuple(a_set - a_sorted_set)
            print 'added', tuple(a_sorted_set - a_set)

        assert len(a_set - a_sorted_set) == len(a_sorted_set - a_set) == 0

def test_main():
    test_support.run_unittest(SortTest)

if __name__ == "__main__":
    test_main()
