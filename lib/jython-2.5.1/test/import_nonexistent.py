try:
    import nonexistent_module
except ImportError:
    pass # This should cause an import error, but as there's a security manager in place it hasn't
         # always done so
else:
    raise Error("Should've caused an import error!")
