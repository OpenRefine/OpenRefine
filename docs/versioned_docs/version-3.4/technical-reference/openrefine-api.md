---
id: openrefine-api
title: OpenRefine API
sidebar_label: OpenRefine API
---

This is a generic API reference for interacting with OpenRefine's HTTP API.

## Create project: {#create-project}

> **Command:** _POST /command/core/create-project-from-upload_

When uploading files you will need to send the data as `multipart/form-data`. This is different to all other API calls which use a mixture of query string and POST parameters.

multipart form-data:

      'project-file' : file contents
      'project-name' : project name
      'format' : format of data in project-file (e.g. 'text/line-based/*sv') [optional]
      'options' : json object containing options relevant to the file format [optional - however, some importers may have required options, such as `recordPath` for the JSON & XML importers].

The formats supported will depend on the version of OpenRefine you are using and any Extensions you have installed. The common formats include:

* 'text/line-based': Line-based text files
* 'text/line-based/*sv': CSV / TSV / separator-based files [separator to be used in specified in the json submitted to the options parameter]
* 'text/line-based/fixed-width': Fixed-width field text files
* 'binary/text/xml/xls/xlsx': Excel files
* 'text/json': JSON files
* 'text/xml': XML files

If the format is omitted OpenRefine will try to guess the format based on the file extension and MIME type.
The values which can be specified in the JSON object submitted to the 'options' parameter will vary depending on the format being imported. If not specified the options will either be guessed at by OpenRefine (e.g. separator being used in a separated values file) or a default value used. The import options for each file format are not currently documented, but can be seen in the OpenRefine GUI interface when importing a file of the relevant format.

If the project creation is successful, you will be redirected to a URL of the form:

      http://127.0.0.1:3333/project?project=<project id>

From the project parameter you can extract the project id for use in future API calls. The content of the response is the HTML for the OpenRefine interface for viewing the project.

### Get project models: {#get-project-models}
    
> **Command:** _GET /command/core/get-models?_

      'project' : project id

Recovers the models for the specific project. This includes  columns, records, overlay models, scripting. In the columnModel a list of the columns is displayed, key index and name, and column groupings.
    
### Response: {#response}
**On success:**
```JSON
{  
   "columnModel":{  
      "columns":[  
         {  
            "cellIndex":0,
            "originalName":"email",
            "name":"email"
         },
         {  
            "cellIndex":1,
            "originalName":"name",
            "name":"name"
         },
         {  
            "cellIndex":2,
            "originalName":"state",
            "name":"state"
         },
         {  
            "cellIndex":3,
            "originalName":"gender",
            "name":"gender"
         },
         {  
            "cellIndex":4,
            "originalName":"purchase",
            "name":"purchase"
         }
      ],
      "keyCellIndex":0,
      "keyColumnName":"email",
      "columnGroups":[  

      ]
   },
   "recordModel":{  
      "hasRecords":false
   },
   "overlayModels":{  

   },
   "scripting":{  
      "grel":{  
         "name":"General Refine Expression Language (GREL)",
         "defaultExpression":"value"
      },
      "jython":{  
         "name":"Python / Jython",
         "defaultExpression":"return value"
      },
      "clojure":{  
         "name":"Clojure",
         "defaultExpression":"value"
      }
   }
}
```



## Apply operations {#apply-operations}

> **Command:** _POST /command/core/apply-operations?_

In the parameter

      'project' : project id

In the form data

      'operations' : Valid JSON **Array** of OpenRefine operations

Example of a Valid JSON **Array**
```JSON
'[  
   {  
      "op":"core/column-addition",
      "description":"Create column zip type at index 15 based on column Zip Code 2 using expression grel:value.type()",
      "engineConfig":{  
         "mode":"row-based",
         "facets":[]
      },
      "newColumnName":"zip type",
      "columnInsertIndex":15,
      "baseColumnName":"Zip Code 2",
      "expression":"grel:value.type()",
      "onError":"set-to-blank"
   },
   {  
      "op":"core/column-addition",
      "description":"Create column testing at index 15 based on column Zip Code 2 using expression grel:value.toString()0,5]",
      "engineConfig":{  
         "mode":"row-based",
         "facets":[]
      },
      "newColumnName":"testing",
      "columnInsertIndex":15,
      "baseColumnName":"Zip Code 2",
      "expression":"grel:value.toString()[0,5]",
      "onError":"set-to-blank"
   }
]
```

