---
id: installing
title: Installing OpenRefine
sidebar_label: Installing
---


## System requirements 

OpenRefine does not require internet access to run its basic functions. Once you download and install it, it runs as a small web server on your own computer, and you access that local web server by using your browser. It only requires an internet connection to import data from the web, reconcile data using a web service, or export data to the web. 

OpenRefine requires three things on your computer in order to function:


#### Compatible Operating System

OpenRefine is designed to work with Windows, Mac, and Linux operating systems. Our team releases packages for each. 


:::tip

OpenRefine may also work in other environments where Linux terminals are available, such as [Chromebooks](https://gist.github.com/organisciak/3e12e5138e44a2fed75240f4a4985b4f), but this is a community-contributed guide, not a core supported function.

:::


#### Java 

Java Development Kit (JDK) must be installed and configured on your computer to run OpenRefine. [Installation and upgrade instructions are below](#install-or-upgrade-java). 


#### Compatible Browser

OpenRefine works best on browsers based on Webkit, such as:



*   Google Chrome
*   Chromium
*   Opera
*   Microsoft Edge

We are aware of some minor rendering and performance issues on other browsers such as Firefox. We don't support Internet Explorer. If you are having issues running OpenRefine, see the [section on Running](running.md#troubleshooting).


### Release versions

OpenRefine always has a latest stable release as well as some more recent work available in beta, release candidate, or nightly release versions. 

If you are installing for the first time, we recommend [the latest stable release](https://github.com/OpenRefine/OpenRefine/releases/latest). 

If you wish to use an extension that is only compatible with an earlier version of OpenRefine, and do not require the latest features, you may find that [an older stable version is best for you](https://github.com/OpenRefine/OpenRefine/releases) in our list of releases. Look at later releases to see which vulnerabilities are being fixed, in order to assess your own risk tolerance for using earlier versions. Look for “final release” versions instead of “beta” or “release candidate” versions.


#### Unstable versions

If you need a recently developed function, and are willing to risk some untested code, you can look at [the most recent items in the reverse-chronological list](https://github.com/OpenRefine/OpenRefine/releases) and see what changes appeal to you. 

“Beta” and “release candidate” versions may both have unreported bugs and are most suitable for people who are wiling to help us troubleshoot these versions by [creating bug reports](https://github.com/OpenRefine/OpenRefine/issues).  

For the absolute latest development updates, see the [snapshot releases](https://github.com/OpenRefine/OpenRefine-nightly-releases/releases). These are created with every commit. 


#### What’s Changed 

Our [latest release is at the time of writing is OpenRefine 3.4](link goes here!), released XXXX XX 2020. The major changes in this version are listed on the [3.4 final release page](link goes here!) with the downloadable packages. 



:::note

[This link always goes to the actual latest release](https://github.com/OpenRefine/OpenRefine/releases/latest), whether this user manual is updated or not. 

:::

You can find information about all of our releases on the [Releases page on Github](https://github.com/OpenRefine/OpenRefine/releases).


:::tip Other distributions

Our community of contributors may have created a version of OpenRefine that works for you. Look at the list of “Other Distributions” on our [Download page]([https://openrefine.org/download.html](https://openrefine.org/download.html)).

:::


## Installing or upgrading


### Back up your data

If you are upgrading from an older version of OpenRefine and have projects already on your computer, you should create backups of those projects before you install a new version.

First, [locate your workspace directory](#where-is-data-stored). Then copy everything you find there and paste it into a folder elsewhere on your computer. 

For extra security you can [export your existing OpenRefine projects](exporting.md#export-a-project). 


:::caution

Take note of the [extensions](#installing-extensions) you have currently installed. They may not be compatible with the upgraded version of OpenRefine. Installations can be installed in two places, so be sure to check both your workspace directory and the existing installation directory. 

:::


### Install or upgrade Java

[Java Development Kit (JDK)](https://jdk.java.net/) is required to run OpenRefine and should be installed first. There are packages for Mac, Windows, and Linux. We recommend you install the latest “Ready for use” version: at the time of writing, this is [JDK 14.0.1](https://jdk.java.net/14/). 

Download the archive (either a `.tar.gz` or a `.zip`) to your computer and then extract its contents to a location of your choice. There is no installation process, so you may wish to extract this folder directly into a place where you put program files, or another stable folder. 

Once you have Java extracted onto your system, you need to tell your computer where to find it when OpenRefine looks for it. 


#### Windows



1. On Windows 10, click the Windows start menu button, type "env," and look at the search results. Click “Edit the system environment variables.”  If you are using an earlier version of Windows, use the “Search” or “Search programs and files” box in the start menu.
2. Click `Environment Variables…` at the bottom of the `Advanced` window that appears.
3. In the `Environment Variables` dialog that appears, click `New…` and create a `JAVA_HOME**`** variable. Set the `Value` to the folder where you installed JDK, in the format `D:\Programs\OpenJDK`.

#### Mac OS

First, find where Java is on your computer with this command:

```which java```

Check the environment variable `JAVA_HOME**` **with:

```$JAVA_HOME/bin/java --version```

To set the environment variable for the current Java version of your MacOS:

```export JAVA_HOME="$(/usr/libexec/java_home)"```

Or, for Java 13.x:

```export JAVA_HOME="$(/usr/libexec/java_home -v 13)"```


#### Linux

[Please read this guide by Charini Nanayakkara for setting up JAVA_HOME on Ubuntu.](https://medium.com/@charinin/setting-java-home-environment-variable-in-ubuntu-e355c80e5b6c)

>This post assumes that you have already installed JDK in your system. Setting `JAVA_HOME` is important for certain applications. This post guides you through the process to be followed to set `JAVA_HOME` environment variable.

>Open a terminal

>Open “profile” file using following command: sudo gedit /etc/profile

>Find the java path in `/usr/lib/jvm`. If it’s JDK 7 the java path would be something similar to `/usr/lib/jvm/java-7-oracle`

>Insert the following lines at the end of the “profile” file

>`JAVA_HOME=/usr/lib/jvm/java-7-oracle`

>`PATH=$PATH:$HOME/bin:$JAVA_HOME/bin`

>`export JAVA_HOME`

>`export PATH`

>Save and close the file.

>Type the following command: 

> `source /etc/environment`

>You may have to restart the system

>Check whether `JAVA_HOME` is properly set with following command: `echo $JAVA_HOME`. If it’s properly set, `/usr/lib/jvm/java-7-oracle` would be displayed on the terminal.



### Install or upgrade

If you are upgrading an existing OpenRefine  installation, you can delete the old program files and install the new files into the same space. Do not overwrite the files as some obsolete files may be left over unnecessarily. 

:::caution

If you have extensions installed, do not delete the `webapp\extensions` folder where you installed them. You may wish to install extensions into the workspace directory instead of the program directory. There is no guarantee that extensions will be forward-compatible with new versions of OpenRefine, and we do not maintain extensions.

:::


#### Windows

Once you have downloaded the `.zip` file, and uncompress it into a folder where you wish to store program files (such as `D:\Program Files\OpenRefine`). 


#### Mac


##### Install via disk image: 

Once you have downloaded the `.dmg` file, open it and drag the OpenRefine icon onto the Applications folder icon (just like you would normally install Mac applications). 


:::info

If you get a message saying "Open Refine can't be opened because it is from an unidentified developer": 


*   Open System Preferences and go to "Security and Privacy" and the “General” tab. You will see a message indicating that "OpenRefine was blocked from opening because it is not from an identified developer." You may also see “macOS cannot verify that this app is free from malware.” See [issue #2191](https://github.com/OpenRefine/OpenRefine/issues/2191) for more information.
*   Click the "Open Anyway" button to complete the OpenRefine installation. 

If you are using an older version of MacOS, such as Mountain Lion or Yosemite, you may encounter different error messages and issue. See [issue #590](https://github.com/OpenRefine/OpenRefine/issues/590) for more information. If you use Yosemite you will need to install[ Java for OS X 2014-001](http://support.apple.com/kb/DL1572) first.

:::


##### Install via Homebrew: 

The quick versions is:



1. Install[ Homebrew from here](http://brew.sh)
2. In Terminal enter ` brew cask install openrefine`
1. Then find OpenRefine in your Applications folder.

We have a detailed[ Homebrew installation guide](https://github.com/OpenRefine/OpenRefine/wiki/Homebrew) if you need it.


#### Linux

Once you have downloaded the `.tar.gz` file, open a shell and type:

```tar xzf openrefine-linux-3.4.tar.gz```


### Where is data stored?

OpenRefine stores data in two places: program files in the program directory (wherever it is you’ve installed it), and project files in what we call the “workspace directory.” You can access this folder easily from OpenRefine by going to the home screen (at [http://127.0.0.1:3333/](http://127.0.0.1:3333/)) and clicking "Browse workspace directory."

By default this is:

**On Mac:**

```~/Library/Application Support/OpenRefine/```

For older versions as Google Refine:

```~/Library/Application Support/Google/Refine/ ```

Logging is to `/var/log/daemon.log` - grep for `com.google.refine.Refine`.

**On Windows:** 

Depending on your version of Windows, the data is in one of these directories:



*   `%appdata%\OpenRefine`
*   `%localappdata%\OpenRefine`
*   `C:\Documents and Settings\(user id)\Local Settings\Application Data\OpenRefine`
*   `C:\Users\(user id)\AppData\Roaming\OpenRefine`
*   `C:\Users\(user id)\AppData\Local\OpenRefine`
*   `C:\Users\(user id)\OpenRefine`

For older Google Refine releases, replace `OpenRefine` with `Google\Refine`.

You can change this by adding this line to the file `openrefine.l4j.ini` and specifying your desired drive and folder path:

```-Drefine.data_dir=D:\**MyDesiredFolder**```

**On Linux:**

```~/.local/share/openrefine/```

You can change this when you run OpenRefine from the terminal, by pointing to the workspace directory through the `-d` parameter:

``` ./refine -p 3333 -i 0.0.0.0 -m 6000M -d /**My/Desired/Folder**```


#### Logs

OpenRefine does not currently output an error log, but because the OpenRefine console window is always open while OpenRefine runs in your browser, you can copy information from the console if an error occurs. 




## Increasing memory allocation



*   Windows
*   Mac
*   Linux


## Installing extensions

Extensions have been created by our contributor community to add functionality or provide convenient shortcuts for common uses of OpenRefine. [We list extensions we know about on our downloads page](https://openrefine.org/download.html). 


:::tip

If you think you’d like to create or modify an extension, [see our developer documentation here](https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Developers). If you’re having a problem, [use our downloads page](https://openrefine.org/download.html) to go to the extension’s page and report the issue there.

:::


### Two ways to install extensions

You can install extensions in one of two ways:



*   [Into your OpenRefine program folder](installing.md#where-is-data-stored), so they will only be available to that version/installation of OpenRefine (meaning the extension will not run if you upgrade OpenRefine), or 
*   Into your workspace, where your projects are stored, so they will be available no matter which version of OpenRefine you’re using.

We provide these options because you may wish to reinstall a given extension manually each time you upgrade OpenRefine, in order to be sure it works properly. 


### Find the right place to install

If you want to install the extension into the program folder, go to your program directory and then go to `/webapp/extensions` (or create it if not does not exist). 

If you want to install the extension into your workspace, you can:



*   launch OpenRefine and click “Open Project” in the sidebar 
*   At the bottom of the screen, click “Browse workspace directory” 
*   A file-explorer window will open in your workspace
*   Create a new folder called “extensions” inside the workspace if it does not exist.

You can also [find your workspace on each operating system using these instructions](installing.md#where-is-data-stored).  


### Install the extension

Some extensions have their own instructions: make sure you read the documentation before you begin installing. 

Some extensions may have multiple versions, to match OpenRefine versions, so be sure to choose the right release for your installation. If you have questions about compatibility or want to request or voice your support for an update, [use our downloads page](https://openrefine.org/download.html) to go to the extension’s page and report the issue there.

Generally, the installation process will be:



*   Download the extension (usually as a zip file from GitHub)
*   Extract the zip contents into the `extensions` directory, making sure all the contents go into one folder with the name of the extension
*   Start (or restart) OpenRefine.

To confirm that installation was a success, follow the instructions provided by the extension. Each extension will appear in its own way inside the OpenRefine interface: make sure you read the documentation to know where the functionality will appear, such as under specific dropdown menus. 


## Advanced OpenRefine uses


### Running as a server


### Automating OpenRefine


