#!/bin/bash

echo "Starting NGB build"
if [[ "${{ github.ref_name }}" == "release/"* ]]; then
  echo "Building with docker distribution"
  BUILD_DOCKER=buildDocker
fi

./gradlew buildJar buildCli buildDoc $BUILD_DOCKER -PbuildNumber=${{ github.run_number }} -PnoTest
