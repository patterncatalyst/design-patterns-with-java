#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/10-state-machines standalone and runs ShipmentDemo, which
# drives the enum machine and the sealed-interface machine through the same
# Pack -> Ship -> Deliver sequence, shows an illegal transition being
# refused, and runs the stochastic machine from a fixed seed. Run from this
# directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShipmentDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch10.shipment.ShipmentDemo"
