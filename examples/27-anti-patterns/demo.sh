#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/27-anti-patterns standalone and runs ShipmentPricingDemo,
# which prices shipments through the over-engineered design and the
# simplified design side by side. Run from this directory. No containers:
# local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShipmentPricingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch27.shipping.ShipmentPricingDemo"
