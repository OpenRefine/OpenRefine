import sys
sys.path = [path for path in sys.path if not path.startswith('/')]
encoded = u'hi'.encode("utf-8")
encoded.decode('utf-8')
