#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/06-encapsulation standalone and runs ShippingDemo, which
# exercises a factory-built gateway, a cached static-factory value, and
# defensive copying against a shared shipment. Run from this directory. No
# containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShippingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch06.shipping.ShippingDemo"
