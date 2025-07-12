#!/bin/bash

OLD_PACKAGE="me.bmax.apatch"
NEW_PACKAGE="com.valvesoftware.aq"
OLD_PATH=$(echo "$OLD_PACKAGE" | tr '.' '/')
NEW_PATH=$(echo "$NEW_PACKAGE" | tr '.' '/')
JNI_OLD=$(echo "$OLD_PACKAGE" | tr '.' '_')
JNI_NEW=$(echo "$NEW_PACKAGE" | tr '.' '_')

echo "📦 Package name: $OLD_PACKAGE → $NEW_PACKAGE"
echo "📁 Direcotry: $OLD_PATH → $NEW_PATH"

# 1. Java/Kotlin/AIDL quick change
echo "Changing package contents..."
find . -type f \( -name "*.kt" -o -name "*.java" -o -name "*.aidl" -o -name "build.gradle.kts" \) \
  -exec sed -i "s/$OLD_PACKAGE/$NEW_PACKAGE/g" {} +

# 2. JNI method names
echo "Updating JNI method names..."
find . -type f -name "*.cpp" \
  -exec sed -i "s/Java_${JNI_OLD}_/Java_${JNI_NEW}_/g" {} +

# 3. JNI içindeki FindClass string'leri
echo "🔧 JNI sınıf yolları güncelleniyor..."
find . -type f -name "*.cpp" \
  -exec sed -i "s|$OLD_PATH|$NEW_PATH|g" {} +

# 4. Java/Kotlin dosyalarının dizinlerini taşı
echo "🚚 Java kaynak dosyaları taşınıyor..."
SRC_DIR="app/src/main/java"
mkdir -p "$SRC_DIR/$NEW_PATH"
mv "$SRC_DIR/$OLD_PATH"/* "$SRC_DIR/$NEW_PATH/" 2>/dev/null
rm -rf "$SRC_DIR/$(echo "$OLD_PACKAGE" | cut -d. -f1)" 2>/dev/null

# 5. AIDL dosyaları
echo "📦 AIDL kaynak dosyaları taşınıyor..."
AIDL_DIR="app/src/main/aidl"
mkdir -p "$AIDL_DIR/$NEW_PATH"
mv "$AIDL_DIR/$OLD_PATH"/* "$AIDL_DIR/$NEW_PATH/" 2>/dev/null
rm -rf "$AIDL_DIR/$(echo "$OLD_PACKAGE" | cut -d. -f1)" 2>/dev/null

# 6. Final kontrol
echo "✅ Paket adı değişimi tamamlandı. Şimdi 'gradlew clean assembleRelease' ile derleyebilirsin."
