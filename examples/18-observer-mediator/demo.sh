#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/18-observer-mediator standalone and runs
# ObserverMediatorDemo, which exercises the Observer fan-out and the
# Mediator dispatch cascade and prints real output for both. Run from this
# directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ObserverMediatorDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch18.shipping.ObserverMediatorDemo"
