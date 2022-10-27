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
      "distinct_values_constraint_qid": "Q21502410"
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
