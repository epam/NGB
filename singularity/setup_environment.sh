set -e

# 1. Install singlularity

## 1.1 Install deps
sudo apt-get update && \
sudo apt-get install -y build-essential \
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

## 1.4 Build singularity
cd $GOPATH/src/github.com/sylabs/singularity
./mconfig
cd ./builddir
make
sudo make install
