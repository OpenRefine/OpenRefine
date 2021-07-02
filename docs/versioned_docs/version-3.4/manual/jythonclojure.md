---
id: jythonclojure
title: Jython & Clojure
sidebar_label: Jython & Clojure
---

## Jython {#jython}

Jython 2.7.2 comes bundled with the default installation of OpenRefine 3.4.1. You can add libraries and code by following [this tutorial](https://github.com/OpenRefine/OpenRefine/wiki/Extending-Jython-with-pypi-modules). A large number of Python files (`.py` or `.pyc`) are compatible. 

Python code that depends on C bindings will not work in OpenRefine, which uses Java / Jython only. Since Jython is essentially Java, you can also import Java libraries and utilize those. 

You will need to restart OpenRefine, so that new Jython or Python libraries are initialized during startup.

OpenRefine now has [most of the Jsoup.org library built into GREL functions](grelfunctions#jsoup-xml-and-html-parsing-functions) for parsing and working with HTML and XML elements.

### Syntax {#syntax}

Expressions in Jython must have a `return` statement:

```
  return value[1:-1]
```

```
  return rowIndex%2
```

Fields have to be accessed using the bracket operator rather than dot notation:

```
  return cells["col1"]["value"]
```

For example, to access the [edit distance](reconciling#reconciliation-facets) between a reconciled value and an original cell value using [recon variables](#reconciliation):

```
  return cell["recon"]["features"]["nameLevenshtein"]
```

To return the lower case of `value` (if the value is not null):

```
  if value is not None:
    return value.lower()
  else:
    return None
```

### Tutorials {#tutorials}
- [Extending Jython with pypi modules](https://github.com/OpenRefine/OpenRefine/wiki/Extending-Jython-with-pypi-modules)
- [Working with phone numbers using Java libraries inside Python](https://github.com/OpenRefine/OpenRefine/wiki/Jython#tutorial---working-with-phone-numbers-using-java-libraries-inside-python)

Full documentation on the Jython language can be found on its official site: [http://www.jython.org](http://www.jython.org).

## Clojure {#clojure}

Clojure 1.10.1 comes bundled with the default installation of OpenRefine 3.4.1. At this time, not all [variables](expressions#variables) can be used with Clojure expressions: only `value`, `row`, `rowIndex`, `cell`, and `cells` are available.

For example, functions can take the form 
```
(.. value (toUpperCase) )
```

Or can look like 
```
(-> value (str/split #" ") last )
```

which functions like `value.split(" ")` in GREL.

For help with syntax, see the [Clojure website's guide to syntax](https://clojure.org/guides/learn/syntax).

User-contributed Clojure recipes can be found on our wiki at [https://github.com/OpenRefine/OpenRefine/wiki/Recipes#11-clojure](https://github.com/OpenRefine/OpenRefine/wiki/Recipes#11-clojure).

Full documentation on the Clojure language can be found on its official site: [https://clojure.org/](https://clojure.org/).