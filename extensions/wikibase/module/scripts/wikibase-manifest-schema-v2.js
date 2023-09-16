const WikibaseManifestSchemaV2 = {
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "https://openrefine.org/schemas/wikibase-manifest-v2.json",
  "type": "object",
  "description": "The schema validates Wikibase manifests with version 2.x. The manifest contains configurations of basic information (e.g. URL of the main page), extensions (e.g. OAuth extension) or external services (e.g. Reconciliation service) of a Wikibase",
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^2\\.[0-9]+$",
      "description": "The version of the Wikibase manifest, in the format of 2.x"
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
        "maxlag": {
          "type": "integer",
          "description": "The default maxlag of this Wikibase. For Wikidata, the default value is 5 (seconds)"
        },
        "tag": {
          "type": "string",
          "description": "The tag to apply to edits made to the Wikibase instance. If the ${version} string is included, it will be replaced by the major.minor version of OpenRefine used to make the edit"
        },
        "max_edits_per_minute": {
          "type": "integer",
          "description": "The maximum number of edits to do per minute, on this Wikibase instance. By default, 60."
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
      "required": ["site_iri", "maxlag", "properties"]
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
    "entity_types": {
      "type": "object",
      "description": "The available entity types on this Wikibase, with particular settings for each of them. The keys of this object are strings such as 'item', 'property', 'mediainfo' or 'lexeme'",
      "patternProperties": {
        "^.*$": {
          "type": "object",
          "description": "The settings for the given entity type.",
          "properties": {
            "reconciliation_endpoint": {
              "type": "string",
              "pattern": "^.*\\${lang}.*$",
              "description": "The default reconciliation API endpoint of the Wikibase for this entity type, the endpoint should include the language variable '${lang}', such as 'https://wikidata.reconci.link/${lang}/api'"
            },
            "site_iri": {
              "type": "string",
              "description": "The RDF prefix in which URIs for entities of this type are minted. If entities of this type are edited locally, this should be the global site_iri of the Wikibase. If those entities are drawn from another Wikibase instance, it should be the site_iri of that instance."
            },
            "mediawiki_api": {
              "type": "string",
              "description": "The MediaWiki API endpoint of the Wikibase, such as 'https://www.wikidata.org/w/api.php'"
            }
          },
          "required": ["site_iri"]
        }
      }
    },
    "editgroups": {
      "type": "object",
      "description": "The configurations of the EditGroups service of the Wikibase",
      "properties": {
        "url_schema": {
          "type": "string",
          "pattern": "^.*\\${batch_id}.*$",
          "description": "The URL schema used in edits summary. This is used for EditGroups to extract the batch id from a batch of edits and for linking to the EditGroups page of the batch. The URL schema must contains the variable '${batch_id}', such as '([[:toollabs:editgroups/b/OR/${batch_id}|details]])' for Wikidata"
        },
      },
      "required": ["url_schema"]
    },
    "schema_templates": {
      "type": "array",
      "description": "A list of predefined schema templates to be offered to the user",
      "items": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string",
            "description": "The name of the template to be displayed to the user"
          },
          "schema": {
            "type": "object",
            "description": "The schema, potentially incomplete, to be loaded with this template"
          }
        },
        "required": ["name", "schema"]
      }
    },
    "hide_structured_fields_in_mediainfo": {
      "type": "boolean",
      "description": "Boolean set to true when the Wikibase instance supports file uploads but does not have structured data associated to them in the form of MediaInfo entities"
    }
  },
  "required": ["version", "mediawiki", "wikibase", "entity_types"]
};
