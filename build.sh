#!/bin/bash
echo "=== Pal Builder (JavaFX + AtlantaFX + Ikonli + AnimateFX) ==="
echo ""

mkdir -p out

JAVAFX_PATH="javafx-sdk/lib"
LIBS="libs/atlantafx-base-2.0.1.jar:libs/ikonli-core-12.3.1.jar:libs/ikonli-javafx-12.3.1.jar:libs/ikonli-materialdesign2-pack-12.3.1.jar:libs/AnimateFX-1.3.0.jar"

echo "Compiling all sources with JavaFX + Libraries..."
javac -encoding UTF-8 --module-path "$JAVAFX_PATH" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "$LIBS" -d out src/model/*.java src/server/*.java src/server/api/*.java src/client/*.java src/ui/*.java

echo "Copying CSS assets..."
cp src/ui/*.css out/

echo "Copying library JARs to output..."
mkdir -p out/libs
cp libs/*.jar out/libs/

if [ $? -eq 0 ]; then
    echo ""
    echo "BUILD SUCCESSFUL!"
    echo ""
    echo "To run the SERVER:"
    echo "  cd out && java server.PalServer"
    echo ""
    echo "To run the CLIENT:"
    echo "  cd out && java --module-path '../javafx-sdk/lib' --add-modules javafx.controls,javafx.fxml,javafx.media -cp '.:libs/*' client.PalClient"
    echo ""
else
    echo ""
    echo "BUILD FAILED. Check the errors above."
fi
