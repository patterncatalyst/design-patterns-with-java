#!/bin/sh
# Usage: scripts/build.sh
#
# Build every example under examples/ via the aggregator pom.xml, skipping
# tests (use scripts/verify.sh to run them). No containers — local JDK 25
# + Maven only. Run scripts/check-prereqs.sh first if you haven't verified
# the toolchain.

set -eu

repo_root="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> mvn -f examples/pom.xml package (tests skipped)"
mvn -f "${repo_root}/examples/pom.xml" -q -DskipTests package

echo "Build complete. Run scripts/verify.sh to run the test suite."
