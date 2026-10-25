#!/bin/sh
# Usage: scripts/verify.sh
#
# Run the JUnit 5 suite for every example via the aggregator pom.xml.
# Run scripts/build.sh first if you want a package step before testing;
# `mvn test` alone also compiles what it needs.

set -eu

repo_root="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> mvn -f examples/pom.xml test"
mvn -f "${repo_root}/examples/pom.xml" -q test
