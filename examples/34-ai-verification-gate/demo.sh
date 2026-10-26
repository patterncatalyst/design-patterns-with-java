#!/bin/sh
# Usage: ./demo.sh
#
# Builds examples/34-ai-verification-gate standalone and runs
# VerificationGateDemo, which classifies a return request across every
# boundary the return policy specifies using both the drafted (accepted)
# and plausible-wrong (sent-back) strategies, then prices the single day
# they disagree about in dollars. Run from this directory. No containers:
# local Maven + JDK 25 only.

set -eu

script_dir="$(cd "$(dirname "$0")" && pwd)"
cd "${script_dir}"

echo "==> mvn compile"
mvn -q compile

echo
echo "==> running VerificationGateDemo"
mvn -q exec:java -Dexec.mainClass="com.patterncatalyst.designpatterns.ch34.returns.VerificationGateDemo"
