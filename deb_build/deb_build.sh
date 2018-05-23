#!/bin/bash -e

tar -xvf pack_files.tar.gz 

#Adding catgenome.jar and cli
cp ngb-cli-*.tar.gz ngb/opt/ngb
cp catgenome-*.jar ngb/opt/ngb
cd ngb/opt/ngb
tar -xvf ngb-*.tar.gz
mv ngb-cli/bin/ngb bin/
mv ngb-cli/lib/* lib/
rm -rf ngb-cli/
rm ngb-cli-*.tar.gz
rm catgenome-*.jar

#mv catgenome-*.jar bin/
cd ~/NGB/deb_build

#Building the package 
sudo dpkg-deb --build ngb/

#Installing the package 
sudo apt install ./ngb.deb 

#Removing the building files
rm -rf ngb




