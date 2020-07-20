---

id: running

title: Running OpenRefine

sidebar_label: Running

---


## Starting and exiting



*   The window, multiple browser tabs, Ctrl-C, etc.
*   Note that it runs offline - your data is private!


## The home screen



*   Language Settings ( TIP: mention language stays set until changed by user in future)
*   Preferences
    *   Command line options
    *   INI file options


## The project/work screen

Clicking the diamond  (or on “OpenRefine")


(Don’t click the browser back button!)


### Project title


### Open, Export, Help buttons


About metadata, Tags, Browse workspace, etc.


### The project header


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


![A screenshot of the History (Undo/Redo) tab with 5 steps.](img/history.jpg "A screenshot of the History (Undo/Redo) tab with 5 steps.")



##### Overview

When you click on “Undo / Redo” in the sidebar of any project, that project’s history is shown as a list of changes in order, with the first "change" being the action of creating the project itself. (That first change, indexed as step zero, cannot be undone.) Here is a sample history with 3 changes:


```
 0. Create project
  1. Remove 7 rows
  2. Create new column Last Name based on column Name by filling 67 rows with grel:value.split(" ")[1] 
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
