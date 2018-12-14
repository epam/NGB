SINGULARITY_FILE=$1
SINGULARITY_IMAGE=$2

if [ -z "$SINGULARITY_FILE" ]; then
  echo "Path to singularity file is not specified, exiting..."
  exit 1
fi

if [ -z "$SINGULARITY_IMAGE" ]; then
  SINGULARITY_IMAGE=$(pwd)/ngb.singularity.img
  echo "Resulting image path is not set, using current directory: $SINGULARITY_IMAGE"
fi

NGB_DOCKER_IMAGE="ngb:latest"
# Check whether ngb:latest is built
docker inspect --type=image $NGB_DOCKER_IMAGE > /dev/null
NGB_DOCKER_BUILT=$?

if [ $NGB_DOCKER_BUILT != 0 ]; then
  echo "$NGB_DOCKER_IMAGE is not found, cannot build singularity image from it, exiting..."
  exit 1
fi

# Setup local docker registry as working with docker locals is not supported by singularity
# And push prebuilt NGB docker image to it
docker run -d -p 5000:5000 --restart=always --name registry registry:2
docker tag $NGB_DOCKER_IMAGE localhost:5000/$NGB_DOCKER_IMAGE
docker push localhost:5000/$NGB_DOCKER_IMAGE

export SINGULARITY_NOHTTPS=1

singularity build "$SINGULARITY_IMAGE" "$SINGULARITY_FILE"
