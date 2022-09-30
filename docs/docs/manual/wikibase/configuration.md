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

To work with OpenRefine, your Wikibase instance needs an associated reconciliation service for each editable entity type:

* To enable editing items (entities with an identifier starting with Q), you can deploy [a Python wrapper](https://github.com/wetneb/openrefine-wikibase) for this. It exposes a reconciliation service for items, built on top of Wikibase's own API and its Query Service.
  Note that this service requires the [UniversalLanguageSelector extension](https://www.mediawiki.org/wiki/Special:MyLanguage/Extension:UniversalLanguageSelector) should be installed.

* To enable editing media files (if your Wikibase instance accepts file uploads), you can use [another Python wrapper](https://github.com/wikimedia/labs-tools-commons-recon-service) which exposes a reconciliation service for media files.

* Editing properties or other entity types is not supported yet.

We are aware that deploying those additional web services can be difficult for some Wikibase users, and we think those web services should be replaced by a MediaWiki extension which exposes the reconciliation endpoints from MediaWiki itself. We are not
aware of anyone planning to work on this, though.

### The format of the manifest {#the-format-of-the-manifest}

The manifest is a JSON object describing all the configuration details necessary for OpenRefine to integrate with your Wikibase instance. As an example, here is the manifest of Wikimedia Commons:

```json
{
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
    "max_edits_per_minute": 60,
    "tag": "openrefine-${version}",
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
  }
}
```

In general, there are several parts of the manifest: version, mediawiki, wikibase, oauth, entity_types and editgroups.

#### version {#version}

The version should in the format "2.x". The minor version should be increased when you update the manifest in a backward-compatible manner. The major version should be "2" if the manifest is in the format specified by [wikibase-manifest-schema-v2.json](https://github.com/afkbrb/wikibase-manifest/blob/master/wikibase-manifest-schema-v2.json).

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

##### tag {#tag}

Specifies a tag which should be applied to all edits made via the tool. The <code>${version}</code> variable will be replaced by the "major.minor" OpenRefine version before making edits.

##### max_edits_per_minute {#max_edits_per_minute}

Determines the editing speed expressed as the maximum number of edits to perform per minute, as an integer. The editing can still be slower than this rate if the performance of the Wikibase instance degrades. If set to 0, this will disable this cap.

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

#### entity_types {#entity_types}

The Wikibase instance can support several entity types (such as `item`, `property` or `lexeme`), and this section stores parameters which are specific to those entity types.

The Wikibase instance must have at least a reconciliation service endpoint linked to it. 

##### reconciliation_endpoint {#reconciliation_endpoint}

The default reconciliation service endpoint for entities of this type. The endpoint must contain the "${lang}" variable such as "https://wikidata.reconci.link/${lang}/api", since the reconciliation service is expected to work for different languages. For the `item` entity type, you can get such a reconciliation service with [openrefine-wikibase](https://github.com/wetneb/openrefine-wikibase). For the `mediainfo` entity type, you can use the [commons-recon-service](https://gerrit.wikimedia.org/g/labs/tools/commons-recon-service) which can be configured to run for other Wikibase instances.

This parameter is optional: you do not need to run a reconciliation for all entity types available in your Wikibase instance. However, it is a prerequisite for being able to do edits to those entity types via OpenRefine.

##### site_iri {#site_iri}

The base IRI for the entities of this type. This property is required. By default, this is expected to be the same as the site IRI for the Wikibase instance (see above), but if entities of this type are federated from another instance, then this should be set to the site IRI of that Wikibase instance.

##### mediawiki_api {#mediawiki_api}

The URL of the MediaWiki API to use with entities of this type. If not provided, it is expected to be the same as the MediaWiki API endpoint for this instance, but if entities of this type are federated from another instance, then this should be set to the MediaWiki API endpoint of that Wikibase instance.

#### hide_structured_fields_in_mediainfo

Not required. Set this flag to true if your Wikibase instance supports file uploads (in which case it should have a `mediainfo` section in the `entity_types` object above), but it does not support adding captions and statements directly on the files themselves (unlike
Wikimedia Commons).

#### editgroups {#editgroups}

Not required. Should be configured if the Wikibase instance has [EditGroups](https://github.com/Wikidata/editgroups) service(s).

##### url_schema {#url_schema}

The URL schema used in edits summary. This is used for EditGroups to extract the batch id from a batch of edits and for linking to the EditGroups page of the batch. The URL schema must contains the variable '${batch_id}', such as '([[:toollabs:editgroups/b/OR/${batch_id}|details]])' for Wikidata.

#### Check the format of the manifest {#check-the-format-of-the-manifest}

As mentioned above, the manifest should be in the format specified by [wikibase-manifest-schema-v2.json](https://github.com/afkbrb/wikibase-manifest/blob/master/wikibase-manifest-schema-v2.json). You can check the format by adding the manifest directly to OpenRefine, and OpenRefine will complain if there is anything wrong with the format.

![test-validate-manifest-format](https://user-images.githubusercontent.com/29347603/90506110-52d85d00-e186-11ea-8077-683d2f234c46.gif)

#### Migrate from the version 1 to the version 2 of the manifest format

If you have created a manifest for your Wikibase instance before OpenRefine 3.6, then you have used the version 1 format for manifests. This format was generalized (into version 2) in OpenRefine 3.6 to allow for editing different entity types.
If you are interested in letting OpenRefine users edit not just items on your Wikibase instance, but also other types of entities (such as media files), then you should migrate your manifest from version 1 to 2.

To do so, you need to:

* Change the `version` field of your manifest to 2.0;

* Introduce the new `entity_types` field, into which the URL of your existing reconciliation service should go (inside the `item` subsection). See the documentation above for more details about the expected values of such fields;

* Deploy [the additional reconciliation service for media files](https://github.com/wikimedia/labs-tools-commons-recon-service) and reference it in the `entity_types` section of the manifest, following the documentation above.

* If your Wikibase instance supports file uploads, but does not use structured data on those files, add the `hide_structured_fields_in_mediainfo` field to your manifest, as documented above.

After you have made those changes to your manifest, OpenRefine users will need to add it again to their list of Wikibase instances for the changes to take effect.

