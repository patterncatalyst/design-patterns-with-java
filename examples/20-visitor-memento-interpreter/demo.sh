#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/20-visitor-memento-interpreter standalone and runs
# ShippingDemo, which prices a manifest with all three Visitor operations,
# edits and restores a ManifestDraft through Memento, and evaluates two
# differently-nested SurchargeExpr trees through Interpreter. Run from this
# directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShippingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch20.shipping.ShippingDemo"
