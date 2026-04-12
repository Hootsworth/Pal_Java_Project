#!/bin/bash
# LAN Social Media - Build Script
# Compiles all Java source files into the out/ directory

echo "=== Pal Builder (JavaFX) ==="

# Clean and create output directory
rm -rf out
mkdir -p out

# Collect all .java files
SOURCES=$(find src -name "*.java")

echo "Compiling sources..."
javac -d out $SOURCES

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "To run the SERVER, open a new terminal and type:"
    echo "  cd out && java server.PalServer"
    echo ""
    echo "To run the CLIENT, open another terminal and type:"
    echo "  cd out && java --module-path ../javafx-sdk/lib --add-modules javafx.controls,javafx.fxml,javafx.media client.PalClient"
    echo ""
    echo "Multiple clients can connect — just run the client command multiple times!"
else
    echo "❌ Build failed. Check errors above."
fi
