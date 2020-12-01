#!/bin/bash

echo "Starting deployment"

# Get current version
NGB_VERSION=$(./gradlew :printVersion -PbuildNumber=$TRAVIS_JOB_NUMBER |  grep "Project version is " | sed 's/^.*is //')
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

aws s3 cp ${NGB_VERSION} s3://ngb-oss-builds/${APPVEYOR_REPO_BRANCH}/ --recursive
# sudo ssh ${DEMO_USER}@${DEMO_SRV} -o StrictHostKeyChecking=no -i ../demo.pem \
#     "test -d ${DEMO_PATH}/${VERSION} || mkdir -p ${DEMO_PATH}/${VERSION}"

# sudo rsync -rave "ssh -o StrictHostKeyChecking=no -i ../demo.pem" ${VERSION}/* ${DEMO_USER}@${DEMO_SRV}:${DEMO_PATH}/${VERSION}

# sudo ssh ${DEMO_USER}@${DEMO_SRV} -o StrictHostKeyChecking=no -i ../demo.pem \
#     "find ${DEMO_PATH}/* -maxdepth 0  -type d -ctime +60 -not -path "*latest" -exec rm -rf {} \; &&" \
#     "cd ${DEMO_PATH} &&" \
#     "rm -rf ${VERSION}/docs &&" \
#     "mkdir -p ${VERSION}/docs &&" \
#     "tar -zxf ${VERSION}/${DOCS_VERSION} -C ${VERSION}/docs"

# Publish the docker image from develop branch to dockerhub
## Check whether ngb:latest is built
docker inspect --type=image "ngb:latest" &> /dev/null
if [ $? -ne 0 ]; then
    echo "Docker image is not built"
    exit 0
fi

if [[ "$APPVEYOR_REPO_BRANCH" == "develop" ]] || [[ "$APPVEYOR_REPO_BRANCH" == "release/"* ]]; then
  docker login -u $DOCKER_USER -p $DOCKER_PSWD
  docker tag ngb:latest $DOCKER_USER/ngb:$NGB_VERSION
  docker push $DOCKER_USER/ngb:$NGB_VERSION
fi

