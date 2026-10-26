#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/32-refactor-rewrite standalone and runs Main, which plays
# the strangler fig migration out as three releases: every shipment on the
# legacy calculator, one shipment migrated to the modern calculator, and
# every shipment migrated. Run from this directory. No containers: local
# Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running Main"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch32.accessorials.Main"
