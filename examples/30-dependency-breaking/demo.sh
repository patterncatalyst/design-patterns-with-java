#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/30-dependency-breaking standalone and runs
# ShipmentDispatchDemo, which exercises all four dependency-breaking
# techniques against production wiring at the terminal. Run from this
# directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShipmentDispatchDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch30.shipping.ShipmentDispatchDemo"
