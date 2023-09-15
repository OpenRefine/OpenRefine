---
id: migrating-older-extensions
title: Migrating older Extensions
sidebar_label: Migrating older Extensions
---

## Migrating from Ant to Maven {#migrating-from-ant-to-maven}

### Why are we doing this change? {#why-are-we-doing-this-change}

Ant is a fairly old (antique?) build system that does not incorporate any dependency management.
By migrating to Maven we are making it easier for developers to extend OpenRefine with new libraries, and  stop having to ship dozens of .jar files in the repository. Using the Maven repository also encourages developers to add dependencies to released versions of libraries instead of custom snapshots that are hard to update.

### When was this change made? {#when-was-this-change-made}

The migration was done between 3.0 and 3.1-beta with this commit:
https://github.com/OpenRefine/OpenRefine/commit/47323a9e750a3bc9d43af606006b5eb20ca397b8

### How to migrate an extension {#how-to-migrate-an-extension}

You will need to write a `pom.xml` in the root folder of your extension to configure the compilation process with Maven. Sample `pom.xml` files for extensions can be found in the extensions that are shipped with OpenRefine (`gdata`, `database`, `jython`, `pc-axis` and `wikidata`). A sample extension (`sample`) is also provided, with a minimal build file.

For any library that your extension depends on, you should try to find a matching artifact in the Maven Central repository. If you can find such an artifact, delete the `.jar` file from your extension and add the dependency in your `pom.xml` file. If you cannot find such an artifact, it is still possible to incorporate your own `.jar` file using `maven-install-plugin` that you can configure in your `pom.xml` file as follows:


      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-install-plugin</artifactId>
        <version>2.5.2</version>
        <executions>
            <execution>
                <id>install-wdtk-datamodel</id>
                <phase>process-resources</phase>
                <configuration>
                    <file>${basedir}/lib/my-proprietary-library.jar</file>
                    <repositoryLayout>default</repositoryLayout>
                    <groupId>com.my.company</groupId>
                    <artifactId>my-library</artifactId>
                    <version>0.5.3-SNAPSHOT</version>
                    <packaging>jar</packaging>
                    <generatePom>true</generatePom>
                </configuration>
                <goals>
                    <goal>install-file</goal>
                </goals>
            </execution>
            <!-- if you need to add more than one jar, add more execution blocks here -->
        </executions>
      </plugin>

And add the dependency to the `<dependencies>` section as usual:

     <dependency>
         <groupId>com.my.company</groupId>
         <artifactId>my-library</artifactId>
         <version>0.5.3-SNAPSHOT</version>
     </dependency>

## Migrating to Wikimedia's i18n jQuery plugin {#migrating-to-wikimedias-i18n-jquery-plugin}

### Why are we doing this change? {#why-are-we-doing-this-change-1}

This adds various important localization features, such as the ability to handle plurals or interpolation. This also restores the language fallback (displaying strings in English if they are not available in the target language) which did not work with the previous set up.

### When was the migration made? {#when-was-the-migration-made}

The migration was made between 3.1-beta and 3.1, with this commit: https://github.com/OpenRefine/OpenRefine/commit/22322bd0272e99869ab8381b1f28696cc7a26721

### How to migrate an extension {#how-to-migrate-an-extension-1}

You will need to update your translation files, merging nested objets in one global object, concatenating keys. You can do this by running the following Python script on all your JSON translation files:

    import json
    import sys

    with open(sys.argv[1], 'r') as f:
        j = json.loads(f.read())

    result = {}
    def translate(obj, path):
        res = {}
        if type(obj) == str:
            result['/'.join(path)] = obj
        else:
            for k, v in obj.items():
                new_path = path + [k]
                translate(v, new_path)
 
    translate(j, [])

    with open(sys.argv[1], 'w') as f:
        f.write(json.dumps(result, ensure_ascii=False, indent=4))

Then your javascript files which retrieve the translated strings should be updated: `$.i18n._('core-dialogs')['cancel']` becomes `$.i18n('core-dialogs/cancel')`. You can do this with the following `sed` script:

     sed -i "s/\$\.i18n._(['\"]\([A-Za-z0-9/_\\-]*\)['\"])\[['\"]\([A-Za-z0-9\-\_]*\)[\"']\]/$.i18n('\1\/\2')/g" my_javascript_file.js

