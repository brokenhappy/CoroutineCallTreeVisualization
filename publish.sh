#!/usr/bin/env bash

# Shell wrapper for the publisher task
# This script ensures the publisher runs from the correct working directory (repo root)

set -e

# Get the directory where this script is located (repo root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Change to the repo root
cd "$SCRIPT_DIR"

# Run the publisher Gradle task with all arguments passed through
./gradlew :publisher:run --args="$*"
