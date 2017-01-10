################################################################################
#### CATGenome Browser Embedded Database Flyway
####
#### This document covers all major concerns about SQL update
#### script management.
####
#### To update automatically any schema, when application starts
#### Flyway has been configured and used.
####
#### More information about Flyway configuration you can find,
#### using resources listed below:
#### 1) http://flywaydb.org/;
#### 2) conf/catgenome/applicationContext-database.xml defines
####    actual Flyway configuration for CATGenome Browser.
####
#### Please READ CAREFULLY ALL SECTIONS listed below and contact
#### me for any further questions if necessary.
####
#### Author: Denis Medvedev, dmedvedev@ggasoftware.com
################################################################################

1. EMBEDDED DATABASE FILE LOCATION
H2_HOME is pointed to the root directory, which H2 will be used to
store embedded database files.
If you clean all contents from H2_HOME, you lose the current persistent
state of your embedded database. Next time when application is started,
Flyway recovers schema and all its objects, but your records will be lost.

1.1. DEV Profile
This configuration is used for DEV local environment and in this case
H2_HOME will be set to @rootDirPath@/H2. The placeholder will be automatically
resolved to your root project dir on the same level with "build" directory used
by Gradle.

1.2. TEST Profile (JUnit)
This configuration is used to run JUnit tests and in this case H2_HOME is
pointed to %USER_HOME%/H2. %USER_HOME% is always pointed to user home directory.

1.3. RELEASE Profile
In fact release configuration is normally moved to the corresponded environment.
H2 will locate its files according to value of "database.jdbc.url" that has to
be provided via properties files located under the corresponded environment where
application should be started.

################################################################################
2. FLYWAY NOTES
2.1. Flyway processes only SQL files that it can find under specified locations,
so all your scripts with appropriate names (see section 3) should be under
classpath "database/catgenome".
2.2. Flyway will try to update an embedded database, associated with application
datasource, on each application start. All processed files will be logged through
Flyway information schema table. Any SQL script with the same version will be
applied only once and skipped if it has been already applied.
2.3. Flyway processes each SQL script only once. So any changes applied to
scripts that has been already applied ARE STRONGLY PROHIBITED. If you need to
revert some applied changes or change something, you HAVE TO ADD a new SQL script
which version is greater than the current one.
2.4. WE'LL MANAGE an embedded database version through prefix of SQL file,
following the format "vYYYY.MM.DD_HH.mm".

################################################################################

3. SQL SCRIPTS NAMING CONVENTION
Each file that contains update SQL script that should be managed by Flyway
should be named properly according to the following format:

vYYYY.MM.DD_HH.mm__{JIRA-ISSUE-KEY}_Describe_Here_What_Is_About_This_Update.sql

Example
You has added a new SQL script on 27th October 2015 at 8:00 p.m. It covers changes
for database required to implement EPMCMBI-1 issue. In fact your script creates
necessary tables to manage additional metadata about uploaded reference genomes.
According to the described circumstances you HAVE TO create SQL file with the
following name:

v2015.10.27_20.00__EPMCMBI-1_Reference_Genome_Metadata_Management.sql