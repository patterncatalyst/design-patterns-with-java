#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/14-singleton standalone and runs ShippingQuoteDemo, which
# exercises the enum singleton, the holder-idiom singleton, the
# hidden-dependency WarehousePricingJob, and the DI-based
# ShippingQuoteService side by side. Run from this directory. No
# containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShippingQuoteDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch14.shipping.ShippingQuoteDemo"
