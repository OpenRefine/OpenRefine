#!/usr/bin/env bash
set -e

GROUP1=cypress/integration/open-project/*.spec.js
GROUP2=cypress/integration/language/*.spec.js
echo "::set-output name=matrix::{\"browser\":[\"chrome\", \"edge\"], \"machines\":[1,2,3]}"