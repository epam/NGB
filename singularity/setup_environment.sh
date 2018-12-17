set -e

SINGULARITY_VERSION=${1:-master}


if [[ "$SINGULARITY_VERSION" == "3."* ]] || [[ "$SINGULARITY_VERSION" == "master" ]]; then
  # 1. Install singlularity
  ## 1.1 Install deps
  sudo apt-get update -q && \
  sudo apt-get install -q -y build-essential \
                          libssl-dev \
                          uuid-dev \
                          libgpgme11-dev \
                          squashfs-tools \
                          libseccomp-dev \
                          pkg-config

  ## 1.2 Install GO
  export VERSION=1.11 OS=linux ARCH=amd64
  cd /tmp
  wget -q https://dl.google.com/go/go$VERSION.$OS-$ARCH.tar.gz
  sudo tar -C /usr/local -xzf go$VERSION.$OS-$ARCH.tar.gz
  echo 'export GOPATH=${HOME}/go' >> ~/.bashrc
  echo 'export PATH=/usr/local/go/bin:${PATH}:${GOPATH}/bin' >> ~/.bashrc
  source ~/.bashrc
  mkdir -p ${GOPATH}/bin
  curl https://raw.githubusercontent.com/golang/dep/master/install.sh | sh

  ## 1.3 Clone singularity
  mkdir -p $GOPATH/src/github.com/sylabs
  cd $GOPATH/src/github.com/sylabs
  git clone https://github.com/sylabs/singularity.git
  cd singularity
  git checkout $SINGULARITY_VERSION

  ## 1.4 Build singularity
  cd $GOPATH/src/github.com/sylabs/singularity
  ./mconfig
  cd ./builddir
  make
  sudo make install
elif [[ "$SINGULARITY_VERSION" == "2."* ]]; then
  sudo apt-get update && \
  sudo apt-get install -y -q build-essential \
                          python

  wget "https://github.com/singularityware/singularity/releases/download/${SINGULARITY_VERSION}/singularity-${SINGULARITY_VERSION}.tar.gz"
  tar -xvzf singularity-${SINGULARITY_VERSION}.tar.gz
  cd singularity-${SINGULARITY_VERSION}
  ./configure --prefix=/usr/local
  make
  sudo make install
else
  echo "Singularity $SINGULARITY_VERSION is not supported, exiting..."
  exit 1
fi
