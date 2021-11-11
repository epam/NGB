#!/bin/bash

echo "Starting NGB build"
if [[ "${GITHUB_REF##*/}" == "develop/"* ]]; then
  echo "Building with docker distribution"
  BUILD_DOCKER=buildDocker
fi

./gradlew buildJar buildCli buildDoc $BUILD_DOCKER -PbuildNumber=test -PnoTest
