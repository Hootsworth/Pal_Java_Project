@echo off
echo === Pal Builder (JavaFX) ===
echo.

if not exist out mkdir out

echo Compiling all sources with JavaFX...
javac -encoding UTF-8 --module-path "javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml,javafx.media -d out src\model\*.java src\server\*.java src\server\api\*.java src\client\*.java src\ui\*.java

echo Copying CSS assets...
copy src\ui\*.css out\ >nul 2>&1

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
    echo   java --module-path "..\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml,javafx.media client.PalClient
    echo.
) else (
    echo.
    echo BUILD FAILED. Check the errors above.
)
pause
