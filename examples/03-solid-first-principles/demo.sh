#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/03-solid-first-principles standalone and runs BillingDemo,
# which prices one freight invoice three ways: the SRP split (calculate,
# format, persist), the OCP rule catalogue (tax, discount, flat fee), and an
# LSP comparison (a loyalty discount that honors the base contract next to a
# rule that does not). Run from this directory. No containers: local Maven +
# JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running BillingDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch03.billing.BillingDemo"
