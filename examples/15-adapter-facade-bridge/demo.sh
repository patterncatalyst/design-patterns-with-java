#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/15-adapter-facade-bridge standalone and runs ShippingDemo,
# which exercises the Adapter, the Facade, and the Bridge against one
# GlobalCourier integration. Run from this directory. No containers: local
# Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShippingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch15.shipping.ShippingDemo"
