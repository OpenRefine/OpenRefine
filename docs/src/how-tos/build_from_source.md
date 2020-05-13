---
id: howto-build-from-source
title: How-To Build, Test and Run OpenRefine from source code
sidebar_label: How-To Build from source
---

 
You will need:
* [OpenRefine source code](https://github.com/OpenRefine/OpenRefine)
* [Java JDK](http://java.sun.com/javase/downloads/index.jsp)
* [Apache Maven](https://maven.apache.org)  (OPTIONAL)
* A Unix/Linux shell environment OR the Windows command line

From the top level directory in the OpenRefine application you can build, test and run OpenRefine using the `./refine` shell script (if you are working in a &ast;nix shell), or using the `refine.bat` script from the Windows command line. Note that the `refine.bat` on Windows only supports a subset of the functionality, supported by the `refine` shell script. The example commands below are using the `./refine` shell script, and you will need to use `refine.bat` if you are working from the Windows command line.

If you are working from the Windows command line you must also install a Java JDK, and [set the JAVA_HOME environment variable](http:confluence.atlassian.com/display/DOC/Setting+the+JAVA\_HOME+Variable+in+Windows) (please ensure it points to the JDK, and not the JRE)

### Maven (Optional)
OpenRefine's build script will download Maven for you and use it, if not found already locally installed.

If you will be using your Maven installation instead of OpenRefine's build script download installation, then set the `MVN_HOME` environment variable. You may need to reboot your machine after setting these environment variables. If you receive a message `Could not find the main class: com.google.refine.Refine. Program will exit.` it is likely `JAVA_HOME` is not set correctly.

Ensure that you set your `MAVEN_HOME` environment variable, for example:

```shell
MAVEN_HOME=E:\Downloads\apache-maven-3.5.4-bin\apache-maven-3.5.4\
```

NOTE: You can use Maven commands directly, but running some goals in isolation might fail (try adding the `compile test-compile` goals in your invocation if that is the case).

### Building

To see what functions are supported by OpenRefine's build system, type
```shell
./refine -h
```

To build the OpenRefine application from source type:
```shell
./refine clean
./refine build
```

### Testing
Since OpenRefine is composed of two parts, a server and a in-browser UI, the testing system reflects that:

* on the server side, it's powered by [TestNG](http://testng.org/) and the unit tests are written in Java;
* on the client side, we currently do not have any tests. (We used to have some, but they relied on Windmill, which has ceased to work a long time ago.)

To run all tests, use:
```shell
./refine test
```
**this option is not available when using refine.bat**


If you want to run only the server side portion of the tests, use:
```shell
./refine server_test
```

### Running
To run OpenRefine from the command line (assuming you have been able to build from the source code successfully)
```shell
./refine
```
By default, OpenRefine will use [refine.ini](https://github.com/OpenRefine/OpenRefine/blob/master/refine.ini) for configuration. You can copy it and rename it to `refine-dev.ini`, which will be used for configuration instead. `refine-dev.ini` won't be tracked by Git, so feel free to put your custom configurations into it.

### Building Distributions (Kits)

The Refine build system uses Apache Ant to automate the creation of the installation packages for the different operating systems. The packages are currently optimized to run on Mac OS X which is the only platform capable of creating the packages for all three OS that we support.

To build the distributions type

```shell
./refine dist <version>
```
where 'version' is the release version.

## Building, Testing and Running OpenRefine from Eclipse
OpenRefine' source comes with Maven configuration files which are recognized by [Eclipse](http://www.eclipse.org/) if the Eclipse Maven plugin (m2e) is installed.

At the command line, go to a directory **not** under your Eclipse workspace directory and check out the source:

```shell
git clone https://github.com/OpenRefine/OpenRefine.git
```

In Eclipse, invoke the `Import...` command and select `Existing Maven Projects`. 

![Import a Maven project](images/Eclipse/eclipse-1.png)

Choose the root directory of your clone of the repository. You get to choose which modules of the project will be imported. You can safely leave out the `packaging` module which is only used to generate the Linux, Windows and MacOS distributions.

![Select maven projects to import](images/Eclipse/eclipse-2.png)

To run and debug OpenRefine from Eclipse, you will need to add an execution configuration on the `server` sub-project.
Right click on the `server` subproject, click `Run as...` and `Run configurations...`. Just pick the root directory of the project and use `exec:java` as a Maven goal.

![Add a run configuration with the exec:java goal](images/Eclipse/eclipse-3.png)

This will add a run configuration that you can then use to run OpenRefine from Eclipse.

### Testing in Eclipse

You can run the server tests directly from Eclipse. To do that you need to have the TestNG launcher plugin installed, as well as the TestNG M2E plugin (for integration with Maven). If you don't have it, you can get it by [installing new software](https://help.eclipse.org/2020-03/index.jsp?topic=/org.eclipse.platform.doc.user/tasks/tasks-129.htm) from this update URL http://dl.bintray.com/testng-team/testng-eclipse-release/

Once the TestNG launching plugin is installed in your Eclipse, right click on the source folder ![](main/tests/server/src), select `Run As` -> `TestNG Test`. This should open a new tab with the TestNG launcher running the OpenRefine tests.

### Test coverage in Eclipse

It is possible to analyze test coverage in Eclipse with the `EclEmma Java Code Coverage` plugin. It will add a `Coverage as…` menu similar to the `Run as…` and `Debug as…` menus which will then display the covered and missed lines in the source editor.