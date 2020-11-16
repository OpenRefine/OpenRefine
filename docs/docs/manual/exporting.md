---
id: exporting
title: Exporting your work
sidebar_label: Exporting
---

## Overview

Once your data is cleaned, you will need to get it out of OpenRefine and into the system of your choice. OpenRefine outputs a number of file formats, can upload your data directly into Google Sheets, and can export directly to Wikidata. 

You can also export your full project data so that it can be opened by someone else using OpenRefine (or yourself, on another computer).

## Export data

Note you will only export data in the current view - that is, with current filters and facets applied. 

To export from a project, click the Export button at the top right corner and pick the format you want. You options are:

*   Tab-separated value (TSV) or Comma-separated value (CSV)
*   HTML-formatted table
*   Excel (XLS or XLSX)
*   ODF spreadsheet
*   Upload to Google Sheets (requires Google account authorization)
*   [Custom tabular exporter](#custom-tabular-exporter)
*   SQL statement exporter: creates [a table containing the data you’ve exported, which you can use to overwrite an existing database](https://blog.ouseful.info/2018/06/19/quick-notes-openrefine-working-with-databases/)
*   [Templating exporter](#templating-exporter)

You can also [export reconciled data to Wikidata](wikidata#editing-wikidata-with-openrefine), or save your schema for future use with others OpenRefine projects:

*   Upload edits to Wikidata
*   Export to QuickStatement
*   Export Wikidata schema

### Custom tabular exporter

![A screenshot of the custom tabular content tab.](/img/custom-tabular-exporter.png)

With the custom tabular exporter, you can choose which of your data to export, the separator you wish to use, and whether you'd like to download it to your computer or upload it into a Google Sheet. This exporter is especially useful with reconciled data, as you can choose whether you wish to output the cells' original values, the matched values, or the matched IDs. 

You can also choose how to output date-formatted cells, and whether you wish to export all the project's data or just the current view set. 

![A screenshot of the custom tabular file download tab.](/img/custom-tabular-exporter2.png)

With the "Option Code" tab, you can copy JSON data that saves your current settings, to reuse on another project, or you can paste in existing JSON settings data to apply to the current project. 


### Templating exporter

If you pick <span class="menuItems">Templating…</span> from the <span class="menuItems">Export</span> dropdown menu, you can “roll your own” exporter. This is useful for formats that we don't support natively yet, or won't support. For example, the Templating exporter generates JSON by default. 

The window that appears allows you to set your own separators, prefix, and suffix to create a complete dataset in the language of your choice. In the Row Template section, you can choose which columns to generate from each row by calling them with variables. 

![A screenshot of the Templating exporter generating JSON by default.](/img/templating-exporter.png)

Once you have created your template, you may wish to save the text you produced for each field elsewhere, in order to reuse it in the future. Once you click "Export" OpenRefine will output a simple text file, and your template will be discarded.

We have a tutorial on using this approach to [export your data as YAML](https://github.com/OpenRefine/OpenRefine/wiki/Export-As-YAML).

## Export a project

You can share a project in progress with another computer, a colleague, or with someone who wants to check your history. This can be useful for showing your data cleanup didn’t distort or manipulate the information in any way. Once you have exported a project, the other OpenRefine installation can [import it as a new project](starting#import-a-project). 

From the <span class="menuItems">Export</span> dropdown, select <span class="menuItems">OpenRefine project archive to file</span>. OpenRefine exports your full project with all of its history. It does not export any current views or applied facets. Any reconciliation information will be preserved, but the importing installation will need to add the same reconciliation services to keep working with that data. 

OpenRefine exports files in `.tar.gz` format. You can rename the file when you save it; otherwise it will bear the project name. You can either save it locally or upload it to Google Drive (which requires you to authorize a Google account), using the <span class="menuItems">OpenRefine project archive to Google Drive...</span> option. OpenRefine will not share the link with you, only confirm that the file was uploaded.