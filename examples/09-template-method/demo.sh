#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/09-template-method standalone and runs
# OrderProcessingDemo, which runs one order through each channel using both
# the abstract-method pipeline and its functional counterpart, side by side.
# Run from this directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running OrderProcessingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch09.shipping.OrderProcessingDemo"
