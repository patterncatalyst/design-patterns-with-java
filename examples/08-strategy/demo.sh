#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/08-strategy standalone and runs ShippingDemo, which prices
# the same Shipment with all three Strategy expressions side by side. Run
# from this directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShippingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch08.shipping.ShippingDemo"
