# test for PR#233 -- more immutability of tuples (see also PR#174)

target = (1,2,3)
tpl = (1,2,3)
list(tpl).reverse()

if tpl <> target:
    print 'Tuples are mutable'
