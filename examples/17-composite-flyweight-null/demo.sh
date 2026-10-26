#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/17-composite-flyweight-null standalone and runs
# ShipmentDemo, which exercises the Composite tree, the Flyweight pool, and
# the Null Object against one small shipment. Run from this directory. No
# containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShipmentDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch17.shipping.ShipmentDemo"
