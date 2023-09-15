---
id: configuration
title: Connecting OpenRefine to a Wikibase instance
sidebar_label: Connecting to Wikibase
---

This page explains how to connect OpenRefine to any Wikibase instance. If you just want to work with [Wikidata](https://www.wikidata.org/), you can ignore this page as Wikidata is configured out of the box in OpenRefine.

## For Wikibase end users {#for-wikibase-end-users}

All you need to configure OpenRefine to work with a Wikibase instance is a *manifest* for that instance, which provides some metadata and links required for the integration to work.

We offer some off-the-shelf manifests for some public Wikibase instances in the [wikibase-manifests](https://github.com/OpenRefine/wikibase-manifests) repository. But the administrators of your Wikibase instance should provide one that is potentially more
up to date, so it makes sense to request it to them first.

## For Wikibase administrators {#for-wikibase-administrators}

To let your users contribute to your Wikibase instance with OpenRefine, you will need to write a manifest as described above. There is currently no canonical location where this manifest should be hosted - just make sure can be found easily by your users. This section explains the format of the manifest.

### Requirements {#requirements}

To work with OpenRefine, your Wikibase instance needs an associated reconciliation service. For instance you can use [a Python wrapper](https://github.com/wetneb/openrefine-wikibase) for this.


### The format of the manifest {#the-format-of-the-manifest}

Here is the manifest of Wikidata:

```json
{
  "version": "1.0",
  "mediawiki": {
    "name": "Wikidata",
    "root": "https://www.wikidata.org/wiki/",
    "main_page": "https://www.wikidata.org/wiki/Wikidata:Main_Page",
    "api": "https://www.wikidata.org/w/api.php"
  },
  "wikibase": {
    "site_iri": "http://www.wikidata.org/entity/",
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
      // ...
    }
  },
  "oauth": {
    "registration_page": "https://meta.wikimedia.org/wiki/Special:OAuthConsumerRegistration/propose"
  },
  "reconciliation": {
    "endpoint": "https://wdreconcile.toolforge.org/${lang}/api"
  },
  "editgroups": {
    "url_schema": "([[:toollabs:editgroups/b/OR/${batch_id}|details]])"
  }
}
```

In general, there are several parts of the manifest: version, mediawiki, wikibase, oauth, reconciliation and editgroups.

#### version {#version}

The version should in the format "1.x". The minor version should be increased when you update the manifest in a backward-compatible manner. The major version should be "1" if the manifest is in the format specified by [wikibase-manifest-schema-v1.json](https://github.com/afkbrb/wikibase-manifest/blob/master/wikibase-manifest-schema-v1.json).

#### mediawiki {#mediawiki}

This part contains some basic information of the Wikibase.

##### name {#name}

The name of the Wikibase, should be unique for different Wikibase instances.

##### root {#root}

The root of the Wikibase. Typically in the form "https://foo.bar/wiki/". The trailing slash cannot be omitted.

##### main_page {#main_page}

The main page of the Wikibase. Typically in the form "https://foo.bar/wiki/Main_Page".

##### api {#api}

The MediaWiki API endpoint of the Wikibase. Typically in the form "https://foo.bar/w/api.php".

#### wikibase {#wikibase}

This part contains configurations of the Wikibase extension.

##### site_iri {#site_iri}

The IRI of the Wikibase, in the form  'http://foo.bar/entity/'. This should match the IRI prefixes used in RDF serialization. Be careful about using "http" or "https", because any variation will break comparisons at various places. The trailing slash cannot be omitted.

##### maxlag {#maxlag}

Maxlag is a parameter that controls how aggressive a mass-editing tool should be when uploading edits to a Wikibase instance. See https://www.mediawiki.org/wiki/Manual:Maxlag_parameter for more details. The value should be adapted according to the actual traffic of the Wikibase.

##### properties {#properties}

Some special properties of the Wikibase.

###### instance_of {#instance_of}

The ID of the property "instance of".

###### subclass_of {#subclass_of}

The ID of the property "subclass of".

##### constraints {#constraints}

Not required. Should be configured if the Wikibase has the [WikibaseQualityConstraints extension](https://www.mediawiki.org/wiki/Extension:WikibaseQualityConstraints) installed. Configurations of constraints consists of IDs of constraints related properties and items. For Wikidata, these IDs are retrieved from [extension.json](https://github.com/wikimedia/mediawiki-extensions-WikibaseQualityConstraints/blob/master/extension.json). To configure this for another Wikibase instance, you should contact an admin of the Wikibase instance to get the content of `extension.json`.

#### oauth {#oauth}

Not required. Should be configured if the Wikibase has the [OAuth extension](https://www.mediawiki.org/wiki/Extension:OAuth) installed.

##### registration_page {#registration_page}

The page to register an OAuth consumer of the Wikibase. Typically in the form "https://foo.bar/wiki/Special:OAuthConsumerRegistration/propose".

#### reconciliation {#reconciliation}

The Wikibase instance must have at least a reconciliation service endpoint linked to it. If there is no reconciliation service for the Wikibase, you can run one with [openrefine-wikibase](https://github.com/wetneb/openrefine-wikibase).

##### endpoint {#endpoint}

The default reconciliation service endpoint of the Wikibase instance. The endpoint must contain the "${lang}" variable such as "https://wdreconcile.toolforge.org/${lang}/api", since the reconciliation service is expected to work for different languages.

#### editgroups {#editgroups}

Not required. Should be configured if the Wikibase instance has [EditGroups](https://github.com/Wikidata/editgroups) service(s).

##### url_schema {#url_schema}

The URL schema used in edits summary. This is used for EditGroups to extract the batch id from a batch of edits and for linking to the EditGroups page of the batch. The URL schema must contains the variable '${batch_id}', such as '([[:toollabs:editgroups/b/OR/${batch_id}|details]])' for Wikidata.

#### Check the format of the manifest {#check-the-format-of-the-manifest}

As mentioned above, the manifest should be in the format specified by [wikibase-manifest-schema-v1.json](https://github.com/afkbrb/wikibase-manifest/blob/master/wikibase-manifest-schema-v1.json). You can check the format by adding the manifest directly to OpenRefine, and OpenRefine will complain if there is anything wrong with the format.

![test-validate-manifest-format](https://user-images.githubusercontent.com/29347603/90506110-52d85d00-e186-11ea-8077-683d2f234c46.gif)
