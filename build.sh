#!/bin/bash

# UniReq Build Script
# Builds the Burp Suite extension JAR file using Maven

set -e  # Exit on any error

echo "🚀 Building UniReq - HTTP Request Deduplicator Extension"
echo "=============================================="

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven is not installed or not in PATH"
    echo "Please install Maven 3.6+ and try again"
    echo "Visit: https://maven.apache.org/install.html"
    exit 1
fi

# Check Java version
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed or not in PATH"
    echo "Please install Java 11+ and try again"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "❌ Error: Java 11 or higher is required (found Java $JAVA_VERSION)"
    echo "Please upgrade your Java installation"
    exit 1
fi

echo "✅ Java $JAVA_VERSION detected"

# Display Maven version
MVN_VERSION=$(mvn -version | head -1)
echo "✅ $MVN_VERSION"

echo ""
echo "🔧 Cleaning previous builds..."
mvn clean -q

echo "📦 Compiling and packaging extension..."
mvn compile package -q

# Check if build was successful
if [ -f "target/unireq-deduplicator-1.0.0.jar" ]; then
    echo ""
    echo "🎉 Build completed successfully!"
    echo "📍 Extension JAR location: target/unireq-deduplicator-1.0.0.jar"
    echo ""
    echo "📋 Next steps:"
    echo "1. Open Burp Suite"
    echo "2. Go to Extensions → Installed"
    echo "3. Click 'Add' and select the JAR file"
    echo "4. Look for the 'UniReq' tab in Burp's interface"
    echo ""
    echo "📊 File size: $(ls -lh target/unireq-deduplicator-1.0.0.jar | awk '{print $5}')"
else
    echo "❌ Build failed - JAR file not found"
    exit 1
fi 