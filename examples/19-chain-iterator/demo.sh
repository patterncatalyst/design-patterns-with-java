#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/19-chain-iterator standalone and runs ChainIteratorDemo,
# which routes four shipments through both chain forms and walks a
# zone-bucketed manifest through both the Iterator and the Stream form. Run
# from this directory. No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ChainIteratorDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch19.shipping.ChainIteratorDemo"
