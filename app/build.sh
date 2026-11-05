#!/usr/bin/env bash
# Builds Enough Procrastination with the plain JDK (no Maven/Gradle needed).
#   ./build.sh          compile + jar -> out/enough-procrastination.jar
#   ./build.sh run      build, then start the app
#   ./build.sh demo     build, then start with sample data
#   ./build.sh docs     build + Javadoc API manual -> ../docs/api
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out/classes
find src/main/java -name '*.java' > out/sources.txt
javac -encoding UTF-8 -Xlint:all,-serial -d out/classes @out/sources.txt
jar --create --file out/enough-procrastination.jar --main-class ep.Main -C out/classes .
case "${1:-}" in
  run)  java -jar out/enough-procrastination.jar ;;
  demo) java -jar out/enough-procrastination.jar --demo ;;
  docs) rm -rf ../docs/api
        javadoc -encoding UTF-8 -charset UTF-8 -quiet -Xdoclint:none -d ../docs/api \
          -windowtitle 'Enough Procrastination API' \
          -doctitle 'Enough Procrastination &mdash; API Manual (beta)' \
          -sourcepath src/main/java -subpackages ep ;;
esac