On success returns JSON response
`{ "code" : "ok" }`
    
## Export rows {#export-rows}

> **Command:** _POST /command/core/export-rows_

In the parameter

      'project' : project id      
      'format' : format... (e.g 'tsv', 'csv')

In the form data

      'engine' : JSON string... (e.g. '{"facets":[],"mode":"row-based"}')
      
Returns exported row data in the specified format. The formats supported will depend on the version of OpenRefine you are using and any Extensions you have installed. The common formats include:

* csv
* tsv
* xls
* xlsx
* ods
* html
    
## Delete project {#delete-project}

> **Command:** _POST /command/core/delete-project_

      'project' : project id...
      
Returns JSON response
    
## Check status of async processes {#check-status-of-async-processes}

> **Command:** _GET /command/core/get-processes_

      'project' : project id...
      
Returns JSON response

## Get all projects metadata: {#get-all-projects-metadata}
    
> **Command:** _GET /command/core/get-all-project-metadata_

Recovers the meta data for all projects. This includes the project's id, name, time of creation and last time of modification.
    
### Response: {#response-1}
```json
{
    "projects":{
        "[project_id]":{
            "name":"[project_name]",
            "created":"[project_creation_time]",
            "modified":"[project_modification_time]"
        },
        ...[More projects]...
    }
}
```
    
## Expression Preview {#expression-preview}
> **Command:** _POST /command/core/preview-expression_

Pass some expression (GREL or otherwise) to the server where it will be executed on selected columns and the result returned.

### Parameters: {#parameters}
* **cellIndex**: _[column]_  
The cell/column you wish to execute the expression on.
* **rowIndices**: _[rows]_
The rows to execute the expression on as JSON array. Example: `[0,1]`
* **expression**: _[language]_:_[expression]_  
The expression to execute. The language can either be grel, jython or clojure. Example: grel:value.toLowercase()
* **project**: _[project_id]_  
The project id to execute the expression on.
* **repeat**: _[repeat]_  
A boolean value (true/false) indicating whether or not this command should be repeated multiple times. A repeated command will be executed until the result of the current iteration equals the result of the previous iteration.
* **repeatCount**: _[repeatCount]_  
The maximum amount of times a command will be repeated.

### Response: {#response-2}
**On success:**
```json
{
  "code": "ok",
  "results" : [result_array]
}
```

The result array will hold up to ten results, depending on how many rows there are in the project that was specified by the [project_id] parameter. Each result is the string that would be put in the cell if the GREL command was executed on that cell. Note that any expression that would return an array or JSon object will be jsonized, although the output can differ slightly from the jsonize() function.

**On error:**
```JSON
{
  "code": "error",
  "type": "[error_type]",
  "message": "[error message]"
}
```

## Third-party software libraries {#third-party-software-libraries}
Libraries using the [OpenRefine API](openrefine-api):

### Python {#python}
* [refine-client-py](https://github.com/PaulMakepeace/refine-client-py/)
  * Or this fork of the above with an extended CLI [openrefine-client](https://github.com/felixlohmeier/openrefine-client)
* [refine-python](https://github.com/maxogden/refine-python)

### Ruby {#ruby}
* [refine-ruby](https://github.com/distillytics/refine-ruby)
  * The above is a maintained fork of [google-refine](https://github.com/maxogden/refine-ruby)
* [google_refine](https://github.com/chengguangnan/google_refine)

### NodeJS {#nodejs}
* [node-openrefine](https://github.com/pm5/node-openrefine)

### R {#r}
* [rrefine](https://cran.r-project.org/web/packages/rrefine/index.html)

### PHP {#php}
* [openrefine-php-client](https://github.com/keboola/openrefine-php-client)

### Java {#java}
* [refine-java](https://github.com/dtap-gmbh/refine-java)

### Bash {#bash}
* [bash-refine.sh](https://gist.github.com/felixlohmeier/d76bd27fbc4b8ab6d683822cdf61f81d) (templates for shell scripts)
