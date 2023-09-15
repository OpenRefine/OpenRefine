---
id: version-release-process
title: How to do an OpenRefine version release
sidebar_label: How to do an OpenRefine version release
---

When releasing a new version of Refine, the following steps should be followed:

1. Make sure the `master` branch is stable and nothing has broken since the previous version. We need developers to stabilize the trunk and some volunteers to try out `master` for a few days.
2. Change the version number in [RefineServlet.java](http://github.com/OpenRefine/OpenRefine/blob/master/main/src/com/google/refine/RefineServlet.java#L62) and in the POM files using `mvn versions:set -DnewVersion=2.6-beta -DgenerateBackupPoms=false`. Commit the changes.
3. Compose the list of changes in the code and on the wiki. If the issues have been updated with the appropriate milestone, the Github issue tracker should be able to provide a good starting point for this.
4. Set up build machine. This needs to be Mac OS X or Linux.
5. Download Windows and Mac JREs to bundle them in the Windows and Mac packages from [AdoptOpenJDK](https://adoptopenjdk.net/). You only need the JREs, not the JDKs. Use the lowest version of Java supported (Java 8 currently). Configure the location of these JREs in the `settings.xml` file at the root of the repository. It is important to download recent versions of the JREs as this impacts which HTTPS certificates are accepted by the tool.
6. Insert the production Google credentials in https://github.com/OpenRefine/OpenRefine/blob/bc540a880eceb88e54f85ca43eb54769de3bfa4f/extensions/gdata/src/com/google/refine/extension/gdata/GoogleAPIExtension.java#L36-L39 without committing the changes.
7. [Build the release candidate kits using the shell script (not just Maven)](https://github.com/OpenRefine/OpenRefine/wiki/Building-OpenRefine-From-Source). This must be done on Mac OS X or Linux to be able to build all 3 kits. On Linux you will need to install the `genisoimage` program first. 
```shell
./refine dist 2.6-beta.2
```
To build the Windows version with embedded JRE, use `mvn package -s settings.xml -P embedded-jre -DskipTests=true`.

8. On a Mac machine, compress the Mac `.dmg` (`genisoimage` does not compress it by default) with the following command on a mac machine: `hdiutil convert openrefine-uncompressed.dmg -format UDZO -imagekey zlib-level=9 -o openrefine-3.1-mac.dmg`. If running OS X in a VM, it's probably quicker and more reliable to transfer the kits to the host machine first and then to Github. Finder -> Go -> Connect -> smb://10.0.2.2/. You can then sign the generated DMG file with `codesign -s "Apple Distribution: Code for Science and Society, Inc." openrefine-3.1-mac.dmg`. This requires that you have installed the appropriate certificate on your Mac, see below.

9. Tag the release candidate in git and push the tag to Github. For example:
```shell
git tag -a -m "Second beta" 2.6-beta.2
    git push origin --tags
```
10. Upload the kits to Github releases [https://github.com/OpenRefine/OpenRefine/releases/](https://github.com/OpenRefine/OpenRefine/releases/)  Mention the SHA sums of all uploaded artifacts.
11. Announce the beta/release candidate for testing
12. Repeat build/release candidate/testing cycle, if necessary.
13. Tag the release in git. Build the distributions and upload them. 
14. [Update the OpenRefine Homebrew cask](https://github.com/OpenRefine/OpenRefine/wiki/Maintaining-OpenRefine's-Homebrew-Cask) or coordinate an update via the [developer list](https://groups.google.com/forum/#!forum/openrefine-dev)
15. Verify that the correct versions are shown in the widget at [http://openrefine.org/download](http://openrefine.org/download)
16. Announce on the [OpenRefine mailing list](https://groups.google.com/forum/#!forum/openrefine).
17. Update the version in master to the next version number with `-SNAPSHOT` (such as `4.3-SNAPSHOT`)
```shell
mvn versions:set -DnewVersion=4.3-SNAPSHOT
```
18. If releasing a new major or minor version, create a snapshot of the docs, following [Docusaurus' versioning procedure](https://docusaurus.io/docs/versioning).

Apple code signing
==================

We have code signing certificates for our iOS distributions. To use them, follow these steps:
* Request advisory.committee@openrefine.org to be added to the Apple team: you need to provide the email address that corresponds to your AppleID account;
* Create a certificate signing request from your Mac: https://help.apple.com/developer-account/#/devbfa00fef7
* Go to https://developer.apple.com/account/resources/certificates/add and select "Apple Distribution" as certificate type
* Upload the certificate signing request in the form
* Download the generated certificate
* Import this certificate in the "Keychain Access" app on your mac
* You can now sign code on behalf of the team using the `codesign` utility, such as `codesign -s "Apple Distribution: Code for Science and Society, Inc." openrefine-3.1-mac.dmg`.
