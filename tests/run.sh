#!/bin/bash
# Compiles and runs the plain-Kotlin logic tests. Needs kotlinc 1.9.x on the path (or KOTLINC set).
set -e
cd "$(dirname "$0")/.."
SRC=app/src/main/java/com/spotapp/personal
KC=${KOTLINC:-kotlinc}
OUT=$(mktemp -d)
$KC -nowarn -jvm-target 17 -d "$OUT" \
  $SRC/Validate.kt $SRC/Dates.kt $SRC/Format.kt $SRC/Voice.kt $SRC/Model.kt $SRC/Json.kt $SRC/BackupParse.kt \
  $SRC/DragOrder.kt $SRC/LogGroups.kt $SRC/StatsLogic.kt $SRC/PhoneSnapshot.kt tests/CoreTest.kt
java -cp "$OUT:$($KC -version >/dev/null 2>&1; dirname $(which $KC))/../lib/kotlin-stdlib.jar" CoreTestKt
