---
id: build-test-run
title: How to build, test and run
sidebar_label: How to build, test and run 
---

import useBaseUrl from '@docusaurus/useBaseUrl';


You will need:
* [OpenRefine source code](https://github.com/OpenRefine/OpenRefine)
* [Java JDK](http://java.sun.com/javase/downloads/index.jsp) (Get [OpenJDK from here](https://jdk.java.net/15/).)
* [Apache Maven](https://maven.apache.org)  (OPTIONAL)
* A Unix/Linux shell environment OR the Windows command line

From the top level directory in the OpenRefine application you can build, test and run OpenRefine using the `./refine` shell script (if you are working in a \*nix shell), or using the `refine.bat` script from the Windows command line. Note that the `refine.bat` on Windows only supports a subset of the functionality, supported by the `refine` shell script. The example commands below are using the `./refine` shell script, and you will need to use `refine.bat` if you are working from the Windows command line.

### Set up JDK {#set-up-jdk}

You must [install JDK](https://jdk.java.net/15/) and set the JAVA_HOME environment variable (please ensure it points to the JDK, and not the JRE).

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs
  groupId="operating-systems"
  defaultValue="win"
  values={[
    {label: 'Windows', value: 'win'},
    {label: 'Mac', value: 'mac'},
    {label: 'Linux', value: 'linux'}
  ]
}>

<TabItem value="win">

1. On Windows 10, click the Start Menu button, type `env`, and look at the search results. Click <span class="buttonLabels">Edit the system environment variables</span>. (If you are using an earlier version of Windows, use the “Search” or “Search programs and files” box in the Start Menu.)

![A screenshot of the search results for 'env'.](/img/env.png "A screenshot of the search results for 'env'.")

2. Click <span class="buttonLabels">Environment Variables…</span> at the bottom of the <span class="tabLabels">Advanced</span> window.
3. In the <span class="tabLabels">Environment Variables</span> window that appears, click <span class="buttonLabels">New…</span> and create a variable with the key `JAVA_HOME`. You can set the variable for only your user account, as in the screenshot below, or set it as a system variable - it will work either way.

![A screenshot of 'Environment Variables'.](/img/javahome.png "A screenshot of 'Environment Variables'.")

4. Set the `Value` to the folder where you installed JDK, in the format `D:\Programs\OpenJDK`. You can locate this folder with the <span class="buttonLabels">Browse directory...</span> button.

</TabItem>

<TabItem value="mac">

First, find where Java is on your computer with this command:

```
which java
```

Check the environment variable `JAVA_HOME` with:

```
$JAVA_HOME/bin/java --version
```

To set the environment variable for the current Java version of your MacOS:

```
export JAVA_HOME="$(/usr/libexec/java_home)"
```

Or, for Java 13.x:

```
export JAVA_HOME="$(/usr/libexec/java_home -v 13)"
```

</TabItem>

<TabItem value="linux">

##### With the terminal {#with-the-terminal}

Enter the following:

```
sudo apt install default-jre
```

This probably won’t install the latest JDK package available on the Java website, but it is faster and more straightforward. (At the time of writing, it installs OpenJDK 11.0.7.)

##### Manually {#manually}

First, [extract the JDK package](https://openjdk.java.net/install/) to the new directory `usr/lib/jvm`:

```
sudo mkdir -p /usr/lib/jvm
sudo tar -x -C /usr/lib/jvm -f /tmp/openjdk-14.0.1_linux-x64_bin.tar.gz
```

Then, navigate to this folder and confirm the final path (in this case, `usr/lib/jvm/jdk-14.0.1`). Open a terminal and type

```
sudo gedit /etc/profile
```

In the text window that opens, insert the following lines at the end of the `profile` file, using the path above:

```
JAVA_HOME=/usr/lib/jvm/jdk-14.0.1
PATH=$PATH:$HOME/bin:$JAVA_HOME/bin
export JAVA_HOME
export PATH
```

Save and close the file. When you are back in the terminal, type

```
source /etc/environment
```

Exit the terminal and restart your system. You can then check that `JAVA_HOME` is set properly by opening another terminal and typing
```
echo $JAVA_HOME
```

It should show the path you set above. 

</TabItem>

</Tabs>

---



### Maven (Optional) {#maven-optional}
OpenRefine's build script will download Maven for you and use it, if not found already locally installed.

If you will be using your Maven installation instead of OpenRefine's build script download installation, then set the `MVN_HOME` environment variable. You may need to reboot your machine after setting these environment variables. If you receive a message `Could not find the main class: com.google.refine.Refine. Program will exit.` it is likely `JAVA_HOME` is not set correctly.

Ensure that you set your `MAVEN_HOME` environment variable, for example:

```shell
MAVEN_HOME=E:\Downloads\apache-maven-3.5.4-bin\apache-maven-3.5.4\
```

NOTE: You can use Maven commands directly, but running some goals in isolation might fail (try adding the `compile test-compile` goals in your invocation if that is the case).

### Building {#building}

To see what functions are supported by OpenRefine's build system, type
```shell
./refine -h
```

To build the OpenRefine application from source type:
```shell
./refine clean
./refine build
```

### Testing {#testing}
Since OpenRefine is composed of two parts, a server and a in-browser UI, the testing system reflects that:

* on the server side, it's powered by [TestNG](http://testng.org/) and the unit tests are written in Java;
* on the client side, we use [Cypress](https://www.cypress.io/) and the tests are written in Javascript

To run all tests, use:
```shell
./refine test
```
**this option is not available when using refine.bat**


If you want to run only the server side portion of the tests, use:
```shell
./refine server_test
```

If you are running the UI tests for the first time, [you must go through the installation process.](functional-tests)  
If you want to run only the client side portion of the tests, use:
```shell
./refine ui_test chrome
```

## Running {#running}
To run OpenRefine from the command line (assuming you have been able to build from the source code successfully)
```shell
./refine
```
By default, OpenRefine will use [refine.ini](https://github.com/OpenRefine/OpenRefine/blob/master/refine.ini) for configuration. You can copy it and rename it to `refine-dev.ini`, which will be used for configuration instead. `refine-dev.ini` won't be tracked by Git, so feel free to put your custom configurations into it.

## Building Distributions (Kits) {#building-distributions-kits}

The Refine build system uses Apache Ant to automate the creation of the installation packages for the different operating systems. The packages are currently optimized to run on Mac OS X which is the only platform capable of creating the packages for all three OS that we support.

To build the distributions type

```shell
./refine dist <version>
```
where 'version' is the release version.

## Building, Testing and Running OpenRefine from Eclipse {#building-testing-and-running-openrefine-from-eclipse}
OpenRefine' source comes with Maven configuration files which are recognized by [Eclipse](http://www.eclipse.org/) if the Eclipse Maven plugin (m2e) is installed.

At the command line, go to a directory **not** under your Eclipse workspace directory and check out the source:

```shell
git clone https://github.com/OpenRefine/OpenRefine.git
```

In Eclipse, invoke the `Import...` command and select `Existing Maven Projects`. 

![Screenshot of Import a Maven project option](/img/eclipse-import-maven-project-1.png)

Choose the root directory of your clone of the repository. You get to choose which modules of the project will be imported. You can safely leave out the `packaging` module which is only used to generate the Linux, Windows and MacOS distributions.

<img alt="Screenshot of Select maven projects to import" src={useBaseUrl('img/eclipse-import-maven-project-2.png')} />

To run and debug OpenRefine from Eclipse, you will need to add an execution configuration on the `server` sub-project.
Right click on the `server` subproject, click `Run as...` and `Run configurations...` and create a new `Maven Build` run configuration.  Rename the run configuration `OpenRefine`.  Enter the root directory of the project as `Base directory` and use `exec:java` as a Maven goal.

![Screenshot of Add a run configuration with the exec:java goal](/img/eclipse-exec-config.png)

This will add a run configuration that you can then use to run OpenRefine from Eclipse.

## Testing in Eclipse {#testing-in-eclipse}

You can run the server tests directly from Eclipse. To do that you need to have the TestNG launcher plugin installed, as well as the TestNG M2E plugin (for integration with Maven). If you don't have it, you can get it by [installing new software](https://help.eclipse.org/2020-03/index.jsp?topic=/org.eclipse.platform.doc.user/tasks/tasks-129.htm) from this update URL http://dl.bintray.com/testng-team/testng-eclipse-release/

Once the TestNG launching plugin is installed in your Eclipse, right click on the source folder "main/tests/server/src", select `Run As` -> `TestNG Test`. This should open a new tab with the TestNG launcher running the OpenRefine tests.

### Test coverage in Eclipse {#test-coverage-in-eclipse}

It is possible to analyze test coverage in Eclipse with the `EclEmma Java Code Coverage` plugin. It will add a `Coverage as…` menu similar to the `Run as…` and `Debug as…` menus which will then display the covered and missed lines in the source editor.

### Debug with Eclipse {#debug-with-eclipse}
Here's an example of putting configuration in Eclipse for debugging, like putting values for the Google Data extension. Other type of configurations that can be set are memory, Wikidata login information and more. 

![Screenshot of Eclipse debug configuration](/img/eclipse-debug-config.png)

## Building, Testing and Running OpenRefine from IntelliJ idea {#building-testing-and-running-openrefine-from-intellij-idea}

At the command line, go to a directory you want to save the OpenRefine project and execute the following command to clone the repository:

```shell
git clone https://github.com/OpenRefine/OpenRefine.git
```

Then, open the IntelliJ idea and go to `file -> open` and select the location of the cloned repository.

![Screenshot of Open option on the IntelliJ File menu](/img/intellij-setup-1.png)

It will prompt you to add as a maven project as the source code contains a pom.xml file in it. Allow `auto-import` so that it can add it as a maven project. 
If it doesn't prompt something like this then you can go on the right side of the IDE and click on maven then, click on `reimport all the maven projects` that will add all the dependencies and jar files required for the project.

![Screenshot of Maven project controls in IntelliJ](/img/intellij-maven.png)

After this, you will be able to properly build, test, and run the OpenRefine project from the terminal. 
But if you will go to any of the test folders and open some file it will show you some import errors because the project isn't yet set up at the module level.

For removing those errors, and enjoying the features of the IDE like ctrl + click, etc you need to set up the project at the module level too. Open the different modules like `extensions/wikidata`, `main` as a project in the IDE. Then, right-click on the project folder and open the module settings.

![Screenshot of open module settings menu in IntelliJ](/img/intellij-open-module-settings.png)

In the module settings, add the source folder and test source folders of that module. 

![Screenshot of module settings in IntelliJ](/img/intellij-module-settings.png)

Then, do the same thing for the main OpenRefine project and now you are good to go.
