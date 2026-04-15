#!/bin/bash
set -e

if [ ! -d "out" ]; then
  if [ -x "dist/PalClient/PalClient" ]; then
    ./dist/PalClient/PalClient
    exit 0
  fi

  echo "[ERROR] out/ folder not found. Run ./build.sh first."
  exit 1
fi

cd out
java --module-path "../javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml,javafx.media -cp ".:libs/*" client.PalClient
