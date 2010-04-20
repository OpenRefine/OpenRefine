d = {}
try:
    del d['nokey']
except KeyError:
    pass
else:
    print 'Deleting missing key should raise KeyError!'
