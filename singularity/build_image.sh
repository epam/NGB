SINGULARITY_FILE=$1
SINGULARITY_IMAGE=$2
SINGULARITY_WD=${3:-dist}

if [ -z "$SINGULARITY_FILE" ]; then
  echo "Path to singularity file is not specified, exiting..."
  exit 1
fi

if [ -z "$SINGULARITY_IMAGE" ]; then
  SINGULARITY_IMAGE=$(pwd)/ngb.singularity.img
  echo "Resulting image path is not set, using current directory: $SINGULARITY_IMAGE"
fi

if [ ! -d $SINGULARITY_WD ]; then
  mkdir -p $SINGULARITY_WD
  echo "Working dir $SINGULARITY_WD does not exist. Created one"
fi

cd $SINGULARITY_WD
echo "Current working dir is $SINGULARITY_WD"

singularity build "$SINGULARITY_IMAGE" "$SINGULARITY_FILE"

cd -
