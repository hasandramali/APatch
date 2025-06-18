#!/bin/bash

# Please edit this to change the package name
OLD_PACKAGE="com.valvesoftware.aq"
NEW_PACKAGE="com.example.settings"

OLD_PATH=$(echo "$OLD_PACKAGE" | tr '.' '/')
NEW_PATH=$(echo "$NEW_PACKAGE" | tr '.' '/')

echo "📦 Eski paket: $OLD_PACKAGE"
echo "📦 Yeni paket: $NEW_PACKAGE"
echo "📁 Eski klasör: $OLD_PATH"
echo "📁 Yeni klasör: $NEW_PATH"

echo "🔄 Package name changing..."
find . -type f \( -name "*.kt" -o -name "*.java" -o -name "*.cpp" -o -name "*.aidl" \) -exec sed -i "s/$OLD_PACKAGE/$NEW_PACKAGE/g" {} +

JNI_OLD=$(echo "$OLD_PACKAGE" | tr '.' '_')
JNI_NEW=$(echo "$NEW_PACKAGE" | tr '.' '_')
find . -type f -name "*.cpp" -exec sed -i "s/Java_${JNI_OLD}_/Java_${JNI_NEW}_/g" {} +

echo "Moving source files to the new directory..."

# Java/Kotlin files
SRC_DIR="app/src/main/java"
mkdir -p "$SRC_DIR/$NEW_PATH"
mv "$SRC_DIR/$OLD_PATH"/* "$SRC_DIR/$NEW_PATH/" 2>/dev/null
rm -rf "$SRC_DIR/$(echo "$OLD_PACKAGE" | cut -d. -f1)" 2>/dev/null

# AIDL sources
AIDL_DIR="app/src/main/aidl"
mkdir -p "$AIDL_DIR/$NEW_PATH"
mv "$AIDL_DIR/$OLD_PATH"/* "$AIDL_DIR/$NEW_PATH/" 2>/dev/null
rm -rf "$AIDL_DIR/$(echo "$OLD_PACKAGE" | cut -d. -f1)" 2>/dev/null

# Gradle settings
echo "⚙️ Updating gradle namespace..."
sed -i "s/namespace = \"$OLD_PACKAGE\"/namespace = \"$NEW_PACKAGE\"/g" app/build.gradle.kts

echo "✅ The package name was successfully changed."
