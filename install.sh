#!/bin/bash
set -e

# Default CAMEO_HOME if not provided as environment variable
CAMEO_HOME=${CAMEO_HOME:-"/workspace/MSOSA2026xHF1"}

echo "Installing Cameo HTTP Server Plugin..."
echo "CAMEO_HOME: $CAMEO_HOME"

# 1. Define target directory
TARGET_DIR="$CAMEO_HOME/plugins/com.haarer.httpserver"
BUILD_DIST="build/plugin-dist/com.haarer.httpserver"

# 2. Create target directory if it doesn't exist
mkdir -p "$TARGET_DIR"

# 3. Copy files
echo "Deploying files to $TARGET_DIR..."
cp -r "$BUILD_DIST"/* "$TARGET_DIR/"

# 4. Copy scripts
echo "Deploying scripts to $TARGET_DIR/scripts..."
mkdir -p "$TARGET_DIR/scripts"
cp -r scripts/* "$TARGET_DIR/scripts/"

echo "Installation successful!"
echo "Please restart Cameo to load the plugin."
