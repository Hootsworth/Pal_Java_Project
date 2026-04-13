@echo off
echo === Pal Builder (JavaFX + AtlantaFX + Ikonli + AnimateFX) ===
echo.

if not exist out mkdir out

set JAVAFX_PATH=javafx-sdk\lib
set LIBS=libs\atlantafx-base-2.0.1.jar;libs\ikonli-core-12.3.1.jar;libs\ikonli-javafx-12.3.1.jar;libs\ikonli-materialdesign2-pack-12.3.1.jar;libs\AnimateFX-1.3.0.jar

echo Compiling all sources with JavaFX + Libraries...
javac -encoding UTF-8 --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "%LIBS%" -d out src\model\*.java src\server\*.java src\server\api\*.java src\client\*.java src\ui\*.java

echo Copying CSS assets...
copy src\ui\*.css out\ >nul 2>&1

echo Copying library JARs to output...
if not exist out\libs mkdir out\libs
copy libs\*.jar out\libs\ >nul 2>&1

if %errorlevel% == 0 (
    echo.
    echo BUILD SUCCESSFUL!
    echo.
    echo To run the SERVER, open a new command prompt and type:
    echo   cd out
    echo   java server.PalServer
    echo.
    echo To run the CLIENT, open another command prompt and type:
    echo   cd out
    echo   java --module-path "..\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml,javafx.media -cp ".;libs\*" client.PalClient
    echo.
) else (
    echo.
    echo BUILD FAILED. Check the errors above.
)
pause
