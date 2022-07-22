---
id: version-release-process
title: How to do an OpenRefine version release
sidebar_label: How to do an OpenRefine version release
---

When releasing a new version of Refine, the following steps should be followed:

1. Make sure the `master` branch is stable and nothing has broken since the previous version. We need developers to stabilize the trunk and some volunteers to try out `master` for a few days.
2. Change the version number in [RefineServlet.java](http://github.com/OpenRefine/OpenRefine/blob/master/main/src/com/google/refine/RefineServlet.java#L62) and in the POM files using `mvn versions:set -DnewVersion=2.6-beta -DgenerateBackupPoms=false`. Commit the changes.
3. Compose the list of changes in the code and on the wiki. If the issues have been updated with the appropriate milestone, the Github issue tracker should be able to provide a good starting point for this.
4. Tag the release candidate in git and push the tag to Github. For example:
```shell
git tag -a -m "Second beta" 2.6-beta.2
    git push origin --tags
```
5. Create a GitHub release based on that tag, with a summary of the changes as description of the release. Publishing the GitHub release will trigger the generation of the packaged artifacts. The download links can point directly to Maven Central and can be built as follows (replace `3.6-rc1` by your version string):
   * Linux: https://oss.sonatype.org/service/local/artifact/maven/content?r=releases&g=org.openrefine&a=openrefine&v=3.6-rc1&c=linux&p=tar.gz
   * MacOS: https://oss.sonatype.org/service/local/artifact/maven/content?r=releases&g=org.openrefine&a=openrefine&v=3.6-rc1&c=mac&p=dmg
   * Windows without embedded JRE: https://oss.sonatype.org/service/local/artifact/maven/content?r=releases&g=org.openrefine&a=openrefine&v=3.6-rc1&c=win&p=zip
   * Windows with embedded JRE: https://oss.sonatype.org/service/local/artifact/maven/content?r=releases&g=org.openrefine&a=openrefine&v=3.6-rc1&c=win-with-java&p=zip
6. Announce the beta/release candidate for testing
7. Repeat build/release candidate/testing cycle, if necessary.
8. [Update the OpenRefine Homebrew cask](https://github.com/OpenRefine/OpenRefine/wiki/Maintaining-OpenRefine's-Homebrew-Cask) or coordinate an update via the [developer list](https://groups.google.com/forum/#!forum/openrefine-dev)
9. Verify that the correct versions are shown in the widget at [http://openrefine.org/download](http://openrefine.org/download)
10. Announce on the [OpenRefine mailing list](https://groups.google.com/forum/#!forum/openrefine).
11. Update the version in master to the next version number with `-SNAPSHOT` (such as `4.3-SNAPSHOT`)
```shell
mvn versions:set -DnewVersion=4.3-SNAPSHOT
```
12. If releasing a new major or minor version, create a snapshot of the docs, following [Docusaurus' versioning procedure](https://docusaurus.io/docs/versioning).

Apple code signing
==================

We have code signing certificates for our iOS distributions. To use them, follow these steps:
* Request advisory.committee@openrefine.org to be added to the Apple team: you need to provide the email address that corresponds to your AppleID account;
* Create a certificate signing request from your Mac: https://help.apple.com/developer-account/#/devbfa00fef7
* Go to https://developer.apple.com/account/resources/certificates/add and select "Apple Distribution" as certificate type
* Upload the certificate signing request in the form
* Download the generated certificate
* Import this certificate in the "Keychain Access" app on your mac
* The signing workflow can be found in `.github/workflows/snapshot_release.yml`

Currently the signing of our releases is disabled because it is blocked by a dependency ([#4568](https://github.com/OpenRefine/OpenRefine/issues/4568))
