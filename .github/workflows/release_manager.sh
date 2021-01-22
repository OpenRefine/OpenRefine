#!/usr/bin/env bash


# Build Release Message
API_JSON=$(printf '{"tag_name": "%s","target_commitish": "master","name": "OpenRefine snapshot %s","body": "This is a snapshot of the development version of OpenRefine, made on %s.\\n\\nThis contains the latest new features and bug fixes, but might not have been tested as thoroughly as official releases.\\n\\nMake sure you [back up your workspace](https://docs.openrefine.org/manual/installing#back-up-your-data) to avoid data loss and report any issues found with this version on [the mailing list](https://groups.google.com/forum/#!forum/openrefine).","draft": false,"prerelease": true}' ${OR_VERSION} ${OR_VERSION} "$(date -u +"%c")" )

# Create Release
echo $( curl --silent -H "Authorization: token ${RELEASE_REPO_TOKEN}" --data "${API_JSON}" "https://api.github.com/repos/${RELEASE_REPO_OWNER}/OpenRefine-snapshot-releases/releases" | jq -r '.upload_url' )
