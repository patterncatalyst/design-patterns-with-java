#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/22-functional-java standalone and runs FulfillmentDemo,
# which runs a nightly shipment batch through every expression in this
# chapter: enum-carried surcharges, a lambda satisfying the same contract,
# an enum Singleton, a function-parameter Template Method, Command as method
# references and a Runnable, composed pricing adjustments, stream
# pipelines, and Optional. Run from this directory. No containers: local
# Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running FulfillmentDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch22.fulfillment.FulfillmentDemo"
