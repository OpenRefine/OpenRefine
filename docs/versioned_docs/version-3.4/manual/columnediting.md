---
id: columnediting
title: Column editing
sidebar_label: Column editing
---

## Overview {#overview}

Column editing contains some of the most powerful data-improvement methods in OpenRefine. The operations in the <span class="menuItems">Edit column</span> menu involve using one column of data to add entirely new columns and fields to your dataset. 

## Splitting or joining {#splitting-or-joining}

Many users find that they frequently need to make their data more granular: for example, splitting a “Firstname Lastname” column into two columns, one for first names and one for last names. The reverse is also often true: you may have several columns of category values that you want to join into one “category” column. 
.
### Split into several columns {#split-into-several-columns}

![A screenshot of the settings window for splitting columns.](/img/columnsplit.png)

You can find this operation at <span class="menuItems">Edit column</span> → <span class="menuItems">Split into several columns...</span>. Splitting one column into several columns requires you to identify the character, string lengths, or evaluating expression you want to split on. Just like [splitting multi-valued cells into rows](cellediting#split-multi-valued-cells), splitting cells into multiple columns will remove the separator character or string you indicate. Splitting by lengths will discard any information that comes after the specified total length. 

You can also specify a maximum number of new columns to be made: separator characters after this limit will be ignored, and the remaining characters will end up in the last column.

New columns will be named after the original column, with a number: “Location 1,” “Location 2,” etc. You can choose to remove the original column with this operation, and you can have [data types](exploring#data-types) identified where possible. This function will work best with converting strings to numbers, and may not work with [dates](exploring#dates).

### Join columns {#join-columns}

![A screenshot of the settings window for joining columns.](/img/columnjoin.png)

You can join columns by selecting <span class="menuItems">Edit column</span> → <span class="menuItems">Join columns...</span>. All the columns currently in your dataset will appear in the pop-up window. You can select or un-select all the columns you want to join, and drag columns to put them in the order you want to join them in. You will define a separator character (optional) and define a string to insert into empty cells (nulls). 

The joined data will appear in the column you originally selected, or you can create a new column for this content and specify a name. You can delete all the columns that were used in this join operation. 

## Add column based on this column {#add-column-based-on-this-column}

Selecting <span class="menuItems">Edit column</span> → <span class="menuItems">Add column based on this column...</span> will open up an [expressions](expressions) window where you can transform the data from this column (using `value`), or write a more complex expression that takes information from any number of columns or from external sources. 

Expressions used in this operation will rely on your knowledge of variables. You can learn more in the [Expressions section on variables](expressions#variables).

The simplest way to use this operation is simply leave the default `value` in the expression field, to create an exact copy of your column. For a column of [reconciled data](reconciling), you can use the variable `cell` instead, to copy both the original string and the existing reconciliation data. This will include matched values, candidates, and new items. 

One useful expression is to create a column based on concatenating (merging) two other columns. Select either of the source columns, choose <span class="menuItems">Edit column</span> → <span class="menuItems">Add column based on this column...</span>, name your new column, and use the following format in the expression window:

```
cells["Column 1"].value + cells["Column 2"].value
```

If your column names do not contain spaces, you can use the following format instead:

```
cells.Column1.value + cells.Column2.value
```

If you are in records mode instead of rows mode, you can concatenate using the following format:

```
row.record.cells.Column1.value + row.record.cells.Column2.value
```

You may wish to add separators or spaces, or modify your input during this operation with more advanced expressions. 

## Add column by fetching URLs {#add-column-by-fetching-urls}

Through the <span class="menuItems">Add column by fetching URLs</span> function, OpenRefine supports the ability to fetch HTML or data from web pages or services. In this operation you will be building URL strings based on your column of data, by using `value` to insert a relevant substring. Your chosen column needs to contains parts of paths to valid HTML pages or files online. 

If you have a column of URLs and want to fetch the information that they point to, you can simply run the expression as `value`. If your column has, for example, unique identifiers for Wikidata entities (numerical values starting with Q), you can download the JSON-formatted metadata about each entity with

```
"https://www.wikidata.org/wiki/Special:EntityData/" + value + ".json"
```

or whatever metadata format you prefer. Information about the format options in Wikidata can be found [here](https://www.wikidata.org/wiki/Wikidata:Data_access). The service you are fetching data from may have similar documentation on its provided options.

![A screenshot of the settings window for fetching URLs.](/img/fetchingURLs.png)

This service is more useful when getting metadata files instead of HTML, but you may wish to work with a page’s entire HTML contents and then parse out information from that. 

:::caution
Be aware that the fetching process can take quite some time and that servers may not want to fulfill hundreds or thousands of page requests in seconds. Fetching allows you to set a “throttle delay” which determines the amount of time between requests. The default is 5 seconds per row in your dataset (5000 milliseconds). We recommend leaving this at 1000 or greater. 
:::

Note the following:
* Before pressing “OK,” copy and paste a URL or two from the preview and test them in another browser tab to make sure they work.
* In some situations you may need to set [HTTP request headers](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers). To set these, click the small “Show” button next to “HTTP headers to be used when fetching URLs” in the settings window. The authorization credentials get logged in your operation history in plain text, which may be a security concern for you. You can set the following request headers:
  * [User-Agent](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent)
  * [Accept](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept)
  * [Authorization](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization)

### Common errors {#common-errors}

When OpenRefine attempts to fetch information from a web service, it can fail in a variety of ways. The following information is meant to help troubleshoot and fix problems encountered when using this function.

First, make sure that your fetching operation is storing errors (check “store error”). Then run the fetch and look at the error messages. 

**“HTTP error 403 : Forbidden”** can be simply down to you not having access to the URL you are trying to use. If you can access the same URL with your browser, the remote site may be blocking OpenRefine because it doesn't recognize its request as valid. Changing the [User-Agent](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent) request header may help. If you believe you should have access to a site but are “forbidden,” you may wish to contract the administrators.

**“HTTP error 404 : Not Found”** indicates that the information you are requesting does not exist, perhaps due to a problem with your cell values if it only happening in certain rows. 

**“HTTP error 500 : Internal Server Error”** indicates the remote server is having a problem filling your request. You may wish to simply wait and try again later, or double-check the URLs. 

**“error: javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure”** can occur when you are trying to retrieve information over HTTPS but the remote site is using an encryption not supported by the Java virtual machine being used by OpenRefine.

You can check which encryption methods are supported by your OpenRefine/Java installation by using a service such as **How's my SSL**. Add the URL `https://www.howsmyssl.com/a/check` to an OpenRefine cell and run “Add column by fetching URLs” on it, which will provide a description of the SSL client being used. 

You can try installing additional encryption supports by installing the [Java Cryptography Extension](https://www.oracle.com/java/technologies/javase-jce8-downloads.html). 
Note that for Mac users and for Windows users with the OpenRefine installation with bundled JRE, these updated cipher suites need to be dropped into the Java install within the OpenRefine application: 

* On Mac, it will look something like `/Applications/OpenRefine.app/Contents/PlugIns/jdk1.8.0_60.jdk/Contents/Home/jre/lib/security`. 
* On Windows: `\server\target\jre\lib\security`.

**“javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed”** can appear when the remote site is using an HTTPS certificate not trusted by your local Java installation. You will need to make sure that the certificate, or (more likely) the root certificate, is trusted. 

The list of trusted certificates is stored in an encrypted file called `cacerts` in your local Java installation. This can be read and updated by a tool called “keytool.” You can find directions on how to add a security certificate to the list of trusted certificates for a Java installation [here](http://magicmonster.com/kb/prg/java/ssl/pkix_path_building_failed.html) and [here](http://javarevisited.blogspot.co.uk/2012/03/add-list-certficates-java-keystore.html).

Note that for Mac users and for Windows users with the OpenRefine installation with bundled JRE, the `cacerts` file within the OpenRefine application needs to be updated. 

* On Mac, it will look something like `/Applications/OpenRefine.app/Contents/PlugIns/jdk1.8.0_60.jdk/Contents/Home/jre/lib/security/cacerts`.
* On Windows: `\server\target\jre\lib\security\`.

## Renaming, removing, and moving {#renaming-removing-and-moving}

Every column's <span class="menuItems">Edit column</span> dropdown contains options to move it (to the beginning, end, left, or right), rename it, and delete it. 
These operations can be undone, but a removed column cannot be restored later if you keep modifying your data. If you wish to temporarily hide a column, go to <span class="menuItems">[View](sortview#view)</span> → <span class="menuItems">Collapse this column</span> instead. 

Be cautious about moving columns in [records mode](cellediting#rows-vs-records): if you change the first column in your dataset (the key column), your records may change in unintended ways. 
