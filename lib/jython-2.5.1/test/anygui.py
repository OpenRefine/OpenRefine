'''Used by test_import_jy/test_getattr_module'''
import sys

class anygui:
    __all__ = ['Window'] # Etc...

    def __init__(self):
        self.__backend = None
        self.__backends = ['msw', 'x', 'mac', 'wx', 'tk', 'java']

    def __try_to_get(self, modulename):
        import imp
        try:
            module = imp.find_module(modulename)
        except (IOError, ImportError, AttributeError, AssertionError):
            return None
        else:
            return module

    def __import_backend(self, wishlist):
        candidates = self.__backends[:]
        for wish in wishlist:
            if wish in candidates:
                candidates.remove(wish)
            else:
                wishlist.remove(wish)
        candidates = wishlist + candidates

        for name in candidates:
            backend = self.__try_to_get('%sgui' % name)
        if not backend:
            raise Exception, 'not able to import any GUI backends'
        self.__backend = backend

    def __getattr__(self, name):
        if not self.__backend:
            self.__import_backend(self.__dict__.get('wishlist', []))
        return self.__backend.__dict__[name]

sys.modules[__name__] = anygui()
