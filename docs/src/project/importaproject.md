---
id: importingaproject
title: Importing A Project
sidebar_label: Importing A Project
---

_Path: docs/Start A Project/Importing a Project_


## Overview

Because OpenRefine only runs locally on your computer, you can’t have a project accessible to more than one person at the same time. 

The best way to collaborate with another person is to export and import projects that save all your changes, so that you can pick up where someone else left off. You can also [export projects](https://docs.openrefine.org/exporters/project.html) and import them to new computers of your own, such as for working on the same project from the office and from home. 

An exported project will include all of the [history](https://github.com/OpenRefine/OpenRefine/wiki/History), so you can see (and undo) all the changes from the previous user. It is essentially a point-in-time snapshot of their work. OpenRefine only exports projects as .tar.gz files at this time. 


## Instructions

Once someone has sent you a project archive file from their computer, you can save it anywhere, including your Downloads folder. 

Launch OpenRefine, or click the diamond if it’s already open, which will take you back to the home screen. 

In the left-hand menu, click “Import Project.” You will see the following screen:



![A screenshot of the Import Project screen.](/img/import.jpg)


Click “Browse…” and navigate to wherever you saved the file you were sent (for example, your Downloads folder). 

You can rename the project if you’d like - we recommend adding your name, a date, or a version number, if you’re planning to continue collaborating with another person (or working from multiple computers).

Then, click “Import Project.”  Your project should appear with a step count beside “undo/redo” if steps were saved by the exporter. 

OpenRefine will store the project in its own workspace directory, so you can now delete the original file that was sent to you.