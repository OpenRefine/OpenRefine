---
id: createaproject
title: Create a project by importing data
sidebar_label: Create a project by importing data
---

When you start OpenRefine, you’ll be taken to the `Create Project` screen. You’ll see on the left side of the screen that your options are to: 



*   import data from a file on your computer
*   import data from a link to the web
*   import data by pasting in text from your clipboard
*   import data from a database (using SQL), and
*   import data from Google Drive. 

From these sources, you can load any of the following file formats:



*   CSV, TSV
*   Text files
*   Fixed width columns
*   JSON
*   XML
*   ODF spreadsheet (ODS)
*   Excel (XLS)
*   PC-Axis
*   MARC
*   RDF data (JSON-LD, N3, N-Triples, Turtle, RDF/XML)
*   Wikitext

More formats can be imported by adding extensions that provide that functionality. 

For whichever method you choose, when you click `Next >>` you will be given a preview and a chance to configure the way OpenRefine interprets the file.  


### Methods to import data


#### Get data from this computer

Click on `Browse…` and select a file on your hard drive. All files will be shown, not just compatible ones. 

If you import an archive file (something with the extension `.zip`, `.tar.gz`, `.tgz`, `.tar.bz2`, `.gz`, or `.bz2`), OpenRefine detects the most common file extension in it and loads all files with that extension into a single project.


#### Web Addresses (URLs)

Type or paste the URL to the data file into the field provided. You can add as many fields as you want. OpenRefine will ____(something I haven’t quite figured out yet)_____________________. 

Do not use this form to load a Google Sheet by its link; use the field on the Google Data form instead. 


#### Clipboard

You can copy and paste in data from anywhere. OpenRefine will recognize comma-separated, tab-separated, or table-formatted information copied from sources such as word-processing documents, spreadsheets, and tables in PDFs. 

This can be useful if you want to pre-select a specific number of rows from your source data, or paste together rows from different places, rather than delete unwanted rows later in the project interace. 


#### Database (SQL)

You may want to pull the latest dataset directly from an online database. This could include an online catalogue, a Wordpress or similar content management system, or a [digital repository or collection management system](https://bits.ashleyblewer.com/blog/2017/08/09/collection-management-system-collection/).

OpenRefine can connect to PostgreSQL, MySQL, MariaDB, and SQLite database systems. It will automatically populate the “Port” field based on which of these you choose, but you can manually edit this if needed.

To import data directly from a database online, you will need to do two things:


*   Add OpenRefine (running from your computer) to an account authorized to access your database
*   Set up OpenRefine to access that database using that account 


![A screenshot of the New Connection Editor](img/databaseconnect.jpg "The New Connection Editor")


Log in to your hosting provider. Get the database type (such as MySQL), database name, and the URL (either an IP address, such as `127.0.0.1`, or the domain that uses the database, such as **blog.openrefine.org**). Then look at the accounts authorized for access. You may wish to create a new account just for OpenRefine, or add OpenRefine to an existing account. 

Each host will have a slightly different method, but generally speaking: look for “accounts with access” to the database you wish to authorize, and within the settings for that account, look for “allowable hosts” or “access hosts.” 

In that list, add the IP address of your own computer, because that is where the OpenRefine access request will be coming from. You can find this easily, by clicking “Test” within OpenRefine once the rest of the information is filled out: OpenRefine will give you an error that looks like 

``` error:Access denied for user 'yourusername'@'123-45-67-89.yourISP.com' ```

Take your IP address from this error message and put that into the “allowable hosts” field on the account you’re trying to use. Add a wildcard to the end of your IP address (“123-45-67-89%”). Save that setting, and then test the connection again with OpenRefine.

You can either connect just once and gather data, or save the connection to use it again later. If you press “Connect” without saving, OpenRefine will forget all the information you just entered. 

If you’d like to save the connection, name your connection in a way you will recognize later. Click “Save” and it will appear in the “Saved Connections” list on the left. Now, click on the “...” ellipsis to the right of the connection you’ve saved, and click “Connect.” 

If your connection is successful, you will see a Query Editor. From here you can write an [SQL query](https://www.w3schools.com/sql/) to pull the specific data you need. 

If you need help, you may be able to find instructions from your hosting provider. Here are the guides from:



*   [Dreamhost](https://help.dreamhost.com/hc/en-us/articles/214883058-How-do-I-connect-to-my-database-using-a-third-party-program-)
*   [GoDaddy](https://ca.godaddy.com/help/connect-remotely-to-a-mysql-database-in-my-linux-hosting-account-16103)


#### Google Data

You have two ways to load in data from Google Sheets:



*   A link to an accessible Google Sheet (that is, one with link-sharing turned on)
*   Selecting a Google Sheet in your Google Drive


##### Google Sheet by URL

You can import data from any Google Sheet that has link-sharing turned on. Paste in a URL that looks something like

```https://docs.google.com/spreadsheets/……….../edit?usp=sharing```

This will only work with Sheets, not with any other Google Drive file that might have an available link. 


##### Google Sheet from Drive

You can authorize OpenRefine to access your Google Drive data and import data from any Google Sheet it finds there. This will include Sheets that belong to you and Sheets that are shared with you, as well as Sheets that are in your trash. 

OpenRefine will not show spreadsheets that are in your email inbox or stored in any other Google property - only in Drive. It also won’t show all compatible file formats, only Sheets files.

OpenRefine will generate a list of all Sheets it finds, with the most recently modified Sheets at the top. If a file you’ve just added isn’t showing in this list, you can close and restart OpenRefine, or simply navigate to an existing project, open it, then head back to the “Create Project” window and check again. 

When you click “Preview” the Sheet will open in a new browser tab. When you click the Sheet title, OpenRefine will begin to process the data.


### Previewing the project

Once OpenRefine is ready to import the data, you will see a screen with “Configure Parsing Options” at the top. You’ll see a preview of the first 100 rows and all identified columns. 

At the bottom of the screen you will find options for telling OpenRefine how to process what it has found. You can tell it which row(s) to parse as column headers, as well as to ignore any number of rows at the top. You can also select a specific range of rows to work with, by discarding some rows at the top (excluding the header) and limiting the total number of rows it loads.

OpenRefine tries to guess how to parse your data based on the file extension. For example, .xml files are going to be parsed as though they are formatted in XML. An unknown file extension (or your clipboard copy-paste) is assumed to be either tab-separated or comma-separated. OpenRefine looks for a tab character; if one is found, it assumes you have imported tab-separated data.

If Google isn’t certain what format you imported, it will provide a list of possibilities under “Parse data as” and some settings. You can specify a custom separator now, or split columns later on in the project interface. 

If you imported a spreadsheet with multiple worksheets, they will be listed along with the number of rows they contain. You can only select data from one worksheet. 

You should create a project name at this stage. You can also supply tags to keep your projects organized. When you’re happy with the preview, click “Create Project.”