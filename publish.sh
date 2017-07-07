#!/bin/bash
# Script deploys built binaries to a server repository
# The following env vars shall be set
# DEMO_USER
# DEMO_SRV
# DEMO_PATH
# DEMO_KEY

echo "Starting deployment"

# Get current version
NGB_VERSION=$(./gradlew :printVersion -PbuildNumber=$TRAVIS_JOB_ID |  grep "Project version is " | sed 's/^.*is //')
echo "Current version is ${NGB_VERSION}"

# Demo server - binaries

DIST="dist"
cd ${DIST}

VERSIONS=(latest $NGB_VERSION)
for VERSION in ${VERSIONS[*]}
do
    echo "Creating ${VERSION} distribution"
    test -d ${VERSION} || mkdir ${VERSION}
    for file in *
    do 
        if [[ -d $file ]]; then
            continue
        fi
        ext="${file##*.}"
        filename="${file%.*}"
        cp -rf "$file" "${VERSION}/${filename}-${VERSION}.${ext}"
    done

    echo "Publishing ${VERSION} distribution"

    echo -e ${DEMO_KEY} > demo.pem
    sudo chmod 600 demo.pem

    sudo ssh ${DEMO_USER}@${DEMO_SRV} -o StrictHostKeyChecking=no -i demo.pem \
        "test -d ${DEMO_PATH}/${VERSION} || mkdir -P ${DEMO_PATH}/${VERSION}"

    sudo rsync -rave "ssh -o StrictHostKeyChecking=no -i demo.pem" ${VERSION}/* ${DEMO_USER}@${DEMO_SRV}:${DEMO_PATH}/${VERSION}

    sudo ssh ${DEMO_USER}@${DEMO_SRV} -o StrictHostKeyChecking=no -i demo.pem \
        "cd ${DEMO_PATH} &&" \
        "rm -rf ${VERSION}/docs &&" \
        "mkdir -p ${VERSION}/docs &&" \
        "tar -zxf ${VERSION}/${DOCS_VERSION} -C ${VERSION}/docs"

    echo "${VERSION} published"
done
