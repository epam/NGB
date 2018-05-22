This folder contains files required for .deb package building and the ngb.deb package itself ready for installation via the dpkg package manager.

## Building of package from binaries

Obtain source code from github: 
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB/deb_build
```
Build the package using `dpkg-deb --build`
```
$ fakeroot dpkg-deb --build ngb/
```
After the completion of the command the `ngb.deb` archive should appear. NOTE! If ngb.deb is already present, it will be rewrittten without any warnings! 

## Installing ngb.deb to the system 

After the ngb.deb is built or the pre-released version is used, it should be installed on the ngb server.
It is done via the dpkg package manager. 
NOTE! Installing .deb packages always require sudo priveleges!

```
$ sudo apt install ./ngb.deb
```
Package files will be copied to /opt/ngb directory and you will be prompted to enter the port number to launch the ngb. By default ngb will be started on localhost:8080 

If you want to install ngb in non-interactive mode using the default port enter

```
$ sudo DEBIAN_FRONTEND=noninteractive apt-get -y install ./ngb.deb
```
## Using ngb

Use ngb-CLI without path to executable. For example:
 
```
$ ngb reg_ref mus_musculus.fasta --name mouse
```
All data will be saved in /opt/ngb_data/contents by default

## Configuring the ngb

If you want to make advanced ngb configuration, you may edit catgenome.properties and server.properties files in /opt/ngb/config/ , rebuild and reinstall the package. 

## Removing the ngb from server

NGB now can be easily removed from your machine by executing. Removing packages also requires sudo privileges 

If you want to save modified configuration: 

```
sudo apt-get remove ngb 
```

For complete removal of all ngb configuration files use: 

```
sudo apt-get purge ngb 
```
All user files will be saved in /opt/ngb_data/ folder. They will be accessed when reinstalling the ngb using the abovementioned way

