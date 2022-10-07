const CommonsManifestV2_0 = {
  "version": "2.0",
  "mediawiki": {
    "name": "Wikimedia Commons",
    "root": "https://commons.wikimedia.org/wiki/",
    "main_page": "https://commons.wikimedia.org/wiki/Main_Page",
    "api": "https://commons.wikimedia.org/w/api.php"
  },
  "wikibase": {
    "site_iri": "https://commons.wikimedia.org/entity/",
    "maxlag": 5,
    "properties": {
      "instance_of": "P31",
      "subclass_of": "P279"
    },
    "constraints": {
      "property_constraint_pid": "P2302",
      "exception_to_constraint_pid": "P2303",
      "constraint_status_pid": "P2316",
      "mandatory_constraint_qid": "Q21502408",
      "suggestion_constraint_qid": "Q62026391",
      "distinct_values_constraint_qid": "Q21502410",
      "multi_value_constraint_qid": "Q21510857",
      "used_as_qualifier_constraint_qid": "Q21510863",
      "single_value_constraint_qid": "Q19474404",
      "symmetric_constraint_qid": "Q21510862",
      "type_constraint_qid": "Q21503250",
      "value_type_constraint_qid": "Q21510865",
      "inverse_constraint_qid": "Q21510855",
      "item_requires_statement_constraint_qid": "Q21503247",
      "value_requires_statement_constraint_qid": "Q21510864",
      "conflicts_with_constraint_qid": "Q21502838",
      "one_of_constraint_qid": "Q21510859",
      "mandatory_qualifier_constraint_qid": "Q21510856",
      "allowed_qualifiers_constraint_qid": "Q21510851",
      "range_constraint_qid": "Q21510860",
      "difference_within_range_constraint_qid": "Q21510854",
      "common_link_constraint_qid": "Q21510852",
      "contemporary_constraint_qid": "Q25796498",
      "format_constraint_qid": "Q21502404",
      "used_for_values_only_constraint_qid": "Q21528958",
      "used_as_reference_constraint_qid": "Q21528959",
      "no_bounds_constraint_qid": "Q51723761",
      "allowed_units_constraint_qid": "Q21514353",
      "single_best_value_constraint_qid": "Q52060874",
      "allowed_entity_types_constraint_qid": "Q52004125",
      "citation_needed_constraint_qid": "Q54554025",
      "property_scope_constraint_qid": "Q53869507",
      "class_pid": "P2308",
      "relation_pid": "P2309",
      "instance_of_relation_qid": "Q21503252",
      "subclass_of_relation_qid": "Q21514624",
      "instance_or_subclass_of_relation_qid": "Q30208840",
      "property_pid": "P2306",
      "item_of_property_constraint_pid": "P2305",
      "minimum_value_pid": "P2313",
      "maximum_value_pid": "P2312",
      "minimum_date_pid": "P2310",
      "maximum_date_pid": "P2311",
      "namespace_pid": "P2307",
      "format_as_a_regular_expression_pid": "P1793",
      "syntax_clarification_pid": "P2916",
      "constraint_scope_pid": "P4680",
      "separator_pid": "P4155",
      "constraint_checked_on_main_value_qid": "Q46466787",
      "constraint_checked_on_qualifiers_qid": "Q46466783",
      "constraint_checked_on_references_qid": "Q46466805",
      "none_of_constraint_qid": "Q52558054",
      "one_of_qualifier_value_property_constraint_qid": "Q52712340",
      "integer_constraint_qid": "Q52848401",
      "wikibase_item_qid": "Q29934200",
      "wikibase_property_qid": "Q29934218",
      "wikibase_lexeme_qid": "Q51885771",
      "wikibase_form_qid": "Q54285143",
      "wikibase_sense_qid": "Q54285715",
      "wikibase_media_info_qid": "Q59712033",
      "property_scope_pid": "P5314",
      "as_main_value_qid": "Q54828448",
      "as_qualifiers_qid": "Q54828449",
      "as_references_qid": "Q54828450"
    }
  },
  "oauth": {
    "registration_page": "https://commons.wikimedia.org/wiki/Special:OAuthConsumerRegistration/propose"
  },
  "entity_types": {
    "item": {
      "site_iri": "http://www.wikidata.org/entity/",
      "reconciliation_endpoint": "https://wikidata.reconci.link/${lang}/api",
      "mediawiki_api": "https://www.wikidata.org/w/api.php"
    },
    "property": {
      "site_iri": "http://www.wikidata.org/entity/",
      "mediawiki_api": "https://www.wikidata.org/w/api.php"
    },
    "mediainfo": {
      "site_iri": "https://commons.wikimedia.org/entity/",
      "reconciliation_endpoint": "https://commonsreconcile.toolforge.org/${lang}/api"
    }
  },
  "hide_structured_fields_in_mediainfo": false,
  "editgroups": {
    "url_schema": "([[:toollabs:editgroups-commons/b/OR/${batch_id}|details]])"
  },
  "schema_templates": [
    {
      "name": "Information (basic data for every Wikimedia Commons file)",
      "schema": {
        "entityEdits": [
          {
            "type": "wbmediainfoeditexpr",
            "subject": null,
            "filePath": null,
            "fileName": null,
            "wikitext": null,
            "overrideWikitext": false,
            "statementGroups": [
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P170",
                  "label": "creator",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P571",
                  "label": "inception",
                  "datatype": "time"
                },
                "statements": [
                  {
                    "value": {
                      "type": "wbdateconstant",
                      "value": ""
                    },
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P7482",
                  "label": "source of file",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P6216",
                  "label": "copyright status",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P275",
                  "label": "copyright license",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P180",
                  "label": "depicts",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              }
            ],
            "nameDescs": [
              {
                "type": "wbnamedescexpr",
                "name_type": "LABEL_IF_NEW",
                "value": {
                  "type": "wbmonolingualexpr",
                  "language": {
                    "type": "wblanguageconstant",
                    "id": "en",
                    "label": "en"
                  },
                  "value": null
                }
              }
            ]
          }
        ],
        "siteIri": "https://commons.wikimedia.org/entity/",
        "entityTypeSiteIRI": {
          "item": "http://www.wikidata.org/entity/",
          "property": "http://www.wikidata.org/entity/",
          "mediainfo": "https://commons.wikimedia.org/entity/"
        },
        "mediaWikiApiEndpoint": "https://commons.wikimedia.org/w/api.php"
      }
    },
    {
      "name": "Artwork (faithful digital representation / digital surrogate of a work)",
      "schema": {
        "entityEdits": [
          {
            "type": "wbmediainfoeditexpr",
            "subject": null,
            "filePath": null,
            "fileName": null,
            "wikitext": null,
            "overrideWikitext": false,
            "statementGroups": [
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P170",
                  "label": "creator",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P571",
                  "label": "inception",
                  "datatype": "time"
                },
                "statements": [
                  {
                    "value": {
                      "type": "wbdateconstant",
                      "value": ""
                    },
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P7482",
                  "label": "source of file",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P6216",
                  "label": "copyright status",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P275",
                  "label": "copyright license",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P6243",
                  "label": "digital representation of",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              }
            ],
            "nameDescs": [
              {
                "type": "wbnamedescexpr",
                "name_type": "LABEL_IF_NEW",
                "value": {
                  "type": "wbmonolingualexpr",
                  "language": {
                    "type": "wblanguageconstant",
                    "id": "en",
                    "label": "en"
                  },
                  "value": null
                }
              }
            ]
          }
        ],
        "siteIri": "https://commons.wikimedia.org/entity/",
        "entityTypeSiteIRI": {
          "item": "http://www.wikidata.org/entity/",
          "property": "http://www.wikidata.org/entity/",
          "mediainfo": "https://commons.wikimedia.org/entity/"
        },
        "mediaWikiApiEndpoint": "https://commons.wikimedia.org/w/api.php"
      }
    },
    {
      "name": "Art photo (from a point of view; not an exact digitization of the work)",
      "schema": {
        "entityEdits": [
          {
            "type": "wbmediainfoeditexpr",
            "subject": null,
            "filePath": null,
            "fileName": null,
            "wikitext": null,
            "overrideWikitext": false,
            "statementGroups": [
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P170",
                  "label": "creator",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P571",
                  "label": "inception",
                  "datatype": "time"
                },
                "statements": [
                  {
                    "value": {
                      "type": "wbdateconstant",
                      "value": ""
                    },
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P7482",
                  "label": "source of file",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P6216",
                  "label": "copyright status",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P275",
                  "label": "copyright license",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P180",
                  "label": "depicts",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P921",
                  "label": "main subject",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              }
            ],
            "nameDescs": [
              {
                "type": "wbnamedescexpr",
                "name_type": "LABEL_IF_NEW",
                "value": {
                  "type": "wbmonolingualexpr",
                  "language": {
                    "type": "wblanguageconstant",
                    "id": "en",
                    "label": "en"
                  },
                  "value": null
                }
              }
            ]
          }
        ],
        "siteIri": "https://commons.wikimedia.org/entity/",
        "entityTypeSiteIRI": {
          "item": "http://www.wikidata.org/entity/",
          "property": "http://www.wikidata.org/entity/",
          "mediainfo": "https://commons.wikimedia.org/entity/"
        },
        "mediaWikiApiEndpoint": "https://commons.wikimedia.org/w/api.php"
      }
    },
    {
      "name": "Book or publication (work has a Wikidata item)",
      "schema": {
        "entityEdits": [
          {
            "type": "wbmediainfoeditexpr",
            "subject": null,
            "filePath": null,
            "fileName": null,
            "wikitext": null,
            "overrideWikitext": false,
            "statementGroups": [
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P170",
                  "label": "creator",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P571",
                  "label": "inception",
                  "datatype": "time"
                },
                "statements": [
                  {
                    "value": {
                      "type": "wbdateconstant",
                      "value": ""
                    },
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P7482",
                  "label": "source of file",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P6216",
                  "label": "copyright status",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P275",
                  "label": "copyright license",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P6243",
                  "label": "digital representation of",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              },
              {
                "property": {
                  "type": "wbpropconstant",
                  "pid": "P195",
                  "label": "collection",
                  "datatype": "wikibase-item"
                },
                "statements": [
                  {
                    "value": null,
                    "qualifiers": [],
                    "references": [],
                    "mode": "add_or_merge",
                    "mergingStrategy": {
                      "type": "snak",
                      "valueMatcher": {
                        "type": "lax"
                      }
                    }
                  }
                ]
              }
            ],
            "nameDescs": [
              {
                "type": "wbnamedescexpr",
                "name_type": "LABEL_IF_NEW",
                "value": {
                  "type": "wbmonolingualexpr",
                  "language": {
                    "type": "wblanguageconstant",
                    "id": "en",
                    "label": "en"
                  },
                  "value": null
                }
              }
            ]
          }
        ],
        "siteIri": "https://commons.wikimedia.org/entity/",
        "entityTypeSiteIRI": {
          "item": "http://www.wikidata.org/entity/",
          "property": "http://www.wikidata.org/entity/",
          "mediainfo": "https://commons.wikimedia.org/entity/"
        },
        "mediaWikiApiEndpoint": "https://commons.wikimedia.org/w/api.php"
      }
    }
  ]
};
