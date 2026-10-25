#!/bin/sh
# Usage: scripts/check-prereqs.sh
#
# Verify the local toolchain this book's examples build with, and print the
# version found for each tool. No containers: everything here runs on the
# host (Fedora is the reference platform; see _docs/00-prerequisites.md).
#
# Pinned/floor versions, from the r01 build plan's verified toolchain table:
#   JDK     25 (verified against 25.0.2-tem, via SDKMAN)
#   Maven   >= 3.9 (verified against 3.9.11)
#   JUnit Jupiter / Mockito / AssertJ — pinned in examples/pom.xml's
#   <dependencyManagement>, not checked here.
#
# If JDK 25 isn't installed, the SDKMAN hint below gets you there:
#   curl -s "https://get.sdkman.io" | bash
#   sdk install java 25.0.2-tem
#   sdk use java 25.0.2-tem

set -eu

status=0

have() {
    command -v "$1" >/dev/null 2>&1
}

check_java() {
    if ! have java; then
        echo "MISSING: java (need JDK >= 25). Install via SDKMAN: sdk install java 25.0.2-tem"
        status=1
        return
    fi
    version_line="$(java -version 2>&1 | head -n1)"
    echo "OK: java -> ${version_line}"
    major="$(java -version 2>&1 | head -n1 | sed -n 's/.*"\([0-9]*\)\..*/\1/p')"
    if [ -z "${major}" ]; then
        major="$(java -version 2>&1 | head -n1 | sed -n 's/.*"\([0-9]*\)".*/\1/p')"
    fi
    if [ -n "${major}" ] && [ "${major}" -lt 25 ]; then
        echo "WARNING: java major version ${major} is below the floor of 25"
        status=1
    fi
}

check_maven() {
    if ! have mvn; then
        echo "MISSING: mvn (need Maven >= 3.9). Install via SDKMAN: sdk install maven"
        status=1
        return
    fi
    echo "OK: mvn -> $(mvn -v | head -n1)"
}

echo "Checking local toolchain for Design Patterns with Java..."
echo
check_java
check_maven
echo

if [ "${status}" -eq 0 ]; then
    echo "All prerequisites found."
else
    echo "One or more prerequisites are missing or below the floor version. See _docs/00-prerequisites.md."
fi

exit "${status}"
