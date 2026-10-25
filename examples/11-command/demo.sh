#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/11-command standalone and runs WarehouseDemo, which runs
# every command, the undo/redo stack, the FIFO queue, and the ReceiptLogger
# shortcut end to end against one WarehouseLedger. Run from this directory.
# No containers: local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running WarehouseDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch11.warehouse.WarehouseDemo"
