#!/bin/bash
#
# Builds everything a release publishes, into dist/:
#
#   catgenome.jar        the H2 flavour, under its historical name
#   catgenome-h2.jar     the same jar
#   catgenome-psql.jar   the PostgreSQL flavour
#   ngb-cli.tar.gz       the command line client
#   ngb-docs.tar.gz      the mkdocs site
#
# and, on a release/* branch, the ngb:latest docker image.
#
# It used to read APPVEYOR_REPO_BRANCH, APPVEYOR_BUILD_NUMBER and APPVEYOR_REPO_COMMIT directly,
# which meant only AppVeyor could run it as intended. The same three values now come from the
# environment under CI-neutral names, and fall back to git, so the script runs by hand:
#
#   BRANCH_NAME    branch being built; release/* also builds the docker image. Default: git.
#   BUILD_NUMBER   appended to the version, e.g. 3.0.0.42. Default: empty, i.e. plain 3.0.0.
#   COMMIT_SHA     appended after the build number. Shown in the UI; not in artifact names,
#                  which publish.sh derives from `gradle :printVersion` without it. Default: git.
#   BUILD_DOCKER   set to `buildDocker` to force the image or to empty to suppress it; overrides
#                  the branch check. .github/workflows/build.yml sets it empty because it builds
#                  and smoke-tests the image itself, from the jar this script has just produced.

set -eu

BRANCH_NAME="${BRANCH_NAME:-$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)}"
BUILD_NUMBER="${BUILD_NUMBER:-}"
COMMIT_SHA="${COMMIT_SHA:-$(git rev-parse HEAD 2>/dev/null || echo '')}"

echo "Starting NGB build of ${BRANCH_NAME} (build number '${BUILD_NUMBER}')"

if [ -z "${BUILD_DOCKER+x}" ]; then
  BUILD_DOCKER=
  case "$BRANCH_NAME" in
    release/*)
      echo "Building with docker distribution"
      BUILD_DOCKER=buildDocker
      ;;
  esac
fi

_build_number="${BUILD_NUMBER}"
if [ -n "${COMMIT_SHA}" ]; then
  # Keep the leading dot out when there is no build number, or the version reads "3.0.0..<sha>".
  _build_number="${_build_number:+${_build_number}.}${COMMIT_SHA}"
fi

# publish.sh versions every file it finds in dist/, so anything left there by an earlier run of this
# script would be published as part of this one.
rm -rf dist

# 1. Build JAR with H2 support and "everything" else (catgenome-h2.jar)
# 2. Rebuild JAR with PSQL support only (catgenome-psql.jar)
# 3. H2 version is also stored as `catgenome.jar` for backward compatability, if any service uses that
# shellcheck disable=SC2086  # $BUILD_DOCKER is a task name or nothing at all, so it cannot be quoted
./gradlew buildJar buildCli buildDoc $BUILD_DOCKER -PbuildNumber="${_build_number}" -PnoTest
mv dist/catgenome.jar dist/catgenome-h2.jar
./gradlew buildJar -PbuildNumber="${_build_number}" -PnoTest -Pdatabase=postgres
mv dist/catgenome.jar dist/catgenome-psql.jar
cp dist/catgenome-h2.jar dist/catgenome.jar

echo "Built:"
ls -l dist
