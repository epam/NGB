#!/bin/bash

echo "Starting NGB build"
if [[ "$APPVEYOR_REPO_BRANCH" == "release/"* ]]; then
  echo "Building with docker distribution"
  BUILD_DOCKER=buildDocker
fi

./gradlew buildJar buildCli buildDoc $BUILD_DOCKER -PbuildNumber=${APPVEYOR_BUILD_NUMBER} -PnoTest
