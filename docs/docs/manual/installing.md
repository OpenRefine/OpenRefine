---
id: installing
title: Installing OpenRefine
sidebar_label: Installing
---


## System requirements 



*   Java 
*   Browser


### Release versions



*   What’s Changed (a plain language summary of major improvements only)
*   Layman explainer of beta, RC, and final
    *   Beta
    *   Development version
        *   [Nightly releases](https://github.com/OpenRefine/OpenRefine-nightly-releases/releases)
*   [Changelogs feed](https://github.com/OpenRefine/OpenRefine/releases)
*   Other distributions


## Installing or upgrading


### Back up your data



*   Windows
*   Mac
*   Linux


### Install/Upgrade



*   Windows
*   Mac
*   Linux


### Where is data stored?



*   Program directory
*   Workspace directory
*   Logs (error?)


## Increasing memory allocation



*   Windows
*   Mac
*   Linux


## Installing extensions

Extensions have been created by our contributor community to add functionality or provide convenient shortcuts for common uses of OpenRefine. [We list extensions we know about on our downloads page](https://openrefine.org/download.html). 

If you think you’d like to create or modify an extension, [see our developer documentation here](https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Developers). If you’re having a problem, [use our downloads page](https://openrefine.org/download.html) to go to the extension’s page and report the issue there.


### Two ways to install extensions

You can install extensions in one of two ways:



*   [Into your OpenRefine program folder](installing.md#whereisdatastored), so they will only be available to that version/installation of OpenRefine (meaning the extension will not run if you upgrade OpenRefine), or 
*   Into your workspace, where your projects are stored, so they will be available no matter which version of OpenRefine you’re using.

We provide these options because you may wish to reinstall a given extension manually each time you upgrade OpenRefine, in order to be sure it works properly. 


### Find the right place to install

If you want to install the extension into the program folder, go to `OpenRefine/webapp/extensions` (or create it if not does not exist). 

If you want to install the extension into your workspace, you can:



*   launch OpenRefine and click “Open Project” in the sidebar 
*   At the bottom of the screen, click “Browse workspace directory” 
*   A file-explorer window will open in your workspace
*   Create a new folder called “extensions” inside the workspace if it does not exist.

You can also [find your workspace on each operating system using these instructions](installing.md#whereisdatastored).  


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


