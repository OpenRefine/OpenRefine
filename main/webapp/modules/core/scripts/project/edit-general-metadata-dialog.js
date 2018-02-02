

function EditGeneralMetadataDialog(projectId, callback) {
    this._projectId = projectId;
    this._callback = callback;
  this._createDialog();
}

EditGeneralMetadataDialog.prototype._createDialog = function() {
  var self = this;

  var schema = {
          "$schema": "http://json-schema.org/draft-06/schema#",
          "title": "Data Package",
          "description": "Data Package is a simple specification for data access and delivery.",
          "type": "object",
          "required": [
              "resources"
          ],
          "properties": {
              "profile": {
                  "default": "data-package",
                  "propertyOrder": 10,
                  "title": "Profile",
                  "description": "The profile of this descriptor.",
                  "context": "Every Package and Resource descriptor has a profile. The default profile, if none is declared, is `data-package` for Package and `data-resource` for Resource.",
                  "type": "string",
                  "examples": [
                      "{\n  \"profile\": \"tabular-data-package\"\n}\n",
                      "{\n  \"profile\": \"http://example.com/my-profiles-json-schema.json\"\n}\n"
                  ]
              },
              "name": {
                  "propertyOrder": 20,
                  "title": "Name",
                  "description": "An identifier string. Lower case characters with `.`, `_`, `-` and `/` are allowed.",
                  "type": "string",
                  "pattern": "^([-a-z0-9._/])+$",
                  "context": "This is ideally a url-usable and human-readable name. Name `SHOULD` be invariant, meaning it `SHOULD NOT` change when its parent descriptor is updated.",
                  "examples": [
                      "{\n  \"name\": \"my-nice-name\"\n}\n"
                  ]
              },
              "id": {
                  "propertyOrder": 30,
                  "title": "ID",
                  "description": "A property reserved for globally unique identifiers. Examples of identifiers that are unique include UUIDs and DOIs.",
                  "context": "A common usage pattern for Data Packages is as a packaging format within the bounds of a system or platform. In these cases, a unique identifier for a package is desired for common data handling workflows, such as updating an existing package. While at the level of the specification, global uniqueness cannot be validated, consumers using the `id` property `MUST` ensure identifiers are globally unique.",
                  "type": "string",
                  "examples": [
                      "{\n  \"id\": \"b03ec84-77fd-4270-813b-0c698943f7ce\"\n}\n",
                      "{\n  \"id\": \"http://dx.doi.org/10.1594/PANGAEA.726855\"\n}\n"
                  ]
              },
              "title": {
                  "propertyOrder": 40,
                  "title": "Title",
                  "description": "A human-readable title.",
                  "type": "string",
                  "examples": [
                      "{\n  \"title\": \"My Package Title\"\n}\n"
                  ]
              },
              "description": {
                  "propertyOrder": 50,
                  "title": "Description",
                  "description": "A text description. Markdown is encouraged.",
                  "type": "string",
                  "examples": [
                      "{\n  \"description\": \"# My Package description\\nAll about my package.\"\n}\n"
                  ]
              },
              "homepage": {
                  "propertyOrder": 60,
                  "title": "Home Page",
                  "description": "The home on the web that is related to this data package.",
                  "type": "string",
                  "format": "uri",
                  "examples": [
                      "{\n  \"homepage\": \"http://example.com/\"\n}\n"
                  ]
              },
              "created": {
                  "propertyOrder": 70,
                  "title": "Created",
                  "description": "The datetime on which this descriptor was created.",
                  "context": "The datetime must conform to the string formats for datetime as described in [RFC3339](https://tools.ietf.org/html/rfc3339#section-5.6)",
                  "type": "string",
                  "format": "date-time",
                  "examples": [
                      "{\n  \"created\": \"1985-04-12T23:20:50.52Z\"\n}\n"
                  ]
              },
              "contributors": {
                  "propertyOrder": 80,
                  "title": "Contributors",
                  "description": "The contributors to this descriptor.",
                  "type": "array",
                  "minItems": 1,
                  "items": {
                      "title": "Contributor",
                      "description": "A contributor to this descriptor.",
                      "properties": {
                          "title": {
                              "title": "Title",
                              "description": "A human-readable title.",
                              "type": "string",
                              "examples": [
                                  "{\n  \"title\": \"My Package Title\"\n}\n"
                              ]
                          },
                          "path": {
                              "title": "Path",
                              "description": "A fully qualified URL, or a POSIX file path..",
                              "type": "string",
                              "examples": [
                                  "{\n  \"path\": \"file.csv\"\n}\n",
                                  "{\n  \"path\": \"http://example.com/file.csv\"\n}\n"
                              ],
                              "context": "Implementations need to negotiate the type of path provided, and dereference the data accordingly."
                          },
                          "email": {
                              "title": "Email",
                              "description": "An email address.",
                              "type": "string",
                              "format": "email",
                              "examples": [
                                  "{\n  \"email\": \"example@example.com\"\n}\n"
                              ]
                          },
                          "organisation": {
                              "title": "Organization",
                              "description": "An organizational affiliation for this contributor.",
                              "type": "string"
                          },
                          "role": {
                              "type": "string",
                              "enum": [
                                  "publisher",
                                  "author",
                                  "maintainer",
                                  "wrangler",
                                  "contributor"
                              ],
                              "default": "contributor"
                          }
                      },
                      "required": [
                          "title"
                      ],
                      "context": "Use of this property does not imply that the person was the original creator of, or a contributor to, the data in the descriptor, but refers to the composition of the descriptor itself."
                  },
                  "examples": [
                      "{\n  \"contributors\": [\n    {\n      \"title\": \"Joe Bloggs\"\n    }\n  ]\n}\n",
                      "{\n  \"contributors\": [\n    {\n      \"title\": \"Joe Bloggs\",\n      \"email\": \"joe@example.com\",\n      \"role\": \"author\"\n    }\n  ]\n}\n"
                  ]
              },
              "keywords": {
                  "propertyOrder": 90,
                  "title": "Keywords",
                  "description": "A list of keywords that describe this package.",
                  "type": "array",
                  "minItems": 1,
                  "items": {
                      "type": "string"
                  },
                  "examples": [
                      "{\n  \"keywords\": [\n    \"data\",\n    \"fiscal\",\n    \"transparency\"\n  ]\n}\n"
                  ]
              },
              "image": {
                  "propertyOrder": 100,
                  "title": "Image",
                  "description": "A image to represent this package.",
                  "type": "string",
                  "examples": [
                      "{\n  \"image\": \"http://example.com/image.jpg\"\n}\n",
                      "{\n  \"image\": \"relative/to/image.jpg\"\n}\n"
                  ]
              },
              "licenses": {
                  "propertyOrder": 110,
                  "title": "Licenses",
                  "description": "The license(s) under which this package is published.",
                  "type": "array",
                  "minItems": 1,
                  "items": {
                      "title": "License",
                      "description": "A license for this descriptor.",
                      "type": "object",
                      "properties": {
                          "name": {
                              "title": "Open Definition license identifier",
                              "description": "MUST be an Open Definition license identifier, see http://licenses.opendefinition.org/",
                              "type": "string",
                              "pattern": "^([-a-zA-Z0-9._])+$"
                          },
                          "path": {
                              "title": "Path",
                              "description": "A fully qualified URL, or a POSIX file path..",
                              "type": "string",
                              "examples": [
                                  "{\n  \"path\": \"file.csv\"\n}\n",
                                  "{\n  \"path\": \"http://example.com/file.csv\"\n}\n"
                              ],
                              "context": "Implementations need to negotiate the type of path provided, and dereference the data accordingly."
                          },
                          "title": {
                              "title": "Title",
                              "description": "A human-readable title.",
                              "type": "string",
                              "examples": [
                                  "{\n  \"title\": \"My Package Title\"\n}\n"
                              ]
                          }
                      },
                      "context": "Use of this property does not imply that the person was the original creator of, or a contributor to, the data in the descriptor, but refers to the composition of the descriptor itself."
                  },
                  "context": "This property is not legally binding and does not guarantee that the package is licensed under the terms defined herein.",
                  "examples": [
                      "{\n  \"licenses\": [\n    {\n      \"name\": \"odc-pddl-1.0\",\n      \"uri\": \"http://opendatacommons.org/licenses/pddl/\"\n    }\n  ]\n}\n"
                  ]
              },
              "resources": {
                  "propertyOrder": 120,
                  "title": "Data Resources",
                  "description": "An `array` of Data Resource objects, each compliant with the [Data Resource](/data-resource/) specification.",
                  "type": "array",
                  "minItems": 1,
                  "items": {
                      "title": "Data Resource",
                      "description": "Data Resource.",
                      "type": "object",
                      "oneOf": [
                          {
                              "required": [
                                  "name",
                                  "data"
                              ]
                          },
                          {
                              "required": [
                                  "name",
                                  "path"
                              ]
                          }
                      ],
                      "properties": {
                          "profile": {
                              "propertyOrder": 10,
                              "default": "data-resource",
                              "title": "Profile",
                              "description": "The profile of this descriptor.",
                              "context": "Every Package and Resource descriptor has a profile. The default profile, if none is declared, is `data-package` for Package and `data-resource` for Resource.",
                              "type": "string",
                              "examples": [
                                  "{\n  \"profile\": \"tabular-data-package\"\n}\n",
                                  "{\n  \"profile\": \"http://example.com/my-profiles-json-schema.json\"\n}\n"
                              ]
                          },
                          "name": {
                              "propertyOrder": 20,
                              "title": "Name",
                              "description": "An identifier string. Lower case characters with `.`, `_`, `-` and `/` are allowed.",
                              "type": "string",
                              "pattern": "^([-a-z0-9._/])+$",
                              "context": "This is ideally a url-usable and human-readable name. Name `SHOULD` be invariant, meaning it `SHOULD NOT` change when its parent descriptor is updated.",
                              "examples": [
                                  "{\n  \"name\": \"my-nice-name\"\n}\n"
                              ]
                          },
                          "path": {
                              "propertyOrder": 30,
                              "title": "Path",
                              "description": "A reference to the data for this resource, as either a path as a string, or an array of paths as strings. of valid URIs.",
                              "oneOf": [
                                  {
                                      "title": "Path",
                                      "description": "A fully qualified URL, or a POSIX file path..",
                                      "type": "string",
                                      "examples": [
                                          "{\n  \"path\": \"file.csv\"\n}\n",
                                          "{\n  \"path\": \"http://example.com/file.csv\"\n}\n"
                                      ],
                                      "context": "Implementations need to negotiate the type of path provided, and dereference the data accordingly."
                                  },
                                  {
                                      "type": "array",
                                      "minItems": 1,
                                      "items": {
                                          "title": "Path",
                                          "description": "A fully qualified URL, or a POSIX file path..",
                                          "type": "string",
                                          "examples": [
                                              "{\n  \"path\": \"file.csv\"\n}\n",
                                              "{\n  \"path\": \"http://example.com/file.csv\"\n}\n"
                                          ],
                                          "context": "Implementations need to negotiate the type of path provided, and dereference the data accordingly."
                                      },
                                      "examples": [
                                          "[ \"file.csv\" ]\n",
                                          "[ \"http://example.com/file.csv\" ]\n"
                                      ]
                                  }
                              ],
                              "context": "The dereferenced value of each referenced data source in `path` `MUST` be commensurate with a native, dereferenced representation of the data the resource describes. For example, in a *Tabular* Data Resource, this means that the dereferenced value of `path` `MUST` be an array.",
                              "examples": [
                                  "{\n  \"path\": [\n    \"file.csv\",\n    \"file2.csv\"\n  ]\n}\n",
                                  "{\n  \"path\": [\n    \"http://example.com/file.csv\",\n    \"http://example.com/file2.csv\"\n  ]\n}\n",
                                  "{\n  \"path\": \"http://example.com/file.csv\"\n}\n"
                              ]
                          },
                          "data": {
                              "propertyOrder": 230,
                              "title": "Data",
                              "description": "Inline data for this resource."
                          },
                          "schema": {
                              "propertyOrder": 40,
                              "title": "Schema",
                              "description": "A schema for this resource.",
                              "type": "object"
                          },
                          "title": {
                              "propertyOrder": 50,
                              "title": "Title",
                              "description": "A human-readable title.",
                              "type": "string",
                              "examples": [
                                  "{\n  \"title\": \"My Package Title\"\n}\n"
                              ]
                          },
                          "description": {
                              "propertyOrder": 60,
                              "title": "Description",
                              "description": "A text description. Markdown is encouraged.",
                              "type": "string",
                              "examples": [
                                  "{\n  \"description\": \"# My Package description\\nAll about my package.\"\n}\n"
                              ]
                          },
                          "homepage": {
                              "propertyOrder": 70,
                              "title": "Home Page",
                              "description": "The home on the web that is related to this data package.",
                              "type": "string",
                              "format": "uri",
                              "examples": [
                                  "{\n  \"homepage\": \"http://example.com/\"\n}\n"
                              ]
                          },
                          "sources": {
                              "propertyOrder": 140,
                              "options": {
                                  "hidden": true
                              },
                              "title": "Sources",
                              "description": "The raw sources for this resource.",
                              "type": "array",
                              "minItems": 1,
                              "items": {
                                  "title": "Source",
                                  "description": "A source file.",
                                  "type": "object",
                                  "required": [
                                      "title"
                                  ],
                                  "properties": {
                                      "title": {
                                          "title": "Title",
                                          "description": "A human-readable title.",
                                          "type": "string",
                                          "examples": [
                                              "{\n  \"title\": \"My Package Title\"\n}\n"
                                          ]
                                      },
                                      "path": {
                                          "title": "Path",
                                          "description": "A fully qualified URL, or a POSIX file path..",
                                          "type": "string",
                                          "examples": [
                                              "{\n  \"path\": \"file.csv\"\n}\n",
                                              "{\n  \"path\": \"http://example.com/file.csv\"\n}\n"
                                          ],
                                          "context": "Implementations need to negotiate the type of path provided, and dereference the data accordingly."
                                      },
                                      "email": {
                                          "title": "Email",
                                          "description": "An email address.",
                                          "type": "string",
                                          "format": "email",
                                          "examples": [
                                              "{\n  \"email\": \"example@example.com\"\n}\n"
                                          ]
                                      }
                                  }
                              },
                              "examples": [
                                  "{\n  \"sources\": [\n    {\n      \"name\": \"World Bank and OECD\",\n      \"uri\": \"http://data.worldbank.org/indicator/NY.GDP.MKTP.CD\"\n    }\n  ]\n}\n"
                              ]
                          },
                          "licenses": {
                              "description": "The license(s) under which the resource is published.",
                              "propertyOrder": 150,
                              "options": {
                                  "hidden": true
                              },
                              "title": "Licenses",
                              "type": "array",
                              "minItems": 1,
                              "items": {
                                  "title": "License",
                                  "description": "A license for this descriptor.",
                                  "type": "object",
                                  "properties": {
                                      "name": {
                                          "title": "Open Definition license identifier",
                                          "description": "MUST be an Open Definition license identifier, see http://licenses.opendefinition.org/",
                                          "type": "string",
                                          "pattern": "^([-a-zA-Z0-9._])+$"
                                      },
                                      "path": {
                                          "title": "Path",
                                          "description": "A fully qualified URL, or a POSIX file path..",
                                          "type": "string",
                                          "examples": [
                                              "{\n  \"path\": \"file.csv\"\n}\n",
                                              "{\n  \"path\": \"http://example.com/file.csv\"\n}\n"
                                          ],
                                          "context": "Implementations need to negotiate the type of path provided, and dereference the data accordingly."
                                      },
                                      "title": {
                                          "title": "Title",
                                          "description": "A human-readable title.",
                                          "type": "string",
                                          "examples": [
                                              "{\n  \"title\": \"My Package Title\"\n}\n"
                                          ]
                                      }
                                  },
                                  "context": "Use of this property does not imply that the person was the original creator of, or a contributor to, the data in the descriptor, but refers to the composition of the descriptor itself."
                              },
                              "context": "This property is not legally binding and does not guarantee that the package is licensed under the terms defined herein.",
                              "examples": [
                                  "{\n  \"licenses\": [\n    {\n      \"name\": \"odc-pddl-1.0\",\n      \"uri\": \"http://opendatacommons.org/licenses/pddl/\"\n    }\n  ]\n}\n"
                              ]
                          },
                          "format": {
                              "propertyOrder": 80,
                              "title": "Format",
                              "description": "The file format of this resource.",
                              "context": "`csv`, `xls`, `json` are examples of common formats.",
                              "type": "string",
                              "examples": [
                                  "{\n  \"format\": \"xls\"\n}\n"
                              ]
                          },
                          "mediatype": {
                              "propertyOrder": 90,
                              "title": "Media Type",
                              "description": "The media type of this resource. Can be any valid media type listed with [IANA](https://www.iana.org/assignments/media-types/media-types.xhtml).",
                              "type": "string",
                              "pattern": "^(.+)/(.+)$",
                              "examples": [
                                  "{\n  \"mediatype\": \"text/csv\"\n}\n"
                              ]
                          },
                          "encoding": {
                              "propertyOrder": 100,
                              "title": "Encoding",
                              "description": "The file encoding of this resource.",
                              "type": "string",
                              "default": "utf-8",
                              "examples": [
                                  "{\n  \"encoding\": \"utf-8\"\n}\n"
                              ]
                          },
                          "bytes": {
                              "propertyOrder": 110,
                              "options": {
                                  "hidden": true
                              },
                              "title": "Bytes",
                              "description": "The size of this resource in bytes.",
                              "type": "integer",
                              "examples": [
                                  "{\n  \"bytes\": 2082\n}\n"
                              ]
                          },
                          "hash": {
                              "propertyOrder": 120,
                              "options": {
                                  "hidden": true
                              },
                              "title": "Hash",
                              "type": "string",
                              "description": "The MD5 hash of this resource. Indicate other hashing algorithms with the {algorithm}:{hash} format.",
                              "pattern": "^([^:]+:[a-fA-F0-9]+|[a-fA-F0-9]{32}|)$",
                              "examples": [
                                  "{\n  \"hash\": \"d25c9c77f588f5dc32059d2da1136c02\"\n}\n",
                                  "{\n  \"hash\": \"SHA256:5262f12512590031bbcc9a430452bfd75c2791ad6771320bb4b5728bfb78c4d0\"\n}\n"
                              ]
                          }
                      }
                  },
                  "examples": [
                      "{\n  \"resources\": [\n    {\n      \"name\": \"my-data\",\n      \"data\": [\n        \"data.csv\"\n      ],\n      \"mediatype\": \"text/csv\"\n    }\n  ]\n}\n"
                  ]
              },
              "sources": {
                  "propertyOrder": 200,
                  "options": {
                      "hidden": true
                  },
                  "title": "Sources",
                  "description": "The raw sources for this resource.",
                  "type": "array",
                  "minItems": 1,
                  "items": {
                      "title": "Source",
                      "description": "A source file.",
                      "type": "object",
                      "required": [
                          "title"
                      ],
                      "properties": {
                          "title": {
                              "title": "Title",
                              "description": "A human-readable title.",
                              "type": "string",
                              "examples": [
                                  "{\n  \"title\": \"My Package Title\"\n}\n"
                              ]
                          },
                          "path": {
                              "title": "Path",
                              "description": "A fully qualified URL, or a POSIX file path..",
                              "type": "string",
                              "examples": [
                                  "{\n  \"path\": \"file.csv\"\n}\n",
                                  "{\n  \"path\": \"http://example.com/file.csv\"\n}\n"
                              ],
                              "context": "Implementations need to negotiate the type of path provided, and dereference the data accordingly."
                          },
                          "email": {
                              "title": "Email",
                              "description": "An email address.",
                              "type": "string",
                              "format": "email",
                              "examples": [
                                  "{\n  \"email\": \"example@example.com\"\n}\n"
                              ]
                          }
                      }
                  },
                  "examples": [
                      "{\n  \"sources\": [\n    {\n      \"name\": \"World Bank and OECD\",\n      \"uri\": \"http://data.worldbank.org/indicator/NY.GDP.MKTP.CD\"\n    }\n  ]\n}\n"
                  ]
              }
          }
      };
  
  var options = {
          schema: schema
        };
  
  var frame = $(DOM.loadHTML("core", "scripts/project/edit-general-metadata-dialog.html"));
  this._elmts = DOM.bind(frame);  

  this._level = DialogSystem.showDialog(frame);
  
  var editor;
  
  this._elmts.okButton.html($.i18n._('core-buttons')["ok"]);
  this._elmts.okButton.click(function() { self._submit(editor); });
  this._elmts.closeButton.html($.i18n._('core-buttons')["close"]);
  this._elmts.closeButton.click(function() { self._dismiss(); });

  $.get(
          "command/core/get-imetaData",
          {
            project : this._projectId,
            metadataFormat : "DATAPACKAGE_METADATA"
          },
          function(o) {
            if (o.code === "error") {
              alert(o.message);
            } 
            editor = new JSONEditor(document.getElementById('jsoneditor'), options, o);
          },
          "json"
        );
  
  $(".dialog-container").css("top", Math.round(($(".dialog-overlay").height() - $(frame).height()) / 16) + "px");
};

EditGeneralMetadataDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

EditGeneralMetadataDialog.prototype._submit = function(editor) {
    if (typeof this._callback === "function") {
        this._callback(editor.getText());
    }
    this._dismiss();
};