You can then chase down the places where you are concatenating translated strings, and replace that with more flexible patterns using [the plugin's features](https://github.com/wikimedia/jquery.i18n#jqueryi18n-plugin).

## Migrating from org.json to Jackson {#migrating-from-orgjson-to-jackson}

### Why are we doing this change? {#why-are-we-doing-this-change-2}

The org.json (or json-java) library has multiple drawbacks.
* First, it has limited functionality - all the serialization and deserialization has to be done explicitly - an important proportion of OpenRefine's code was dedicated to implementing these;
* Second, its implementation is not optimized for speed - multiple projects have reported speedups when migrating to more modern JSON libraries;
* Third, and this was the decisive factor to initiate the migration: [its license](https://json.org/license) is the MIT license with an additional condition which makes it non-free. Getting rid of this dependency was required by the Software Freedom Conservancy as a prerequisite to become a fiscal sponsor for the project.

### When was the migration made? {#when-was-the-migration-made-1}

This change was made between 3.1 and 3.2-beta, with this commit: https://github.com/OpenRefine/OpenRefine/commit/5639f1b2f17303b03026629d763dcb6fef98550b

### How to migrate an extension or fork {#how-to-migrate-an-extension-or-fork}

You will need to use the Jackson library to serialize the classes that implement interfaces or extend classes exposed by OpenRefine.
The interface `Jsonizable` was deleted. Any class that used to implement this now needs to be serializable by Jackson, producing the same format as the previous serialization code. This applies to any operation, facet, overlay model or GREL function. If you are new to Jackson, have a look at [this tutorial](https://www.baeldung.com/jackson) to learn how to annotate your class for serialization. Once this is done, you can remove the `void write(JSONWriter writer, Properties options)` method from your class. Note that it is important that you do this migration for all classes implementing the `Jsonizable` interface that are exposed to OpenRefine's core.

We encourage you to migrate out of org.json completely, but this is only required for the classes that interact with OpenRefine's core.

#### General notes about migrating {#general-notes-about-migrating}

OpenRefine's ObjectMapper is available at `ParsingUtilities.mapper`. It is configured to only serialize the fields and getters that have been explicitly marked with `@JsonProperty` (to avoid accidental JSON format changes due to refactoring). On deserialization it will ignore any field in the JSON payload that does not correspond to a field in the Java class. It has serializers and deserializers  for `OffsetDateTime` and `LocalDateTime`.

Useful snippets to use in tests:
* deserialize an instance: `MyClass instance = ParsingUtilities.mapper.readValue(jsonString, MyClass.class);` (replaces calls to `Jsonizable.write`);
* serialize an instance: `String json = ParsingUtilities.mapper.writeValueAsString(myInstance);` (replaces calls to static methods such as `load`, `loadStreaming` or `reconstruct`);
* the equivalent of `JSONObject` is `ObjectNode`, the equivalent of `JSONArray` is `ArrayNode`;
* create an empty JSON object: `ParsingUtilities.mapper.createObjectNode()` (replaces `new JSONObject()`);
* create an empty JSON array: `ParsingUtilities.mapper.createArrayNode()` (replaces `new JSONArray()`).

Before undertaking the migration, we recommend that you write some tests which serialize and deserialize your objects. This will help you make sure that the JSON format is preserved during the migration. One way to do this is to collect some sample JSON representations of your objects, and check in your tests that deserializing these JSON payloads and serializing them back to JSON preserves the JSON payload. Some utilities are available to help you with that in [`TestUtils`](https://github.com/OpenRefine/OpenRefine/blob/master/main/tests/server/src/com/google/refine/tests/util/TestUtils.java) (we had [some to test org.json serialization](https://github.com/OpenRefine/OpenRefine/blob/3.1/main/tests/server/src/com/google/refine/tests/util/TestUtils.java) before we got rid of the dependency, feel free to copy them).

#### For functions {#for-functions}

Before the migration, you had to explicitly define JSON serialization of functions with a `write` method. You should now override the getters returning the various documentation fields.

Example: `Cos` function [before](https://github.com/OpenRefine/OpenRefine/blob/3.1/main/src/com/google/refine/expr/functions/math/Cos.java) and [after](https://github.com/OpenRefine/OpenRefine/blob/master/main/src/com/google/refine/expr/functions/math/Cos.java).

#### For operations {#for-operations}

Before the JSON migration we refactored engine-dependent operations so that the engine configuration is represented by an `EngineConfig` object instead of a `JSONObject`. Therefore the constructor for your operation should be updated to use this new class. Your constructor should also be annotated to be used during deserialization.

Note that you do not need to explicitly serialize the operation type, this is already done for you by `AbstractOperation`.

Example: `ColumnRemovalOperation` [before](https://github.com/OpenRefine/OpenRefine/blob/3.1/main/src/com/google/refine/operations/column/ColumnRemovalOperation.java) and [after](https://github.com/OpenRefine/OpenRefine/blob/master/main/src/com/google/refine/operations/column/ColumnRemovalOperation.java).

#### For changes {#for-changes}

Changes are serialized in plain text but often relies on JSON serialization for parts of the data. Just use the methods above with `ParsingUtilities.mapper` to maintain this behaviour.

Example: `ReconChange` [before](https://github.com/OpenRefine/OpenRefine/blob/3.1/main/src/com/google/refine/model/changes/ReconChange.java) and [after](https://github.com/OpenRefine/OpenRefine/blob/master/main/src/com/google/refine/operations/column/ColumnRemovalOperation.java).

#### For importers {#for-importers}

The importing options have been migrated from `JSONObject` to `ObjectNode`. Your compiler should help you propagate this change. Utility functions in `JSONUtilities` have been migrated to Jackson so you should have minimal changes if you used them.

Example: `TabularImportingParserBase` [before](https://github.com/OpenRefine/OpenRefine/blob/3.1/main/src/com/google/refine/importers/TabularImportingParserBase.java) and [after](https://github.com/OpenRefine/OpenRefine/blob/master/main/src/com/google/refine/importers/TabularImportingParserBase.java).

#### For overlay models {#for-overlay-models}

Migrate serialization and deserialization as for other objects.

Example: `WikibaseSchema` [before](https://github.com/OpenRefine/OpenRefine/blob/3.1/extensions/wikidata/src/org/openrefine/wikidata/schema/WikibaseSchema.java#L203) and [after](https://github.com/OpenRefine/OpenRefine/blob/master/extensions/wikidata/src/org/openrefine/wikidata/schema/WikibaseSchema.java#L60)

#### For preference values {#for-preference-values}

Any class that is stored in OpenRefine's preference now needs to implement the `com.google.refine.preferences.PreferenceValue` interface. The static `load` method and the `write` method used previously for deserialization should be deleted and regular Jackson serialization and deserialization should be implemented instead. Note that you do not need to explicitly serialize the class name, this is already done for you by the interface.

Example: `TopList` [before](https://github.com/OpenRefine/OpenRefine/blob/3.1/main/src/com/google/refine/preference/TopList.java) and [after](https://github.com/OpenRefine/OpenRefine/blob/master/main/src/com/google/refine/preference/TopList.java)
