#!/bin/bash
#
# Publishes what build.sh put in dist/: every artifact to
# s3://<bucket>/public/builds/<branch>/<version>/, versioned in its filename, and on a release/*
# branch the ngb:latest image to Docker Hub as <namespace>/ngb:<version>.
#
# As with build.sh the APPVEYOR_* variables are gone; nothing here is specific to a CI provider:
#
#   BRANCH_NAME       the path component under public/builds/. Default: git.
#   BUILD_NUMBER      must be the same value build.sh was given, or the version in the artifact
#                     names will not be the version inside them.
#   S3_BUILDS_URI     default s3://ngb-oss-builds/public/builds
#   DOCKER_NAMESPACE  Docker Hub account or organisation to push into; default lifescience, which
#                     is where https://hub.docker.com/r/lifescience/ngb lives.
#   DOCKERHUB_USERNAME, DOCKERHUB_TOKEN
#                     optional `docker login`. The workflow logs in with docker/login-action and
#                     leaves these unset; a hand run can pass them instead. AppVeyor used
#                     DOCKER_USER/DOCKER_PSWD, and DOCKER_USER doubled as the namespace.
#   NGB_PUBLISH_DRY_RUN=1
#                     print the aws and docker commands instead of running them. This is how the
#                     script gets exercised without credentials - see .devenv/TEST-BASELINE.md.
#
# AWS credentials are not read from here. The workflow assumes an IAM role through GitHub's OIDC
# provider (secret AWS_PUBLISH_ROLE_ARN) and the AWS CLI picks the result up from the environment;
# AppVeyor held a long-lived access key pair in its project settings.

set -eu

BRANCH_NAME="${BRANCH_NAME:-$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)}"
BUILD_NUMBER="${BUILD_NUMBER:-}"
S3_BUILDS_URI="${S3_BUILDS_URI:-s3://ngb-oss-builds/public/builds}"
DOCKER_NAMESPACE="${DOCKER_NAMESPACE:-lifescience}"
DRY_RUN="${NGB_PUBLISH_DRY_RUN:-}"

run() {
    if [ -n "$DRY_RUN" ]; then
        echo "DRY RUN: $*"
    else
        "$@"
    fi
}

echo "Starting deployment"

# Get current version
#   Here we use a "short" version notation, i.e. {major}.{minor}.{patch}.${build}
#   Commint SHA is not included in the artifacts naming. It is shown in the app only (e.g. in the GUI)
NGB_VERSION=$(./gradlew :printVersion -PbuildNumber="$BUILD_NUMBER" | grep "Project version is " | sed 's/^.*is //')
echo "Current version is ${NGB_VERSION}"

cd dist
echo "Creating ${NGB_VERSION} distribution"
mkdir -p "${NGB_VERSION}"
for file in *
do
    [[ -d $file ]] && continue
    ext="${file#*.}"
    filename="${file%%.*}"
    versioned_file=${filename}-${NGB_VERSION}.${ext}
    cp -rf "$file" "${NGB_VERSION}/${versioned_file}"
done

echo "Publishing ${NGB_VERSION} distribution"

run aws s3 cp "${NGB_VERSION}" "${S3_BUILDS_URI}/${BRANCH_NAME}/${NGB_VERSION}/" --recursive

cd ..

if ! docker inspect --type=image "ngb:latest" &> /dev/null; then
    echo "Docker image is not built"
    exit 0
fi

if [[ "$BRANCH_NAME" == "release/"* ]]; then
  if [ -n "${DOCKERHUB_USERNAME:-}" ] && [ -n "${DOCKERHUB_TOKEN:-}" ]; then
    run docker login -u "$DOCKERHUB_USERNAME" --password-stdin <<<"$DOCKERHUB_TOKEN"
  fi
  run docker tag ngb:latest "$DOCKER_NAMESPACE/ngb:$NGB_VERSION"
  run docker push "$DOCKER_NAMESPACE/ngb:$NGB_VERSION"
fi
