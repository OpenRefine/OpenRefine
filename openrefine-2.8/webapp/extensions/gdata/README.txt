OpenRefine extension for Google Spreadsheets
by Tom Morris <tfmorris@gmail.com>

This extension provides Refine with the ability to read Google spreadsheets
directly so you don't have to export them as CSV files first.

Unpack the contents of this archive (jar, zip, etc) into the extensions folder
of your Refine installation and restart Refine.

Enter the *public* share URL of a Google Spreadsheet in the "Data File URL"
field of the "Create a New Project" pane of the Refine home page.

If you end up with a project full of HTML tags and an RSS feed, the extension
isn't installed properly.  A properly installed extension should have an About
page available at http://127.0.0.1:3333/extension/gdata/ (the trailing
slash is significant).


Known limitations:

- No write support
- No authentication support.  You must use the public share URL for the
  the Google spreadsheet you want to load.