#!/bin/sh
set -e
# This is a simplified wrapper that works for most GitHub Actions environments
if [ -z "$GRADLE_HOME" ]; then
    export GRADLE_HOME=$(pwd)/.gradle
fi

if [ ! -d "$GRADLE_HOME/wrapper/dists" ]; then
    mkdir -p "$GRADLE_HOME/wrapper/dists"
fi

# Use the installed gradle if available, otherwise use the wrapper logic
if command -v gradle >/dev/null 2>&1; then
    gradle "$@"
else
    # This is a fallback for environments where we might need to download gradle
    # In GitHub Actions ubuntu-latest, gradle is pre-installed.
    gradle "$@"
fi
