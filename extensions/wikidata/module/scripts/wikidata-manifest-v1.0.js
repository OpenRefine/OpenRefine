const WikidataManifestV1_0 = {
  "version": "1.0",
  "mediawiki": {
    "name": "Wikidata",
    "root": "https://www.wikidata.org/wiki/",
    "main_page": "https://www.wikidata.org/wiki/Wikidata:Main_Page",
    "api": "https://www.wikidata.org/w/api.php"
  },
  "wikibase": {
    "properties": {
      "entity_prefix": "http://www.wikidata.org/entity/",
      "instance_of": "P31",
      "subclass_of": "P279",
      "property_constraint": "P2302"
    },
    "constraints": [
      {
        "name": "allowed_entity_types_constraint",
        "qid": "Q52004125",
        "item_of_property_constraint": "P2305",
        "wikibase_item": "Q29934200",
        "wikibase_property": "Q29934218",
        "lexeme": "Q51885771",
        "form": "Q54285143",
        "sense": "Q54285715",
        "wikibase_mediainfo": "Q59712033"
      },
      {
        "name": "allowed_qualifiers_constraint",
        "qid": "Q21510851",
        "property": "P2306"
      },
      {
        "name": "allowed_units_constraint",
        "qid": "Q21514353",
        "item_of_property_constraint": "P2305"
      },
      {
        "name": "citation_needed_constraint",
        "qid": "Q54554025"
      },
      {
        "name": "conflicts_with_constraint",
        "qid": "Q21502838",
        "property": "P2306",
        "item_of_property_constraint": "P2305"
      },
      {
        "name": "contemporary_constraint",
        "qid": "Q25796498"
      },
      {
        "name": "difference_within_range_constraint",
        "qid": "Q21510854",
        "property": "P2306",
        "minimum_value": "P2313",
        "maximum_value": "P2312"
      },
      {
        "name": "distinct_values_constraint",
        "qid": "Q21502410"
      },
      {
        "name": "format_constraint",
        "qid": "Q21502404",
        "format_as_a_regular_expression": "P1793",
        "syntax_clarification": "P2916"
      },
      {
        "name": "integer_constraint",
        "qid": "Q52848401"
      },
      {
        "name": "inverse_constraint",
        "qid": "Q21510855",
        "property": "P2306"
      },
      {
        "name": "item_requires_statement_constraint",
        "qid": "Q21503247",
        "property": "P2306",
        "item_of_property_constraint": "P2305"
      },
      {
        "name": "mandatory_qualifier_constraint",
        "qid": "Q21510856",
        "property": "P2306"
      },
      {
        "name": "multi_value_constraint",
        "qid": "Q21510857"
      },
      {
        "name": "no_bounds_constraint",
        "qid": "Q51723761"
      },
      {
        "name": "none_of_constraint",
        "qid": "Q52558054",
        "item_of_property_constraint": "P2305"
      },
      {
        "name": "one_of_constraint",
        "qid": "Q21510859",
        "item_of_property_constraint": "P2305"
      },
      {
        "name": "one_of_qualifier_value_property_constraint",
        "qid": "Q52712340",
        "property": "P2306",
        "item_of_property_constraint": "P2305"
      },
      {
        "name": "property_scope_constraint",
        "qid": "Q53869507",
        "property_scope": "P5314",
        "as_main_value": "Q54828448",
        "as_qualifiers": "Q54828449",
        "as_references": "Q54828450"
      },
      {
        "name": "range_constraint",
        "qid": "Q21510860",
        "minimum_value": "P2313",
        "maximum_value": "P2312",
        "minimum_date": "P2310",
        "maximum_date": "P2311"
      },
      {
        "name": "single_best_value_constraint",
        "qid": "Q52060874",
        "separator": ":P4155"
      },
      {
        "name": "single_value_constraint",
        "qid": "Q19474404",
        "separator": ":P4155"
      },
      {
        "name": "symmetric_constraint",
        "qid": "Q21510862"
      },
      {
        "name": "type_constraint",
        "qid": "Q21503250",
        "relation": "P2309",
        "instance_of": "Q21503252",
        "subclass_of": "Q21514624",
        "instance_or_subclass_of": "Q30208840",
        "class": "P2308"
      },
      {
        "name": "value_requires_statement_constraint",
        "qid": "Q21510864",
        "property": "P2306",
        "item_of_property_constraint": "P2305"
      },
      {
        "name": "value_type_constraint",
        "qid": "Q21510865",
        "relation": "P2309",
        "instance_of": "Q21503252",
        "subclass_of": "Q21514624",
        "instance_or_subclass_of": "Q30208840",
        "class": "P2308"
      }
    ]
  },
  "reconciliation": {
    "endpoint": "https://wdreconcile.toolforge.org/en/api"
  }
};
