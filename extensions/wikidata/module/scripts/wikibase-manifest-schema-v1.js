const WikibaseManifestSchemaV1 = {
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "TBD",
  "type": "object",
  "description": "The schema validates Wikibase manifests with version 1.x. The manifest contains configurations of basic information (e.g. URL of the main page), extensions (e.g. OAuth extension) or external services (e.g. Reconciliation service) of a Wikibase",
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
        "site_iri": {
          "type": "string",
          "pattern": "^.*/$",
          "description": "The IRI of the Wikibase, such as 'http://www.wikidata.org/entity/'. This should match the IRI prefixes used in RDF serialization. Be careful about using 'http' or 'https', because any variation will break comparisons at various places. The trailing slash cannot be omitted"
        },
        "properties": {
          "type": "object",
          "properties": {
            "instance_of": {
              "type": "string",
              "description": "The 'instance of' qid of the Wikibase ('P31' for Wikidata)"
            },
            "subclass_of": {
              "type": "string",
              "description": "The 'subclass of' qid of the Wikibase ('P279' for Wikidata)"
            }
          },
          "required": ["instance_of", "subclass_of"]
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
    "oauth": {
      "type": "object",
      "description": "The configurations of the OAuth extension. Not required. Configuring this if and only if the OAuth extension is installed",
      "properties": {
        "registration_page": {
          "type": "string",
          "description": "The url of the OAuth consumer registration page, 'https://meta.wikimedia.org/wiki/Special:OAuthConsumerRegistration/propose' for Wikidata"
        }
      },
      "required": ["registration_page"]
    },
    "reconciliation": {
      "type": "object",
      "description": "The configurations of the default reconciliation service of the Wikibase",
      "properties": {
        "endpoint": {
          "type": "string",
          "pattern": "^.*\\${lang}.*$",
          "description": "The default reconciliation API endpoint of the Wikibase, the endpoint should include the language variable '${lang}', such as 'https://wdreconcile.toolforge.org/${lang}/api'"
        }
      },
      "required": ["endpoint"]
    },
  },
  "required": ["version", "mediawiki", "wikibase", "reconciliation"]
};
