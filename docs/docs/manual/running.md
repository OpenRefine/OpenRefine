---
id: running
title: Running OpenRefine
sidebar_label: Running
---

## Starting and exiting

OpenRefine does not require internet access to run its basic functions. Once you download and install it, it runs as a small web server on your own computer, and you access that local web server by using your browser. 

You will see a command line window open when you run OpenRefine. Leave that window alone while you work on datasets in your browser. 

No matter how you load OpenRefine, it will load in your computer’s default browser. If you would like to use another browser instead, start OpenRefine and then point your chosen browser at the home screen: [http://127.0.0.1:3333/](http://127.0.0.1:3333/).

OpenRefine works best on browsers based on Webkit, such as:
*   Google Chrome
*   Chromium
*   Opera
*   Microsoft Edge

We are aware of some minor rendering and performance issues on other browsers such as Firefox. We don't support Internet Explorer.

You can launch multiple projects at the same time by simply having multiple tabs or browser windows open. From the “Open Project” screen, you can right-click on project names and select “Open in new tab.” 

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
To exit OpenRefine, close all the browser tabs, then navigate to the command line window. To close this window and ensure OpenRefine exits properly, hold down `control` and press `C` on your keyboard.

#### With openrefine.exe
You can run OpenRefine by double-clicking `openrefine.exe` or calling it from the command line. If you want to [modify the way `openrefine.exe` opens](#starting-with-modifications), you can edit the `openrefine.l4j.ini` file.  

#### With refine.bat
On Windows, OpenRefine can also be run by using the file `refine.bat` in the program directory. If you start OpenRefine using `refine.bat`, you can do so by opening the file itself, or by calling it from the command line. 

If you call `refine.bat` from the command line, you can [start OpenRefine with modifications](#starting-with-modifications). If you want to modify the way `refine.bat` opens through double-clicking or using a shortcut, you can edit the `refine.ini` file.  

</TabItem>

<TabItem value="mac">
You can find OpenRefine in your Applications folder, or you can call it from the command line. To exit, close all your OpenRefine browser tabs, go back to the terminal window and press `Command` and `Q` to close it down.

:::caution Problems starting?
If you are using an older version of OpenRefine, you may get a message saying "Open Refine can't be opened because it is from an unidentified developer". Try the following steps to resolve it: 
*   Open System Preferences and go to "Security and Privacy" and the “General” tab. You will see a message indicating that "OpenRefine was blocked from opening because it is not from an identified developer." You may also see “MacOS cannot verify that this app is free from malware.” See [issue #2191](https://github.com/OpenRefine/OpenRefine/issues/2191) for more information.
*   Click the "Open Anyway" button to complete the OpenRefine installation. 
If you are using an older version of MacOS, such as Mountain Lion or Yosemite, you may encounter different error messages. See [issue #590](https://github.com/OpenRefine/OpenRefine/issues/590) for more information. If you use Yosemite you will need to install[ Java for OS X 2014-001](http://support.apple.com/kb/DL1572) first.
:::

</TabItem>

<TabItem value="linux">
Use a terminal to launch OpenRefine. First, navigate to the installation folder. Then call the program:

```
cd openrefine-3.4
  ./refine
```

This will start OpenRefine and open your browser to the home screen.

To exit, close all the browser tabs, and then press `control` and `C` in the terminal window.

:::caution Did you get a JAVA_HOME error?
“Error: Could not find the ‘java’ executable at ‘’, are you sure your JAVA_HOME environment variable is pointing to a proper java installation?”

If you see this error, you need to [install and configure a JDK package](installing#linux), including setting up `JAVA_HOME`.
:::

</TabItem>

</Tabs>

---

### Troubleshooting
If you are having problems connecting to OpenRefine with your browser, you may need to: 
*   double-check your Firefox proxy settings. Look under “Options” for “Network Settings” on the “General” tab, and switch from “Use system proxy settings” to “Auto-detect proxy settings for this network.”
*   uncheck "automatically detect settings" and add an exception to your firewall rules to allow `127.0.0.1` (or the custom IP address you run OpenRefine with). Do this if you are getting a "Network Error (tcp_error)" message.
*   configure OpenRefine to use an IP address other than `127.0.0.1` and a port other than `3333`.
*   install Microsoft Loopback Adapter. Do this if OpenRefine seems to start up on Windows, but your browser won't connect to `127.0.0.1`. See [this issue on the Data Carpentry tutorial](https://github.com/datacarpentry/OpenRefine-ecology-lesson/issues/29).

### Starting with modifications

When you run OpenRefine from a command line, you can change a number of default settings. To see the full list of command-line options, run `refine -h`.

On Windows, use a slash:

```C:>refine /i 127.0.0.2 /p 3334```

_How do you call --debug and --jmx in windows?_

|Command|Use|Syntax example|
|---|---|---|
|-w|Path to the webapp|refine -w /path/to/openrefine|
|-d|Path to the workspace|refine -d /the/workspace|
|-m|Memory maximum heap|./refine -m 6000M|
|-p|Port|refine -p 3334|
|-i|Interface (IP address, or IP and port)|refine -i 127.0.0.2:3334|
|-k|Add a Google API key|_need an example_|
|-v|Verbosity (from low to high): error,warn,info,debug,trace|_need an example_|
|-x|Additional configuration parameters|_need an example_|
|--debug|Enable debugging (on port 8000)|refine --debug|
|--jmx|Enable JMX monitoring for Jconsole and JvisualVM|refine --jmx|

#### Modifications with `.ini` files

You can change different settings by editing the `.ini` files in the Windows installation. 

<Tabs
  groupId="operating-systems"
  defaultValue="win"
  values={[
    {label: 'Windows', value: 'win'}
  ]
}>

<TabItem value="win">

You can modify the way `openrefine.exe` runs by editing `openrefine.l4j.ini`; you can modify the way `refine.bat` runs by editing `refine.ini`. These are different options and have different syntax than the key/value descriptions above. Some of the most common keys with their defaults are:



*   refine.autoreload - false
*   refine.autosave|Integer - 5 (minutes)
*   refine.connection.max_idle_time - 60000
*   refine.context_path - /
*   refine.data_dir - /
*   refine.development - false
*   refine.headless - false
*   refine.host - 127.0.0.1
*   refine.max_form_content_size - 1048576
*   refine.port - 3333
*   refine.queue.idle_time - 60
*   refine.queue.max_size - 300
*   refine.queue.size - 30
*   refine.scanner.period - 1
*   refine.verbosity - info
*   refine.webapp - main/webapp

The syntax within the `.ini` files is as follows:


```
-Drefine.autoreload=true
-Drefine.port=3333 
-Drefine.host=127.0.0.1
-Drefine.webapp=broker/core
```


etc.

Refer to the[ official Java documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/java.html) for more preferences that can be set.

</TabItem>

</Tabs>

---

## The home screen

When you first launch OpenRefine, you will see a menu on the left hand side that includes “Create Project,” “Open Project,” “Import Project,” and “Language Settings.” This is called the “home screen,” where you can manage your projects and general settings. 

### Language Settings

You can set your preferred interface language here. This language setting will persist until you change it again in the future. Currently we support 23 languages: 

* Arabic
* Bengali
* Cebuano
* Czech
* German
* English (UK)
* English (US)
* Spanish
* Filipino
* French
* Hebrew
* Magyar
* Italian
* Japanese
* Korean
* Norwegian Bokmål
* Dutch
* Punjabi
* Portuguese (Brazil)
* Romanian
* Swedish
* Tagalog
* Chinese


:::info
We use Weblate to provide translations for the software interface. You can check [our profile on Weblate]([https://hosted.weblate.org/projects/openrefine/#languages](https://hosted.weblate.org/projects/openrefine/#languages)) to see which languages are in the process of being supported. We would love your help to make OpenRefine even more accessible to people in other languages.
:::


### Preferences

At this time you can set preferences using a key/value pair: that is, selecting one of the keys below and setting a value for it. 

|Setting|Key|Value syntax|Default|Example|
|---|---|---|---|---|
|Interface language|userLang|[ISO 639-1](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) two-digit code|en|fr|
|Maximum facets|ui.browsing.listFacet.limit|Number|2000|5000|
|Timeout for Google Drive import|googleReadTimeOut|Number (microseconds)|180000|500000|
|Timeout for Google Drive authorization|googleConnectTimeOut|Number (microseconds)|180000|500000|
|Maximum lag for Wikidata edit retries|wikibase.upload.maxLag|Number (seconds)|5|10|

To leave the Preferences screen, click on the “OpenRefine” logo or the diamond.

If the preference you’re looking for isn’t here, look at the options you can set from the [command line or in an `.ini` file](#starting-with-modifications).

## The project screen

Once you have [begun to work on a project](#starting), you will spend most of your time on the project screen (or work screen). This is a quick walkthrough of the parts of the interface you should familiarize yourself with.

![Image goes here...](/img/projectscreen.png)


### Logo

At any time you can close your current project and go back to the home screen by clicking on the “OpenRefine” logo or the diamond. If you’d like to open another project in a new browser tab or window, you can right-click on the logo and use “Open in a new tab.” 

:::caution
Don’t click the “back” button on your browser. 
:::

### Project title
You can rename a project at any time by clicking inside the project title, which will turn into a text field. Project names don’t have to be unique, as OpenRefine organizes them based on a unique ID. 

### Open, Export, Help buttons
About metadata, Tags, Browse workspace, etc.

### The project header
OpenRefine will tell you the total number of rows in your project, although it won’t display them all at one time. You can keep track of whether rows are being created or deleted 

Number of selected/ total rows/records, pagination, display rows 
Rows vs. records

### Extensions dropdown 

### The project grid 
(or The grid, or The grid view, or The data grid, why have I seen so many different names for this)

### The project sidebar

#### Facet/Filter
Permalink

#### History (Undo/Redo)

In OpenRefine, any activity that changes the data can be undone. Changes are tracked from the very beginning, when a project is first created. The undo/redo change history of each project is saved with the project's data, so quitting OpenRefine does not erase the history. When you restart OpenRefine, you can view and undo changes that you made before you quit OpenRefine. 

Project history gets saved when you export a project archive, and restored when you import that archive to a new installation of OpenRefine. 

![A screenshot of the History (Undo/Redo) tab with 5 steps.](/img/history.jpg "A screenshot of the History (Undo/Redo) tab with 5 steps.")

When you click on “Undo / Redo” in the sidebar of any project, that project’s history is shown as a list of changes in order, with the first "change" being the action of creating the project itself. (That first change, indexed as step zero, cannot be undone.) Here is a sample history with 3 changes:

```
0. Create project
1. Remove 7 rows
2. Create new column Last Name based on column Name with grel:value.split(" ") 
3. Split 230 cell(s) in column Address into several columns by separator
```

The current state of the project is highlighted with a dark blue background. If you move back and forth on the **_timeline_** you will see the current state become highlighted, while the actions that came after that state will be grayed out. 

To revert your data back to an earlier state, simply click on the last action in the timeline you want to keep. In the example above, if we keep the removal of 7 rows but revert everything we did after that, then click on "Remove 7 rows." The last 2 changes will be undone, in order to bring the project back to state #1.

In this example, changes #2 and #3 will now be grayed out. You can redo a change by clicking on it in the history - everything up to and including it will be redone. 

If you have moved back one or more states, and then you perform a new operation on your data, the later actions (everything that’s greyed out) will be erased and cannot be re-applied. 

The Undo/Redo tab will show you which step you’re on, and if you’re about to risk erasing work - by saying something like “4/5" or “1/7” at the end.


##### Reusing Operations

Operations that you perform in OpenRefine can be reused. For example, a formula you wrote inside one project can be copied and applied to another project later. 

To reuse one or more operations, you first extract it from the project where it was first applied. Click to the Undo/Redo tab and click “Extract…”. This brings up a box that lists all operations up to the current state (it does not show undone operations). Select the operation or operations you want to extract using the checkboxes on the left, and they will be encoded as JSON on the right. Copy that JSON off to the clipboard. 

Move to the second project, go to the Undo/Redo tab, click “Apply…” and paste in that JSON.

_Not all operations can be extracted. Edits to a single cell can’t be replicated._

##### Known Issues

_If your project history is bigger than 100 MB (output file), Chrome will crash when you try to extract the JSON. Firefox can handle around 200 MB (output file). You may need to disable your spellchecker._

### Common extension buttons
