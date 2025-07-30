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

# 1. Build JAR with H2 support and "everything" else (catgenome-h2.jar)
# 2. Rebuild JAR with PSQL support only (catgenome-psql.jar)
# 3. H2 version is also stored as `catgenome.jar` for backward compatability, if any service uses that
./gradlew buildJar buildCli buildDoc $BUILD_DOCKER -PbuildNumber=${_build_number} -PnoTest && \
mv dist/catgenome.jar dist/catgenome-h2.jar && \
./gradlew buildJar -PbuildNumber=${_build_number} -PnoTest -Pdatabase=postgres && \
mv dist/catgenome.jar dist/catgenome-psql.jar && \
cp dist/catgenome-h2.jar dist/catgenome.jar
