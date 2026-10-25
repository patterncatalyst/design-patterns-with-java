#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/04-dependency-inversion standalone and runs Main, the
# composition root that wires two gateways into three high-level policies
# by hand, with no DI framework. Run from this directory. No containers:
# local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running Main"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch04.notification.Main"
