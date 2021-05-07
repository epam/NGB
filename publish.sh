#!/bin/bash

echo "Starting deployment"

# Get current version
NGB_VERSION=$(./gradlew :printVersion -PbuildNumber=$APPVEYOR_BUILD_NUMBER |  grep "Project version is " | sed 's/^.*is //')
echo "Current version is ${NGB_VERSION}"

cd dist
echo "Creating ${NGB_VERSION} distribution"
mkdir -p ${NGB_VERSION}
for file in *
do 
    [[ -d $file ]] && continue
    ext="${file#*.}"
    filename="${file%%.*}"
    versioned_file=${filename}-${NGB_VERSION}.${ext}
    cp -rf "$file" "${NGB_VERSION}/${versioned_file}"
    if [[ $versioned_file == *"ngb-docs"* ]]; then
        DOCS_VERSION=$versioned_file
    fi
done

echo "Publishing ${NGB_VERSION} distribution"

aws s3 cp ${NGB_VERSION} s3://ngb-oss-builds/public/builds/${APPVEYOR_REPO_BRANCH}/${NGB_VERSION}/ --recursive

docker inspect --type=image "ngb:latest" &> /dev/null
if [ $? -ne 0 ]; then
    echo "Docker image is not built"
    exit 0
fi

echo "branch: $APPVEYOR_PULL_REQUEST_HEAD_REPO_BRANCH"

if [[ "$APPVEYOR_PULL_REQUEST_HEAD_REPO_BRANCH" == "develop" ]] || [[ "$APPVEYOR_PULL_REQUEST_HEAD_REPO_BRANCH" == "release/"* ]]; then
  echo "Publishing into Docker repository: $DOCKER_USER"
  docker login -u $DOCKER_USER -p $DOCKER_PSWD
  docker tag ngb:latest $DOCKER_USER/ngb:$NGB_VERSION
  docker push $DOCKER_USER/ngb:$NGB_VERSION
fi

