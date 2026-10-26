#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/33-final-project standalone and runs ShipmentDispatchDemo,
# which runs one shipment through every seam the final project composes:
# Builder resolving a Strategy at open time, State accepting and rejecting
# events, Command driving both State and Strategy with undo, and Observer
# hearing about every legal transition along the way. Run from this
# directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShipmentDispatchDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch33.shipping.ShipmentDispatchDemo"
