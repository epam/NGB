#!/bin/bash
set -e
#Unpacking files from archives 
#TODO: Repack files
#	add dialog for port change
#	add .deb package size 

tar -xvf pack_files.tar.gz 

#Adding changes description and NGB version to /DEBIAN/changelog file and to /DEBIAN/control - 
#on Travis-CI

#Adding catgenome.jar and cli
cp ngb-cli-*.tar.gz ngb/opt/ngb
cp catgenome-*.jar ngb/opt/ngb
cd ngb/opt/ngb
tar -xvf ngb-*.tar.gz
mv -n ngb-cli/* ./
rm -rf ngb-cli/
rm ngb-cli-*.tar.gz
mv catgenome-*.jar bin/
cd ~/NGB/deb_build

#Building the package 
sudo dpkg-deb --build ngb/

#Installing the package 
sudo dpkg -i ngb.deb 

#Removing the building files





