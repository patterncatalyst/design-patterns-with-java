#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/24-virtual-threads standalone and runs ShippingDemo, which
# fans a shipping-rate quote out to eight simulated carriers twice: once
# unbounded (one virtual thread per carrier, no concurrency cap) and once
# bounded to three in-flight calls at a time. Run from this directory. No
# containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShippingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch24.shipping.ShippingDemo"
