#!/usr/bin/env bash

set -euo pipefail

mysql_port="$1"
postgres_port="$2"
shift 2

if [[ ! "$mysql_port" =~ ^[0-9]+$ || ! "$postgres_port" =~ ^[0-9]+$ ]]; then
  echo "MySQL and PostgreSQL ports must be numeric" >&2
  exit 2
fi

mvn "$@" \
  -Ddatabase.test.excludedGroups= \
  -DmySqlDbName=test_db \
  -DmySqlDbHost=127.0.0.1 \
  -DmySqlDbPort="$mysql_port" \
  -DmySqlDbUser=root \
  -DmySqlDbPassword=root \
  -DmySqlTestTable=test_table \
  -DpgSqlDbName=test_db \
  -DpgSqlDbHost=127.0.0.1 \
  -DpgSqlDbPort="$postgres_port" \
  -DpgSqlDbUser=postgres \
  -DpgSqlDbPassword=postgres \
  -DpgSqlTestTable=test_table \
  -DmariadbDbName=test_db \
  -DmariadbDbHost=127.0.0.1 \
  -DmariadbDbPort="$mysql_port" \
  -DmariadbDbUser=root \
  -DmariadbDbPassword=root \
  -DmariadbTestTable=test_table \
  jacoco:prepare-agent test jacoco:report
