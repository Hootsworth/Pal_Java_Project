@echo off
setlocal

REM Prefer running from freshly compiled out/ classes.
if not exist "out" (
    REM Fallback to packaged launcher only when out/ is unavailable.
    if exist "dist\PalClient\PalClient.exe" (
        start "" "dist\PalClient\PalClient.exe"
        exit /b 0
    )

    echo [ERROR] out\ folder not found. Run build.bat first.
    exit /b 1
)

pushd out
java --module-path "..\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml,javafx.media -cp ".;libs\*" client.PalClient
set ERR=%ERRORLEVEL%
popd

exit /b %ERR%
