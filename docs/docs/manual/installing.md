---
id: installing
title: Installing OpenRefine
sidebar_label: Installing
---

## System requirements 

OpenRefine does not require internet access to run its basic functions. Once you download and install it, it runs as a small web server on your own computer, and you access that local web server by using your browser. It only requires an internet connection to import data from the web, reconcile data using a web service, or export data to the web. 

OpenRefine requires three things on your computer in order to function:

#### Compatible operating system

OpenRefine is designed to work with **Windows**, **Mac**, and **Linux** operating systems. [Our team releases packages for each](https://openrefine.org/download.html). 

#### Java 

Java Development Kit (JDK) must be installed and configured on your computer to run OpenRefine. The Mac version of OpenRefine includes JDK; new in OpenRefine 3.4, there is also a Windows package with JDK included. To install JDK separately, [installation and upgrade instructions are below](#install-or-upgrade-java).

#### Compatible browser

OpenRefine works best on browsers based on Webkit, such as:

*   Google Chrome
*   Chromium
*   Opera
*   Microsoft Edge

We are aware of some minor rendering and performance issues on other browsers such as Firefox. We don't support Internet Explorer. If you are having issues running OpenRefine, see the [section on Running](running.md#troubleshooting).

### Release versions

OpenRefine always has a latest stable release as well as some more recent work available in beta, release candidate, or nightly release versions. 

If you are installing for the first time, we recommend [the latest stable release](https://github.com/OpenRefine/OpenRefine/releases/latest). 

If you wish to use an extension that is only compatible with an earlier version of OpenRefine, and do not require the latest features, you may find that [an older stable version is best for you](https://github.com/OpenRefine/OpenRefine/releases) in our list of releases. Look at later releases to see which security vulnerabilities are being fixed, in order to assess your own risk tolerance for using earlier versions. Look for “final release” versions instead of “beta” or “release candidate” versions.

#### Unstable versions

If you need a recently developed function, and are willing to risk some untested code, you can look at [the most recent items in the reverse-chronological list](https://github.com/OpenRefine/OpenRefine/releases) and see what changes appeal to you. 

“Beta” and “release candidate” versions may both have unreported bugs and are most suitable for people who are wiling to help us troubleshoot these versions by [creating bug reports](https://github.com/OpenRefine/OpenRefine/issues).  

For the absolute latest development updates, see the [snapshot releases](https://github.com/OpenRefine/OpenRefine-nightly-releases/releases). These are created with every commit. 

#### What’s changed 

Our [latest release is at the time of writing is OpenRefine 3.4](**link goes here!**), released **XXXX XX 2020**. The major changes in this version are listed on the [3.4 final release page](**link goes here!**) with the downloadable packages. 

You can find information about all of our releases on the [Releases page on Github](https://github.com/OpenRefine/OpenRefine/releases).

:::info Other distributions
OpenRefine may also work in other environments, such as [Chromebooks](https://gist.github.com/organisciak/3e12e5138e44a2fed75240f4a4985b4f) where Linux terminals are available. Look at our list of [Other Distributions](https://openrefine.org/download.html) on the Downloads page for other ways of running OpenRefine, and refer to our contributor community to see new environments in development.
:::

## Installing or upgrading
### Back up your data

If you are upgrading from an older version of OpenRefine and have projects already on your computer, you should create backups of those projects before you install a new version.

First, [locate your workspace directory](installing.md#where-is-data-stored). Then copy everything you find there and paste it into a folder elsewhere on your computer. 

For extra security you can [export your existing OpenRefine projects](exporting.md#export-a-project). 

:::caution
Take note of the [extensions](#installing-extensions) you have currently installed. They may not be compatible with the upgraded version of OpenRefine. Installations can be installed in two places, so be sure to check both your workspace directory and the existing installation directory. 
:::

### Install or upgrade Java

[Java Development Kit (JDK)](https://jdk.java.net/) is required to run OpenRefine and should be installed first. [OpenRefine installation packages for Mac and Windows come bundled with JDK]([https://openrefine.org/download.html](https://openrefine.org/download.html)), so you do not need to install it separately if you use those bundles. 

There are JDK packages for Mac, Windows, and Linux. We recommend you install the latest “Ready for use” version: at the time of writing, this is [JDK 14.0.1](https://jdk.java.net/14/). 

Download the archive (either a `.tar.gz` or a `.zip`) to your computer and then extract its contents to a location of your choice. There is no installation process, so you may wish to extract this folder directly into a place where you put program files, or another stable folder. 

Once you have Java extracted onto your system, you need to tell your computer where to find it when OpenRefine looks for it.  

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

1. On Windows 10, click the Windows start menu button, type "env," and look at the search results. Click “Edit the system environment variables.” (If you are using an earlier version of Windows, use the “Search” or “Search programs and files” box in the start menu.)

![A screenshot of the search results for 'env'.](/img/env.png "A screenshot of the search results for 'env'.")

2. Click “Environment Variables…” at the bottom of the “Advanced” window that appears.
3. In the “Environment Variables” dialog that appears, click “New…” and create a variable with the key `JAVA_HOME`. You can set the variable for only your user account, as in the screenshot below, or set it as a system variable - it will work either way.

![A screenshot of 'Environment Variables'.](/img/javahome.png "A screenshot of 'Environment Variables'.")

4. Set the `Value` to the folder where you installed JDK, in the format `D:\Programs\OpenJDK`. You can locate this folder with the “Browse directory...” button.

</TabItem>

<TabItem value="mac">

First, find where Java is on your computer with this command:

```which java```

Check the environment variable `JAVA_HOME` with:

```$JAVA_HOME/bin/java --version```

To set the environment variable for the current Java version of your MacOS:

```export JAVA_HOME="$(/usr/libexec/java_home)"```

Or, for Java 13.x:

```export JAVA_HOME="$(/usr/libexec/java_home -v 13)"```

</TabItem>

<TabItem value="linux">

##### With the terminal

Enter the following:

```sudo apt install default-jre```

This probably won’t install the latest JDK package available on the Java website, but it is faster and more straightforward. (At the time of writing, it installs OpenJDK 11.0.7.)

##### Manually

First, [extract the JDK package](https://openjdk.java.net/install/) to the new directory `usr/lib/jvm`:
```
sudo mkdir -p /usr/lib/jvm
sudo tar -x -C /usr/lib/jvm -f /tmp/openjdk-14.0.1_linux-x64_bin.tar.gz
```
Then, navigate to this folder and confirm the final path (in this case, `usr/lib/jvm/jdk-14.0.1`. 
Open a terminal and type
```sudo gedit /etc/profile```
In the text window that opens, insert the following lines at the end of the “profile” file, using the path above:

```
JAVA_HOME=/usr/lib/jvm/jdk-14.0.1
PATH=$PATH:$HOME/bin:$JAVA_HOME/bin
export JAVA_HOME
export PATH
```
Save and close the file. When you are back in the terminal, type
```source /etc/environment```
Exit the terminal and restart your system. You can then check that JAVA_HOME is set properly by opening another terminal and typing
```echo $JAVA_HOME```
It should show the path you set above. 

</TabItem>

</Tabs>

---

### Install or upgrade OpenRefine

If you are upgrading an existing OpenRefine  installation, you can delete the old program files and install the new files into the same space. Do not overwrite the files as some obsolete files may be left over unnecessarily. 

:::caution
If you have extensions installed, do not delete the `webapp\extensions` folder where you installed them. You may wish to install extensions into the workspace directory instead of the program directory. There is no guarantee that extensions will be forward-compatible with new versions of OpenRefine, and we do not maintain extensions.
:::

<Tabs
  groupId="operating-systems"
  defaultValue="win"
  values={[
    {label: 'Windows', value: 'win'},
    {label: 'Mac', value: 'mac'},
    {label: 'Mac via Homebrew', value: 'mac-hb'},
    {label: 'Linux', value: 'linux'}
  ]
}>

<TabItem value="win">

Once you have downloaded the `.zip` file, extract it into a folder where you wish to store program files (such as `D:\Program Files\OpenRefine`). You can right-click on `openrefine.exe` or `refine.bat` and pin one of those programs to your Start Menu or create shortcuts for easier access. 

</TabItem>

<TabItem value="mac">

Once you have downloaded the `.dmg` file, open it and drag the OpenRefine icon onto the Applications folder icon (just like you would normally install Mac applications). 

</TabItem>

<TabItem value="mac-hb">

The quick version:

1. Install[ Homebrew from here](http://brew.sh)
2. In Terminal enter ` brew cask install openrefine`
1. Then find OpenRefine in your Applications folder.

The long version:

[Homebrew](http://brew.sh) is a popular command-line package manager for Mac. Installing Homebrew is accomplished by pasting the installation command on the Homebrew website into a Terminal window. Once Homebrew is installed, applications like OpenRefine can be installed via a simple command. You can [install Homebrew from their website]([http://brew.sh](http://brew.sh)).

###### Install

Install OpenRefine with this command:

``` brew cask install openrefine```

You should see output like this:

```
==> Downloading https://github.com/OpenRefine/OpenRefine/releases/download/2.7/openrefine-mac-2.7.dmg
########################### 100.0%
  ==> Verifying checksum for Cask openrefine
  ==> Installing Cask openrefine
  ==> Moving App 'OpenRefine.app' to '/Applications/OpenRefine.app'.
  🍺  openrefine was successfully installed!
```

Behind the scenes, this command causes Homebrew to download the OpenRefine installer, verify the file’s authenticity (using a SHA-256 checksum), mount the disk image, copy the `OpenRefine.app` application bundle into the Applications folder, unmount the disk image, and save a copy of the installer and metadata about the installation for future use.

_If an existing `OpenRefine.app` is found in the Applications folder, Homebrew will not overwrite it, so installing via Homebrew requires either deleting or renaming previously installed copies._

###### Uninstall

To uninstall OpenRefine, paste this command into the Terminal:

``` brew cask uninstall openrefine```

You should see output like this:

``` ==> Removing App '/Applications/OpenRefine.app'.```

###### Update

To update to the latest version of OpenRefine, paste this command into the Terminal:

``` brew cask reinstall openrefine```

You should see output like this:

```
 ==> Downloading https://github.com/OpenRefine/OpenRefine/releases/download/2.7/openrefine-mac-2.7.dmg
########################### 100.0%
  ==> Verifying checksum for Cask openrefine
  ==> Removing App '/Applications/OpenRefine.app'.
  ==> Moving App 'OpenRefine.app' to '/Applications/OpenRefine.app'.
  🍺  openrefine was successfully installed!
```

If you had previously installed the `openrefine-dev` cask (containing a release candidate) and you want to move to the stable release, you need to first uninstall the old cask and then install the new one:

```
 brew cask uninstall openrefine-dev
 brew cask install openrefine
```
</TabItem>

<TabItem value="linux">

Once you have downloaded the `.tar.gz` file, open a shell, navigate to the folder containing the download, and type:

```tar xzf openrefine-linux-3.4.tar.gz```

</TabItem>

</Tabs>

---


### Set where data is stored

OpenRefine stores data in two places: program files in the program directory, wherever it is you’ve installed it; and project files in what we call the “workspace directory.” You can access this folder easily from OpenRefine by going to the [home screen](running.md#the-home-screen) (at [http://127.0.0.1:3333/](http://127.0.0.1:3333/)) and clicking "Browse workspace directory."

By default this is:

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

Depending on your version of Windows, the data is in one of these directories:
*   `%appdata%\OpenRefine`
*   `%localappdata%\OpenRefine`
*   `C:\Documents and Settings\(user id)\Local Settings\Application Data\OpenRefine`
*   `C:\Users\(user id)\AppData\Roaming\OpenRefine`
*   `C:\Users\(user id)\AppData\Local\OpenRefine`
*   `C:\Users\(user id)\OpenRefine`

For older Google Refine releases, replace `OpenRefine` with `Google\Refine`.

You can change this by adding this line to the file `openrefine.l4j.ini` and specifying your desired drive and folder path:

```-Drefine.data_dir=D:\MyDesiredFolder```

If your folder path has spaces, use neutral quotation marks around it:

```-Drefine.data_dir="D:\My Desired Folder"```

If the folder does not exist, OpenRefine will create it. 

</TabItem>

<TabItem value="mac">

```~/Library/Application Support/OpenRefine/```

For older versions as Google Refine:

```~/Library/Application Support/Google/Refine/ ```

Logging is to `/var/log/daemon.log` - grep for `com.google.refine.Refine`.

</TabItem>

<TabItem value="linux">

```~/.local/share/openrefine/```

You can change this when you run OpenRefine from the terminal, by pointing to the workspace directory through the `-d` parameter:

``` ./refine -p 3333 -i 0.0.0.0 -m 6000M -d /My/Desired/Folder```

</TabItem>

</Tabs>

---


### Logs

OpenRefine does not currently output an error log, but because the OpenRefine console window is always open while OpenRefine runs in your browser, you can copy information from the console if an error occurs. 

<Tabs
  groupId="operating-systems"
  defaultValue="win"
  values={[
    {label: 'Mac', value: 'mac'}
  ]
}>

<TabItem value="mac">

You can access OpenRefine server logs from the terminal on Mac:

*   Find the OpenRefine app/icon in Finder
*   Ctrl+Click on the icon and select "Show Package Contents" from the context menu that displays
*   This should open a new Finder menu showing a folder called "Contents" - navigate into this folder then into the "MacOS" folder
*   Ctrl+Click on "JavaAppLauncher"
*   Choose 'Open With' from menu, and select "Terminal"

---

</TabItem>

</Tabs>




## Increasing memory allocation

OpenRefine relies on having computer memory available to it to work effectively. If you are planning to work with large data sets, you may wish to set up OpenRefine to handle it at the outset. By “large” we generally mean one of the following indicators:
*   more than **one million** rows
*   more than **one million **total cells
*   an input file size of more than 50 megabytes (MB)
*   more than **50** [rows per record in records mode](**running.md#records-mode**)

By default OpenRefine is set to operate with 1 gigabyte (GB) of memory (1024MB). If OpenRefine is running slowly, or you are getting "out of memory" errors (for example, `java.lang.OutOfMemoryError`), or generally feel that OpenRefine is slow, you can try allocating more memory. 

A good practice is to start with no more than 50% of whatever memory is left over after the estimated usage of your operating system, to leave memory for your browser to run.

All of the settings below use a four-digit number to specify the megabytes (MB) used. The default is usually 1024MB, but the new value doesn't need to be a multiple of 1024.

:::info Dealing with large datasets
If your project is big enough to need more than the default amount of memory, consider turning off "Parse cell text into numbers, dates, ..." on import. It's convenient, but less efficient than explicitly converting any columns that you need as a data type other than the default "string" type.
:::

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

#### Using openrefine.exe

If you run `openrefine.exe`, you will need to edit the `openrefine.l4j.ini` file found in the program directory and edit the line

```
# max memory memory heap size
-Xmx1024M
```

The line `-Xmx1024M` defines the amount of memory available in megabytes (actually [mebibytes](https://en.wikipedia.org/wiki/Mebibyte)). Change the number “1024” - for example, edit the line to `-Xmx2048M` to make 2048MB [2GB] of memory available. 

:::caution openrefine.exe not running?
Once you increase the memory allocation, you may find that you cannot run `openrefine.exe`. In this case, your computer needs a 64-bit version of [Java](https://www.java.com/en/download/help/index_installing.xml) (this is different from [Java JDK](#install-or-upgrade-java). Look for the “Windows Offline (64-bit)” download on the Downloads page and install that. Your system must also be set to use the 64-bit version of Java by [changing the Java configuration](https://www.java.com/en/download/help/update_runtime_settings.xml).
:::

#### Using refine.bat

On Windows, OpenRefine can also be run by using the file `refine.bat` in the program directory. If you start OpenRefine using `refine.bat`, the memory available to OpenRefine can be specified either through command line options, or through the `refine.ini` file.

To set the maximum amount of memory on the command line when using `refine.bat`, 'cd' to the program directory, then type

```refine.bat /m 2048m```

where "2048" is the maximum amount of MB that you want OpenRefine to use.

To change the default that `refine.bat` uses, edit the `refine.ini` line that reads 

```REFINE_MEMORY=1024M```

Note that this file is only read if you use `refine.bat`, not `openrefine.exe`.

</TabItem>
<TabItem value="mac">

If you have downloaded the `.dmg` package and you start OpenRefine by double-clicking on it:

*   close OpenRefine
*   "control-click" on the OpenRefine icon (opens the contextual menu)
*   click on "show package content" (a finder window opens)
*   open the "Contents" folder
*   open the "Info.plist" file with any text editor (like Mac's default TextEdit)
*   Change "-Xmx1024M" into, for example, “-Xmx2048M” or “-Xmx8G”
*   save the file
*   restart OpenRefine.

</TabItem>
<TabItem value="linux">

If you have downloaded the `.tar.gz` package and you start OpenRefine from the command line, add the "-m xxxxM" parameter like this:

`./refine -m 2048m`

#### Setting a default

If you don't want to set this option on the command line each time, you can also set it in the `refine.ini` file. Edit the line

```REFINE_MEMORY=1024M```

Make sure it is not commented out (that is, that the line doesn't start with a '#' character), and change “1024” to a higher value. Save the file, and when you next start OpenRefine it will use this value.

</TabItem>

</Tabs>

---


## Installing extensions

Extensions have been created by our contributor community to add functionality or provide convenient shortcuts for common uses of OpenRefine. [We list extensions we know about on our downloads page](https://openrefine.org/download.html). 

:::info
If you’d like to create or modify an extension, [see our developer documentation here](https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Developers). If you’re having a problem, [use our downloads page](https://openrefine.org/download.html) to go to the extension’s page and report the issue there.
:::

### Two ways to install extensions

You can [install extensions in one of two places](installing.md#set-where-data-is-stored):

*   Into your OpenRefine program folder, so they will only be available to that version/installation of OpenRefine (meaning the extension will not run if you upgrade OpenRefine), or 
*   Into your workspace, where your projects are stored, so they will be available no matter which version of OpenRefine you’re using.

We provide these options because you may wish to reinstall a given extension manually each time you upgrade OpenRefine, in order to be sure it works properly. 

### Find the right place to install

If you want to install the extension into the program folder, go to your program directory and then go to `/webapp/extensions` (or create it if not does not exist). 

If you want to install the extension into your workspace, you can:
*   launch OpenRefine and click “Open Project” in the sidebar 
*   At the bottom of the screen, click “Browse workspace directory” 
*   A file-explorer or finder window will open in your workspace
*   Create a new folder called “extensions” inside the workspace if it does not exist.

You can also [find your workspace on each operating system using these instructions](installing.md#set-where-data-is-stored).  

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
