#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/05-resource-management standalone and runs
# ResourceManagementDemo, which exercises a try-with-resources normal and
# exception path, the Execute-Around Method idiom around a ledger write,
# and a pooled TrackingFeedHandle resource.
# Run from this directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ResourceManagementDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch05.shipping.ResourceManagementDemo"
