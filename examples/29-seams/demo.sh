#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/29-seams standalone and runs ShipmentEscalationDemo, which
# exercises the legacy escalation service, the seamed escalation service,
# and the risk scorer's subclass-and-override seam side by side. Run from
# this directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShipmentEscalationDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch29.shipping.ShipmentEscalationDemo"
