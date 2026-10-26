#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/21-records-sealed-patterns standalone and runs
# ShippingDemo, which rejects an invalid RefrigeratedParcel in its compact
# constructor, computes billable weight for a refrigerated and an oversized
# parcel, describes shipping events through a nested record pattern,
# evaluates a guarded when clause, and quotes a RateRule tree two different
# ways by nesting the same three pieces differently. Run from this
# directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShippingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch21.shipping.ShippingDemo"
