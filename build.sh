#!/bin/bash

echo "Starting NGB build"
if [[ "$APPVEYOR_REPO_BRANCH" == "release/"* ]]; then
  echo "Building with docker distribution"
  BUILD_DOCKER=buildDocker
fi

_build_number="${APPVEYOR_BUILD_NUMBER}"
if [ "${APPVEYOR_REPO_COMMIT}" ]; then
  _build_number="${_build_number}.${APPVEYOR_REPO_COMMIT}"
fi
./gradlew buildJar buildCli buildDoc $BUILD_DOCKER -PbuildNumber=${_build_number} -PnoTest
