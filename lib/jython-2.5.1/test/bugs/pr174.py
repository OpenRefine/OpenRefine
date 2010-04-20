# test for PR#174 -- immutability of tuples

target = (1,2,3)
lst = [1,2,3]
tpl = tuple(lst)
lst[0] = 0

if tpl <> target:
    print 'Tuples are mutable'
