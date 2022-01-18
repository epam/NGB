#!/bin/bash

echo "Starting NGB build"
_build_number="${APPVEYOR_BUILD_NUMBER}"
if [ "${APPVEYOR_REPO_COMMIT}" ]; then
  _build_number="${_build_number}.${APPVEYOR_REPO_COMMIT}"
fi
./gradlew buildJar buildCli buildDoc buildDocker -PbuildNumber=${_build_number} -PnoTest