#!/usr/bin/env bash

OR_VERSION=${env.OR_VERSION}

RELEASE_REPO_OWNER=${env.RELEASE_REPO_OWNER}

RELEASE_REPO_TOKEN=${env.RELEASE_REPO_TOKEN}


# Build Release Message
API_JSON=$(printf '{"tag_name": "%s","target_commitish": "master","name": "%s","body": "Release of version %s","draft": false,"prerelease": false}' ${OR_VERSION} ${OR_VERSION} ${OR_VERSION} )

# Create Release
echo $( curl --silent --data "${API_JSON}" "https://api.github.com/repos/${RELEASE_REPO_OWNER}/OpenRefine-nightly-releases/releases?access_token=${RELEASE_REPO_TOKEN}" | jq -r '.upload_url' )
