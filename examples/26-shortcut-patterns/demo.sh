#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/26-shortcut-patterns standalone and runs
# ShortcutDispatchDemo, which routes a short sequence of events through both
# dispatch shortcuts side by side. Run from this directory. No containers:
# local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running ShortcutDispatchDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch26.shipping.ShortcutDispatchDemo"
