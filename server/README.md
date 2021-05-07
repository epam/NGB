# Requirements

**[Oracle JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)** or **[Open JDK 8](http://openjdk.java.net/install/)**

# How to build NGB server

Obtain the source code from github:
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB
```
## Build NGS server  using gradle

*This will compile, run tests, check PMD and checkstyle rules, package into war-file*
```
$ ./gradlew server:catgenome:clean server:catgenome:build 
```

*To build NGB server with Postgres database support, execute
```
$ ./gradlew server:catgenome:clean server:catgenome:build -Pdatabase=postgres
``` 
Several profiles are available for NGB server build:
    
  * **dev** - builds NGB server with a default configuration, in this case server's files and the database are stored in the root directory of the project
  * **release** - builds NGB server with an empty configuration for further deploying on Tomcat and external configuration (see installation documentation for details)
  * **jar** - configuration for a standalone jar build, configures NGB server to store the files and the database in
   the runtime directory of NGB server
Default profile is **dev**. You can specify the profile by adding the **"-Pprofile"** option to Gradle command:
   ```
   $ ./gradlew server:catgenome:clean server:catgenome:build -Pprofile=release
   ```
   
## Build NGB Command Line Interface using gradle

*This will compile, run tests, check PMD and checkstyle rules, package into tarball*
```
$ ./gradlew server:ngb-cli:clean server:ngb-cli:build
```
