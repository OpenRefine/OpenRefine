---
id: starting
title: Starting a project
sidebar_label: Starting a project
---

## Overview

An OpenRefine project is started by importing in some existing data - OpenRefine doesn’t allow you to create a dataset from nothing.

No matter where your data comes from, OpenRefine doesn’t modify your original data source. It copies all the information from your input, creates its own project file, and stores it in your [workspace directory](installing#set-where-data-is-stored).   

The data and all of your edits are automatically saved inside the project file. When you’re finished modifying the data, you can export it back out into the file format of your choice. 

You can also receive and open other people’s projects, or send them yours, by exporting a project archive and importing it. 

## Create project by importing data

When you start OpenRefine, you’ll be taken to the “Create Project” screen. You’ll see on the left side of the screen that your options are to: 

*   import data from a file on your computer
*   import data from a link to the web
*   import data by pasting in text from your clipboard
*   import data from a database (using SQL), and
*   import Sheets from Google Drive. 

From these sources, you can load any of the following file formats:

*   comma-separated values (CSV) or text-separated values (TSV)
*   Text files
*   Fixed-width columns
*   JSON
*   XML
*   OpenDocument spreadsheet (ODS)
*   Excel spreadsheet (XLS or XLSX)
*   PC-Axis (PX)
*   MARC
*   RDF data (JSON-LD, N3, N-Triples, Turtle, RDF/XML)
*   Wikitext

More formats can be imported by [adding extensions to provide that functionality](https://openrefine.org/download.html). 

If you supply two or more files for one project, the files’ rows will be loaded in the order that you specify, and OpenRefine will create a column at the beginning of the dataset with the source URL or file name in it to help you identify where each row came from. If the files have matching columns, the data will load in each column; if not, the successive files will append all of their new columns to the end of the dataset:

|File|Fruit|Quantity|Berry|Berry source|
|---|---|---|---|---|
|fruits.csv|Orange|4|
|fruits.csv|Apple|6|
|berries.csv||9|Mulberry|Greece|
|berries.csv||2|Blueberry|Canada|


For whichever method you choose, when you click <span class="menuItems">Next >></span> you will be given a preview and a chance to configure the way OpenRefine interprets the file.  

### Get data from this computer

Click on <span class="menuItems">Browse…</span> and select a file on your hard drive. All files will be shown, not just compatible ones. 

If you import an archive file (something with the extension `.zip`, `.tar.gz`, `.tgz`, `.tar.bz2`, `.gz`, or `.bz2`), OpenRefine detects the files inside it, shows you a preview screen, and allows you to select which ones to load. This does not work with `.rar` files. 

### Web Addresses (URLs)

Type or paste the URL to the data file into the field provided. You can add as many fields as you want. OpenRefine will download the file and preview it for you. 

If you supply two or more file URLs, OpenRefine will identify each one and ask you to choose which (or all) to load. 

Do not use this form to load a Google Sheet by its link; use [the Google Data form instead](#google-data). 

### Clipboard

You can copy and paste in data from anywhere. OpenRefine will recognize comma-separated, tab-separated, or table-formatted information copied from sources such as word-processing documents, spreadsheets, and tables in PDFs. You can also just paste in a list of items that you want to turn into multi-column rows. OpenRefine recognizes each new text line as a row. 

This can be useful if you want to pre-select a specific number of rows from your source data, or paste together rows from different places, rather than delete unwanted rows later in the project interace. 

This can also be useful if you would like to paste in a list of URLs, which you can use later to fetch the data online and build columns with. 

### Database (SQL)

If you are an administrator or have SQL access to a database of information, you may want to pull the latest dataset directly from there. This could include an online catalogue, a content management system, or a digital repository or collection management system. You can also load a database (`.db`) file saved locally. You will need to use an [SQL query](https://www.w3schools.com/sql/) to import your intended data.

There are some publicly-accessible databases you can query, such as [one provided by Rfam](https://docs.rfam.org/en/latest/database.html). The instructions provided by Rfam can help you understand how to connect to and query from any database.

OpenRefine can connect to PostgreSQL, MySQL, MariaDB, and SQLite database systems. It will automatically populate the <span class="menuItems">Port</span> field based on which of these you choose, but you can manually edit this if needed.

If you have a `.db` file, you can supply the path to the file on your computer directly in the <span class="menuItems">Database</span> field at the bottom of the form. You can leave the rest of the fields blank.

To import data directly from a database, you will need the database type (such as MySQL), database name, the hostname (either an IP address or the domain that hosts the database), and the port on the host. You will need an account authorized for access, and you may need to add OpenRefine's IP address or host to the <span class="menuItems">allowable hosts</span> for that account. You can find that information by pressing <span class="menuItems">Test</span> and getting the IP from the error message that results.

You can either connect just once to gather data, or save the connection to use it again later. If you press <span class="menuItems">Connect</span> without saving, OpenRefine will forget all the information you just entered. If you’d like to save the connection, name your connection in a way you will recognize later. Click <span class="menuItems">Save</span> and it will appear in the <span class="menuItems">Saved Connections</span> list on the left. From now on, you can click on the <span class="menuItems">...</span> ellipsis to the right of the connection you’ve saved, and click <span class="menuItems">Connect</span>.

If your connection is successful, you will see a Query Editor where you can run your SQL query. OpenRefine will give you an error if you write a statement that tries to modify the source database in any way.

### Google Data

You have two ways to load in data from Google Sheets:
*   A link to an accessible Google Sheet (that is, one with link-sharing turned on)
*   Selecting a Google Sheet in your Google Drive


#### Google Sheet by URL

You can import data from any Google Sheet that has link-sharing turned on. Paste in a URL that looks something like

```https://docs.google.com/spreadsheets/………/edit?usp=sharing```

This will only work with Sheets, not with any other Google Drive file that might have an available link, including `.xls` and other valid files that are hosted in Google Drive. These links will also not work [by URL](#web-addresses-urls), so you need to download the files to your computer.

#### Google Sheet from Drive

You can authorize OpenRefine to access your Google Drive data and import data from any Google Sheet it finds there. This will include Sheets that belong to you and Sheets that are shared with you, as well as Sheets that are in your trash. 

OpenRefine will not show spreadsheets that are in your email inbox or stored in any other Google property - only in Drive. It also won’t show all compatible file formats, only Sheets files.

OpenRefine will generate a list of all Sheets it finds, with the most recently modified Sheets at the top. If a file you’ve just added isn’t showing in this list, you can close and restart OpenRefine, or simply navigate to an existing project, open it, then head back to the <span class="menuItems">Create Project</span> window and check again. 

When you click <span class="menuItems">Preview</span> the Sheet will open in a new browser tab. When you click the Sheet title, OpenRefine will begin to process the data.


## Project preview

Once OpenRefine is ready to import the data, you will see a screen with <span class="menuItems">Configure Parsing Options</span> at the top. You’ll see a preview of the first 100 rows and all identified columns. 

At the bottom of the screen you will find options for telling OpenRefine how to process what it has found. You can tell it which row(s) to parse as column headers, as well as to ignore any number of rows at the top. You can also select a specific range of rows to work with, by discarding some rows at the top (excluding the header) and limiting the total number of rows it loads.

OpenRefine tries to guess how to parse your data based on the file extension. For example, `.xml` files are going to be parsed as though they are formatted in XML. An unknown file extension (or your clipboard copy-paste) is assumed to be either tab-separated or comma-separated. OpenRefine looks for a tab character; if one is found, it assumes you have imported tab-separated data.

If OpenRefine isn’t certain what format you imported, it will provide a list of possibilities under <span class="menuItems">Parse data as</span> and some settings. You can specify a custom separator now, or split columns later on in the project interface. 

If you imported a spreadsheet with multiple worksheets, they will be listed along with the number of rows they contain. You can only select data from one worksheet. 

Note that OpenRefine does not preserve any formatting, such as cell or text colour, that my have been in the original data file. 

You should create a project name at this stage. You can also supply tags to keep your projects organized. When you’re happy with the preview, click <span class="menuItems">Create Project</span>.


## Import a project

Because OpenRefine only runs locally on your computer, you can’t have a project accessible to more than one person at the same time. 

The best way to collaborate with another person is to export and import projects that save all your changes, so that you can pick up where someone else left off. You can also [export projects](exporting) and import them to new computers of your own, such as for working on the same project from the office and from home. 

An exported project will include all of the [history](running#history-undoredo), so you can see (and undo) all the changes from the previous user. It is essentially a point-in-time snapshot of their work. OpenRefine only exports projects as `.tar.gz` files at this time. 

### Instructions

Once someone has sent you a project archive file from their computer, you can save it anywhere, including your Downloads folder. 

In the left-hand menu of the home screen, click <span class="menuItems">Import Project</span>. Click <span class="menuItems">Browse…</span> and navigate to wherever you saved the file you were sent (for example, your Downloads folder). 

You can rename the project if you’d like - we recommend adding your name, a date, or a version number, if you’re planning to continue collaborating with another person (or working from multiple computers).

Then, click <span class="menuItems">Import Project</span>.  Your project should appear with a step count beside <span class="menuItems">Undo/Redo</span> if steps were saved by the exporter. 

OpenRefine will store the project in its own workspace directory, so you can now delete the original file that was sent to you. 


## Project management

You can access all of your created projects by clicking on <span class="menuItems">Open Project</span>. Your project list can be organized by modification date, title, row count, and other metadata you can supply (such as subject, descripton, tags, or creator). To edit the fields you see here, click <span class="menuItems">About</span> to the left of each project. There you can edit a number of available fields. You can also see the project ID that corresponds to the name of the folder in your work directory.  


### Naming projects 

You may have multiple projects from the same dataset, or multiple versions from sharing a project with another person. OpenRefine automatically generates a project name from the imported file, or <span class="menuItems">clipboard</span> when you use Clipboard importing. Project names don’t have to be unique, so OpenRefine will create many projects with the same name unless you intervene. 

You can name a project when you create it or import it, and you can rename a project by opening it and clicking on the project name at the top of the screen. 

### Autosaving 

OpenRefine saves all of your actions (everything you can see in the <span class="menuItems">Undo/Redo</span> panel). That includes flagging and starring rows.

It doesn’t, however, save your facets, filters, or any kind of view you may have in place while you work. This includes the number of rows showing, whether you are showing your data as rows or records, and any sorting or column collapsing you may have done. A good rule of thumb is: if it’s not showing in <span class="menuItems">Undo/Redo</span>, you will lose it when you leave the project workspace. 

You can only save and share facets and filters, not any other type of view. To save current facets and filters, click <span class="menuItems">Permalink</span>. The project will reload with a different URL, which you can then copy and save elsewhere. This permalink will save both the facets and filters you’ve set, and the settings for each one (such as sorting by count rather than by name). 

### Deleting projects

You can delete projects, which will erase the project files from the work directory on your computer. This is immediate and cannot be undone.

Go to <span class="menuItems">Open Project</span> and find the project you want to delete. Click on the <span class="menuItems">X</span> to the left of the project name. There will be a confirmation dialog. 

### Project files

You can find all of your raw project files in your work directory. They will be named according to the unique Project ID that OpenRefine has assigned them, which you can find on the <span class="menuItems">Open Project</span> screen, under the “About” link for each project. 