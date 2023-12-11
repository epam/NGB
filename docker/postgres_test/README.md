# Requirements

* **[Docker engine](https://docs.docker.com/engine/installation/)**
* **[Docker compose](https://docs.docker.com/compose/install/)**
* **[Oracle JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)** or **[Open JDK 8](http://openjdk.java.net/install/)**

## docker-compose file destination

This file is used to run the posgreSQL database image when running server 
catgenome tests with the option `-Pdatabase=postgres`.
The image starts automatically by the `composeUp` task, which is associated with the `test` task