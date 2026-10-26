#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/16-decorator-proxy standalone and runs ShippingDemo, which
# runs the decorator stack two ways (order swapped) and both proxies against
# the same shipment. Run from this directory. No containers: local Maven +
# JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShippingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch16.shipping.ShippingDemo"
