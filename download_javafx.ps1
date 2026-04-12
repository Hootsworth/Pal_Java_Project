$ErrorActionPreference = "Stop"
$url = "https://download2.gluonhq.com/openjfx/17.0.10/openjfx-17.0.10_windows-x64_bin-sdk.zip"
$zipPath = ".\javafx.zip"
$extractPath = ".\"

Write-Host "Downloading JavaFX 17 SDK..."
Invoke-WebRequest -Uri $url -OutFile $zipPath

Write-Host "Extracting..."
Expand-Archive -Path $zipPath -DestinationPath $extractPath -Force

Write-Host "Tidy up..."
Remove-Item $zipPath
Rename-Item -Path ".\javafx-sdk-17.0.10" -NewName "javafx-sdk" -ErrorAction SilentlyContinue

Write-Host "Done!"
