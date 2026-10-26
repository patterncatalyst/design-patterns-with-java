#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/31-characterization standalone and runs Main, which prices
# the same shipments through LegacyFreightQuoteCalculator and the
# Fowler-refactored FreightQuoteCalculator side by side, then prints the
# 9.99kg/10.0kg weight-tier cliff the characterization suite pins. Run from
# this directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running Main"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch31.shipping.Main"
