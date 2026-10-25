#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/07-testing-with-junit standalone and runs Main, which
# books shipments through ShipmentBookingService the same way the test
# suite does: one path that succeeds, one rejected by ShipmentValidator,
# and one rejected because the ledger is at capacity. Run from this
# directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running Main"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch07.booking.Main"
