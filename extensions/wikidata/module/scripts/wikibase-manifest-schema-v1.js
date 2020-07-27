const WikibaseManifestSchemaV1 = {
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "TBD",
  "type": "object",
  "description": "The schema validates Wikibase manifests with version 1.x",
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^1\\.[0-9]+$",
      "description": "The version of the Wikibase manifest, in the format of 1.x",
    },
    "mediawiki": {
      "type": "object",
      "description": "The configurations of the MediaWiki engine",
      "properties": {
        "name": {
          "type": "string",
          "description": "The name of the Wikibase, such as 'Wikidata'"
        },
        "root": {
          "type": "string",
          "pattern": "^.*/$",
          "description": "The URL of the root of the Wikibase, such as 'https://www.wikidata.org/wiki/'. The trailing slash cannot be omitted"
        },
        "main_page": {
          "type": "string",
          "description": "The URL of the main page of the Wikibase, such as 'https://www.wikidata.org/wiki/Wikidata:Main_Page'"
        },
        "api": {
          "type": "string",
          "description": "The MediaWiki API endpoint of the Wikibase, such as 'https://www.wikidata.org/w/api.php'"
        }
      },
      "required": ["name", "root", "main_page", "api"]
    },
    "wikibase": {
      "type": "object",
      "description": "The configurations of the Wikibase extension",
      "properties": {
        "properties": {
          "type": "object",
          "properties": {
            "entity_prefix": {
              "type": "string",
              "pattern": "^.*/$",
              "description": "The entity prefix of the Wikibase, such as 'http://www.wikidata.org/entity/'. The trailing slash cannot be omitted"
            },
            "instance_of": {
              "type": "string",
              "description": "The 'instance of' qid of the Wikibase ('P31' for Wikidata)"
            },
            "subclass_of": {
              "type": "string",
              "description": "The 'subclass of' qid of the Wikibase ('P279' for Wikidata)"
            }
          },
          "required": ["entity_prefix", "instance_of", "subclass_of"]
        },
        "constraints": {
          "type": "object",
          "description": "Constraints related qids and pids, not required since the constraints extension may not be installed",
          "properties": {
            "property_constraint_pid": {
              "type": "string",
              "description": "The property constraint pid of the Wikibase ('P2302' for Wikidata)"
            }
          },
          "patternProperties": {
            "^.*$": {
              "type": "string",
              "description": "If a pid/qid is missing, constraint checks depending on it will be skipped"
            }
          },
          "required": ["property_constraint_pid"]
        }
      },
      "required": ["properties"]
    },
    "reconciliation": {
      "type": "object",
      "description": "The configurations of the default reconciliation service of the Wikibase",
      "properties": {
        "endpoint": {
          "type": "string",
          "description": "The default reconciliation API endpoint of the Wikibase, such as 'https://wdreconcile.toolforge.org/en/api'"
        }
      },
      "required": ["endpoint"]
    },
  },
  "required": ["version", "mediawiki", "wikibase", "reconciliation"]
};
