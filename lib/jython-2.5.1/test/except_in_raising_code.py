def test():
    print noname

def foo():
    try:
        test()
    except ValueError:
        raise RuntimeError("Accessing a undefined name should raise a NameError")
