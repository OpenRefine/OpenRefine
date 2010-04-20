class NoOpMetaClass(type):
    "A no-op meta class, useful for testing the SF bug #1781500"
    def __new__(cls, name, bases, attrs):
        r = type.__new__(cls, name, bases, attrs)
        return r
