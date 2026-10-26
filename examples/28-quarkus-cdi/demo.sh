#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/28-quarkus-cdi standalone and runs its @QuarkusTest suite.
# @QuarkusTest boots the Arc CDI container in-process for every test class;
# there is no packaged jar, no `quarkus dev`, and no container anywhere in
# this loop. The module declares only quarkus-arc as a runtime extension, so
# there is nothing here for Quarkus Dev Services to start: no datasource, no
# Kafka, no messaging. Run from this directory. No containers, no Docker:
# local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn test (boots Arc in-process; no Docker, no Dev Services)"
mvn test
