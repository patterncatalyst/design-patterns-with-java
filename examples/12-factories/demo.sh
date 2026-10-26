#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/12-factories standalone and runs ShippingDemo, which
# processes shipments through the Abstract Factory's regional toolkits and
# inspects shipments through the Factory Method's inspector hierarchy. Run
# from this directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShippingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch12.shipping.ShippingDemo"
