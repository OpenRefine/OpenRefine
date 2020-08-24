---
id: columnediting
title: Column editing
sidebar_label: Column editing
---

## Overview

Column editing contains some of the most powerful data-improvement methods in OpenRefine. While we call it “edit column,” in fact many of the best features involve using one column of data to add entirely new columns and fields to your dataset. 

## Split or Join

Many users find that they frequently need to make their data more granular: for example, splitting a “Firstname Lastname” column into two columns, one for first names and one for last names. You may want to split out an address column into columns for street addresses, cities, territories, and postal codes. 

The reverse is also often true: you may have several columns of category values that you want to join into one “category” column. 

### Split into several columns...

![A screenshot of the settings window for splitting columns.](/img/columnsplit.png)

Splitting one column into several columns requires you to identify the character, string lengths, or evaluating expression you want to split on. Just like [splitting multi-valued cells into rows](cellediting#split-multi-valued-cells), splitting cells into multiple columns will remove the separator character or string you indicate. Lengths will discard any information that comes after the specified total length. 

You can also specify a maximum number of new columns to be made: separator characters after this limit will be ignored, and the remaining characters will end up in the last column.

New columns will be named after the original column, with a number: “Location 1,” “Location 2,” etc. You can have the original column removed with this operation, and you can have [data types](exploring#data-types) identified where possible. This function will work best with converting to numbers, and may not work with dates.

### Join columns…

![A screenshot of the settings window for joining columns.](/img/columnjoin.png)

You can join columns by selecting “Edit column” > “Join columns…”. All the columns currently in your dataset will appear in the pop-up window. You can select or un-select all the columns you want to join, and drag columns to put them in the order you want to join them in. You will define a separator character (optional) and define a string to insert into empty cells (nulls). 

The joined data will appear in the column you originally selected, or you can create a new column based on this join and specify a name. You can delete all the columns that were used in this join operation. 

## Add column based on this column

This selection will open up an [expressions](expressions) window where you can transform the data from this column (using `value`) or write a complex expression that takes information from any number of columns or from external reconciliation sources. 

One common use of this function is to join information from two projects together in one place. Just like referring from one table to another table in an entity-relationship database, you need a key that exists in both. 

The expression is

```cell.cross('arg1','arg2').cells['arg3'].value[arg4]```

where 

*   **arg1** is the name of the project you want to pull data from (you will get an error in the Preview window if there are multiple projects with the identified name)
*   **arg2** is the column in that project with matching values to the column in the current project
*   **arg3** is the column in that project you’d like to copy over (you can only specify one column at a time)
*   and **arg4** is which value in that column to import (most likely 0).

Learn more about [cross()](expressions#cross) and other GREL functions to use in this window on the [Expressions](expressions) page. 

Some of the other most common ways to add a new column based on an existing one are separate functions, and are explained below.


## Add column by fetching URLs

Through the "Add column by fetching URLs" function, OpenRefine supports the ability to fetch HTML or data from web pages or services. In this operation you will be taking strings from your selected column and inserting them into URL strings. This presumes your chosen column contains parts of paths to valid HTML pages or files online. 

If you have a column of URLs and watch to fetch the information that they point to, you can simply run the expression as `value`. If your column has, for example, unique identifiers for Wikidata entities (numerical values starting with Q), you can download the JSON-formatted metadata about each entity with

```“https://www.wikidata.org/wiki/Special:EntityData/” + value + “.json”```

or whatever metadata format you prefer. [Information about these Wikidata options can be found here](https://www.wikidata.org/wiki/Wikidata:Data_access).

This service is more useful when getting metadata files instead of HTML files, but you may wish to work with a page’s entire HTML contents and then parse out information from that. Be cautioned that the fetching process can take quite some time and that servers may not want to fulfill hundreds or thousands of page requests in seconds. 

Fetching allows you to set a “throttle delay” which determines the amount of time between requests. The default is 5 seconds per row in your dataset (5000 milliseconds), so you can estimate how long it will take to work through the entire column using that number. We recommend leaving this value at 5000 or greater. 

![A screenshot of the settings window for fetching URLs.](/img/fetchingURLs.png)

Note the following:

* Many systems prevent you from making too many requests per second. To avoid this problem, set the throttle delay, which tells OpenRefine to wait the specified number of milliseconds between URL requests.
* Before pressing OK, copy/paste a URL or two from the right column in the dialog and test them in another browser tab to make sure they work.
* In some situations you may need to set[ HTTP request headers](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers). To set these, click the small “Show” button next to "HTTP headers to be used when fetching URLs" in the settings window. You can set the following request headers:
  * [User-Agent](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent)
  * [Accept](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept)
  * [Authorization](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization)

### Common errors

When OpenRefine attempts to fetch information from a web page or service, it can fail in a variety of ways. The following information is meant to help troubleshoot and fix problems encountered when using this function.

First, make sure that your fetching operation is storing errors (check “store error”). The run the fetch and look at the error messages. 

**"Received fatal alert: handshake_failure" **can occur when you are trying to retrieve information over HTTPS but the remote site is using an encryption not supported by the Java virtual machine being used by OpenRefine.

You can check which encryption methods are supported by your OpenRefine/Java installation by using a service such as **How's my SSL**. Add the URL `https://www.howsmyssl.com/a/check` to an OpenRefine cell and run "Add column by fetching URLs" on it, which will provide a description of the SSL client being used. 

You can try installing additional encryption supports by installing the [Java Cryptography Extension](https://www.oracle.com/java/technologies/javase-jce8-downloads.html). Note on OpenRefine for Mac, these updated cipher suites need to be dropped into the Java install within the OpenRefine application: something like `/Applications/OpenRefine.app/Contents/PlugIns/jdk1.8.0_60.jdk/Contents/Home/jre/lib/security`.

**"sun.security.validator.ValidatorException: PKIX path building failed"**can occur when you try to retrieve information over HTTPS but the remote site is using a certificate not trusted by your local Java installation. You will need to make sure that the certificate, or (more likely) the root certificate, is trusted. 

The list of trusted certificates is stored in an encrypted file called `cacerts` in your local Java installation. This can be read and updated by a tool called “keytool.” You can find directions on how to add a security certificate to the list of trusted certificates for a Java installation [here](http://magicmonster.com/kb/prg/java/ssl/pkix_path_building_failed.html) and [here](http://javarevisited.blogspot.co.uk/2012/03/add-list-certficates-java-keystore.html).

Note on OpenRefine for Mac, you need to update the `cacerts` file within the OpenRefine application: something like `/PlugIns/jdk1.8.0_60.jdk/Contents/Home/jre/lib/security/cacerts`.

**"HTTP error 403 : Forbidden" **can be simply down to you not having access to the URL you are trying to use. If you can access the same URL with your browser, the remote site may be blocking OpenRefine because it doesn't recognize its request as valid. Changing the[ User-Agent](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent) request header may help. If you believe you should have access to a site but are “forbidden,” you may wish to contract the administrators.

**"HTTP error 404 : Not Found"** indicates that the information you are requesting does not exist, perhaps due to a problem with your cell values if it only happening in certain rows.** "HTTP error 500 : Internal Server Error"** indicates the remote server is having a problem filling your request. You may wish to simply wait and try again later, or double-check the URLs. 

## Add columns from reconciled values

The “Add columns from reconciled values…” feature operates on reconciled columns in OpenRefine - that is, columns containing data that has been through [reconciliation](#reconcile) processes. See that section for more details about how to get your data into a reconciled format. 

Once you have pulled reconciliation values and selected one for each cell, selecting “Add column from reconciled values” will bring up a window to choose which information you’d like to generate into a new column. If you have reconciled against Wikidata, for example, you will see the option of adding their unique identifiers as a new column (automatically named “Qid”). 

The quality of the suggested properties will depend on how you have reconciled your data beforehand: reconciling against a specific type will provide you with suggested properties of that type.

![A screenshot of the settings window for adding reconciled values.](/img/columnreconciled.png)

If you have left any values unreconciled in your column, you will see “&lt;not reconciled>” in the preview. These will generate blank cells if you continue with the column addition process. 

One reason to reconcile a dataset to some database is that it allows you to pull data from the database into OpenRefine. There are two ways to do this:

*   If the reconciliation service supports the[ Data Extension API](https://github.com/OpenRefine/OpenRefine/wiki/Data%20Extension%20API), then you can use the "Add columns from reconciled values" action to augment your OpenRefine project with new columns. For example, if you have a column of chemical elements identified by name, you can fetch categorical information about them such as their atomic number and their element symbol, as the animation shows below.

![A screenshare of elements fetching related information.](/img/reconcileelements.gif)

*   If the reconciliation service does not support this feature, look out for a generic web API for that data source. You can use the "Add column by fetching URLs" operation to call this API with the IDs obtained from the reconciliation process. For instance, if you have used the OpenCorporates service, you can use[ their API](https://api.opencorporates.com/documentation/API-Reference) to fetch JSON-formatted data about the reconciled companies. The GREL expression `"https://api.opencorporates.com/companies/"+cell.recon.match.id` will be translated to URLs that can then be parsed in OpenRefine.

## Rename, Remove, and Move

Any column can be repositioned, renamed, or deleted with these actions. They can be undone, but a removed column cannot be restored later if you keep modifying your data. If you wish to temporarily hide a column, go to “View” > “Collapse this column” instead. 

Be cautious about moving columns in [records mode](cellediting#rows-vs-records): if you change the first column in your dataset (the key column), your records may change in unintended ways. 
