#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/23-idiom-retirement standalone and runs
# IdiomRetirementDemo, which exercises the enum singleton, the records, the
# sealed-interface-plus-switch, and the Optional-based lookup side by side.
# Run from this directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running IdiomRetirementDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch23.shipping.IdiomRetirementDemo"
