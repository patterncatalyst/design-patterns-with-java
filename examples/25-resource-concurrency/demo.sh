#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/25-resource-concurrency standalone and runs
# DispatchHubDemo, which exercises the connection pool, the monitor object,
# the active object, and the ThreadLocal tracking-code generator against one
# narrative. Run from this directory. No containers: local Maven + JDK 25
# only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running DispatchHubDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch25.dispatch.DispatchHubDemo"
