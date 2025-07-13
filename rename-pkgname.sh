#!/bin/bash

OLD_PACKAGE="me.bmax.apatch"
NEW_PACKAGE="com.valvesoftware.aq"
OLD_PATH=$(echo "$OLD_PACKAGE" | tr '.' '/')
NEW_PATH=$(echo "$NEW_PACKAGE" | tr '.' '/')
JNI_OLD=$(echo "$OLD_PACKAGE" | tr '.' '_')
JNI_NEW=$(echo "$NEW_PACKAGE" | tr '.' '_')

NOSIGNATURE=false
if [ "$1" == "-nosignature" ]; then
    NOSIGNATURE=true
fi

echo "📦 Package name: $OLD_PACKAGE → $NEW_PACKAGE"
echo "📁 Directory: $OLD_PATH → $NEW_PATH"

# 1. Java/Kotlin/AIDL quick change
echo "Changing package contents..."
find . -type f \( -name "*.kt" -o -name "*.java" -o -name "*.aidl" -o -name "build.gradle.kts" \) \
  -exec sed -i "s/$OLD_PACKAGE/$NEW_PACKAGE/g" {} +

# 2. JNI method names
echo "Updating JNI method names..."
find . -type f -name "*.cpp" \
  -exec sed -i "s/Java_${JNI_OLD}_/Java_${JNI_NEW}_/g" {} +

# 3. FindClass strings from JNI contents
echo "🔧 Updating JNI class paths..."
find . -type f -name "*.cpp" \
  -exec sed -i "s|$OLD_PATH|$NEW_PATH|g" {} +

# 4. Move directories of Java/Kotlin files
echo "🚚 Moving Java source files..."
SRC_DIR="app/src/main/java"
NEW_SRC_DIR="$SRC_DIR/$NEW_PATH"
mkdir -p "$NEW_SRC_DIR"
mv "$SRC_DIR/$OLD_PATH"/* "$NEW_SRC_DIR/" 2>/dev/null
rm -rf "$SRC_DIR/$(echo "$OLD_PACKAGE" | cut -d. -f1)" 2>/dev/null

# 5. AIDL sources
echo "📦 Moving AIDL files..."
AIDL_DIR="app/src/main/aidl"
mkdir -p "$AIDL_DIR/$NEW_PATH"
mv "$AIDL_DIR/$OLD_PATH"/* "$AIDL_DIR/$NEW_PATH/" 2>/dev/null
rm -rf "$AIDL_DIR/$(echo "$OLD_PACKAGE" | cut -d. -f1)" 2>/dev/null

# 6. Handle -nosignature parameter
if [ "$NOSIGNATURE" = true ]; then
    echo "🛡️ Disabling signature verification..."
    APATCH_APP_FILE=$(find "$NEW_SRC_DIR" -name "APatchApp.kt" -o -name "APApplication.kt" | head -n 1)
    
    if [ -n "$APATCH_APP_FILE" ]; then
        echo "Found APatchApp file at: $APATCH_APP_FILE"
        sed -i '259s/^/\/\* /' "$APATCH_APP_FILE"
        sed -i '268s/$/ \*\//' "$APATCH_APP_FILE"
        echo "✅ Signature verification disabled in $APATCH_APP_FILE"
    else
        echo "❌ APatchApp.kt file not found in $NEW_SRC_DIR"
        echo "Trying to find in other locations..."
        
        # Alternative search in case the file is in a different path
        ALTERNATE_FILE=$(find "app/src/main/java" -name "APatchApp.kt" -o -name "APApplication.kt" | head -n 1)
        if [ -n "$ALTERNATE_FILE" ]; then
            echo "Found alternate file at: $ALTERNATE_FILE"
            sed -i '259s/^/\/\* /' "$ALTERNATE_FILE"
            sed -i '268s/$/ \*\//' "$ALTERNATE_FILE"
            echo "✅ Signature verification disabled in $ALTERNATE_FILE"
        else
            echo "❌ Could not find APatchApp.kt file anywhere!"
        fi
    fi
fi

# 7. End
echo "✅ Package name changed."
if [ "$NOSIGNATURE" = false ]; then
    echo "You can use the -nosignature parameter to disable apk signature verification"
fi
