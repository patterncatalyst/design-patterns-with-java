#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/13-builder-prototype standalone and runs ShippingDemo,
# which assembles a Shipment through Shipment.Builder, rejects an invalid
# combination, derives a changed CustomsDeclaration with a wither method,
# and clones independent packages from a PackagingCatalog. Run from this
# directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShippingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch13.shipping.ShippingDemo"
