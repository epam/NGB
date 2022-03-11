# Running NGB from standalone Jar

A standalone Jar distribution is available for NGB, which includes embedded Tomcat server

## Environment requirements

Verify that your system meets or exceeds the following hardware/software requirements

* Server hardware requirements
    * CPU: 2 cores
    * RAM: 4Gb
    * HDD: 20 Gb free space
    * **[Oracle JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)** or **[Open JDK 8](http://openjdk.java.net/install/)**
    * GIT

* Client web-browser requirements
    * Chrome (>= 56)
    * Firefox (>= 51)
    * Safari (>= 9)
    * EDGE (>= 25)
  
## Get Source Code

Obtain NGB source code from GitHub:
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB
```

## Build JAR file

Build NGB standalone jar, using Gradle build script
```
$ ./gradlew buildJar
```

You can find **catgenome.jar** archive in the **dist/** folder

## Run JAR file

Run **catgenome.jar**

*Note:* data files, used by NGB instance, will be located in a `current` directory (current directory of a console)

```
# Data files will be located in the same folder as a catgenome.jar
$ java -jar catgenome.jar

# Data files will be located in /home/user folder
$ pwd
/home/user
$ java -jar NGB/dist/catgenome.jar
```

NGB will be available at http://localhost:8080/catgenome

## Configuring NGB instance

By default NGB will run on port 8080 and locate all the data (files and database) in the runtime folder.

To customize the configuration the following options are available:

### Configure data storage

You can provide an external file **catgenome.properties** to specify data location. Available properties:

* **files.base.directory.path=/opt/catgenome/contents** path for storing NGB files (Fasta, BAM, VCF, etc.)
* **database.driver.class=org.h2.Driver** driver for NGB database, default database is H2
* **database.jdbc.url=jdbc:h2:file:/opt/catgenome/H2/catgenome** path to NGB database location
* **database.username=catgenome** user for NGB database
* **database.password=** password for NGB database, may be empty
* **database.max.pool.size=25** NGB database connection pool configuration
* **database.initial.pool.size=5** NGB database connection pool configuration

If you want to enable browsing NGS files directly from server's file system, add the following properties:

* **file.browsing.allowed=true** - enables file browsing from file system
* **ngs.data.root.path=/opt/catgenome** - sets root of allowed to browsing file system part to /opt/catgenome. 

If this property is not set, root will be set to the root of file system.

If you want to configure default options for tracks visualization on a client side, add the following properties:
* **config.path=/opt/catgenome/configs** path to a directory that contains `json` configuration files for NGB client

If you want to specify max number of VcfIndexEntries keeping in memory during vcf loading, add the following property. For files, which produce more entries then the number, extra entries will be spilled to disk (temp directory).
* **files.vcf.max.entries.in.memory=1000000** - 1000000 entries take about 3Gb in the heap

If you want to disable cache for headers and indexes of VCF, GTF, BED files, change the following property. By default it is true:
* **server.index.cache.enabled=false** - disables caching for headers and indexes

If you want to secure NGB we provide several options:
#### 1. JWT Authentication 
With this option user can be authenticated using third-party JWT tokens. To enable this authentication, set the following properties:
 * **jwt.security.enable=true** enables the JWT Authorization
 * **jwt.key.public=PUBLIC_KEY_VALUE** public key to perform JWT token validation
 * **jwt.required.claims=groups=LAB,roles=USER** comma-separated list of required claims to perform authorization, if property is not set any valid token will be authorized. 
 Supported claims are: **groups**, **roles**, **org_unit_id**. Several values of each type are supported: **groups=LAB,groups=TEST**.

If this authentication is enabled for NGB each call to server API should include a valid JWT token either in header (**"Authorization: Bearer {TOKEN_VALUE}"**) or in cookies.

#### 2. SAML SSO Authentication
With this option users can be authenticated in NGB with the help of a third-party Identity Provider (**IDP**). 
This is particularly useful when NGB needs to be integrated into some existing infrastructure.
To enable this authentication option, first create a SAML SSO endpoint in your IDP. Then set the following properties:
While creating the endpoint you will need to create a signing certificate. Create a Java Keystore (**JKS**) and put it there under some alias, 
together with your IDP's signing certificate.
 
 * **saml.security.enable=true** enables SAML authentication
 * **server.ssl.ciphers=HIGH:!RC4:!aNULL:!MD5:!kEDH** Specify encryption algorithms to use. This enables HTTPS protocol handling, which is required by SAML.
 * **server.ssl.key-store=file:/path/to/your/keystore.jks** Specify the path to your JKS
 * **server.ssl.metadata=/path/to/your/IDP/FederationMetadata** A path to your IDP's metadata file
 * **server.ssl.key-store-password=password** Your JKS password
 * **server.ssl.keyAlias=key-alias** An alias of the certificate to sign HTTPS connection 
 * **saml.sign.key=key-alias-2** An alias of the SAML certificate, that you've sent to the IDP
 * **server.ssl.endpoint.id=https://localhost:8080/catgenome** Endpoint ID, that you've sent to IDP
 * **saml.authn.request.binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect**
 * **saml.authorities.attribute.names=http://schemas.xmlsoap.org/ws/2005/05/identity/claims/tokenGroups**
 * **saml.user.attributes=Email=http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress,Name=http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name**

With SAML authentication enabled, **ngb-cli** won't have access to the application. If you need CLI access, enable JWT security alongside with SAML, as described above.
You can use a third-party JWT tokens or let NGB generate them for you.
To enable generation of JWT tokens, set the following options:
 * **jwt.security.enable=true** enables the JWT Authorization
 * **jwt.key.public=PUBLIC_KEY_VALUE** public key to perform JWT token validation
 * **jwt.key.private=PRIVATE_KEY_VALUE** private key to sign JWT tokens
 * **jwt.token.expiration.seconds=TOKEN_EXPIRATION_PERIOD** JWT token expiration period. If left blank, the default value will be used, which is 2592000 seconds (30 days).     

#

You should put **catgenome.properties** in **config** folder in the runtime folder or provide path to folder with properties file from command line:
 
```
$ java -jar catgenome.jar --conf=/folder/with/properties
```
 
### Configure Embedded Tomcat

NGB uses Spring Boot so it supports a full stack of Spring Boot Application properties.
These properties may be specified by the command line:

```
# Run NGB on 9999 port 
$ java -jar catgenome.jar --server.port=9999
# Disable traffic compression
$ java -jar catgenome.jar --server.compression.enabled=false
```
 
or in **application.properties** file in **config** folder in the runtime folder:
* server.port=9999
* server.compression.enabled=false

See the full list of available options in the [Spring Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html)

### Configure for working with AWS S3

If you want to add the ability of browsing NGS files from external data storages (AWS S3), you need to configure some AWS parameters before running **catgenome.jar**.
You may do it in two ways:

#### 1. Through configuration and credential files

Create folder with name ".aws" in home directory location:
```
$ mkdir $HOME/.aws
```
In this folder create two files - `credentials` and `config` - with the following content:
> **$HOME/.aws/credentials**
> ```
> [default]
> aws_access_key_id=AKIAIOSFODNN7EXAMPLE
> aws_secret_access_key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
> ```
> Where:
>
> `[default]` - profile name
>
> `aws_access_key_id` – AWS access key
>
> `aws_secret_access_key` – AWS secret key
>
> **Note**: Do not forget to replace values of *aws_access_key_id* and *aws_secret_access_key* variables with your own AWS access key and AWS secret key.

> **$HOME/.aws/config**
> ```
> [default]
> region=us-east-1
> ```
> Where:
>
> `[default]` - profile name
>
> `region` – default AWS region
>
> **Note**: Replace value of *region* variable, if needed.

More information about `credentials` and `config` AWS files see [here](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html).

#### 2. Through setting environment variables

Set the following AWS environment variables:
```
$ export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
$ export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
$ export AWS_DEFAULT_REGION=us-east-1
```
Where:
- `AWS_ACCESS_KEY_ID` – specifies an AWS access key
- `AWS_SECRET_ACCESS_KEY` – specifies the secret key associated with the access key
- `AWS_DEFAULT_REGION` – specifies the AWS region

> **Note**: Do not forget to replace values of *AWS_ACCESS_KEY_ID* and *AWS_SECRET_ACCESS_KEY* variables with your own AWS access key and AWS secret key. And replace value of *AWS_DEFAULT_REGION* variable, if needed.

After that you may run **catgenome.jar** file to start NGB instance as usually.

### Configure access to Blob Containers in a Microsoft Azure storage account

In addition to files and AWS S3, an NGB instance can access [blobs](https://docs.microsoft.com/en-us/azure/storage/blobs/storage-blobs-introduction#blobs) in  [containers](https://docs.microsoft.com/en-us/azure/storage/blobs/storage-blobs-introduction#containers) within one [Azure storage account](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-overview). This includes [Azure Data Lake Storage Gen2](https://docs.microsoft.com/en-us/azure/storage/blobs/data-lake-storage-introduction) with the hierarchical structure. Azure Data Lake (Gen1) is not supported.

> If NGB is running in Azure hosted virtual machines or containers that have an appropriate  [Managed Identity](https://docs.microsoft.com/en-us/azure/active-directory/managed-identities-azure-resources/overview) assigned, access configuration can be as simple as specifying the storage account name only.

Available authentication methods listed in NGB's order of precedence: 

1. [Access Key](https://docs.microsoft.com/en-us/azure/storage/common/storage-account-keys-manage?tabs=azure-portal) (ordinary blob containers only).
2. [Service Principal](https://docs.microsoft.com/en-us/azure/active-directory/develop/app-objects-and-service-principals#service-principal-object) using Service Principal Id/client id and -secret.
3. Credentials acquired by the Azure Identity Platform Library:
   - [Environmental credetials](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.environmentcredential?view=azure-dotnet)
   - [Managed Identity](https://docs.microsoft.com/en-us/azure/active-directory/managed-identities-azure-resources/overview)
   - other authentication methods, see [DefaultAzureCredential Class](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.defaultazurecredential?view=azure-dotnet) for details.

#### NGB configuration
Configuration properties can be specified either as 
- an entry in `catgenome.conf`, e. g 
  ```
  azure.storage.account=mystorageaccountname
  ``` 
- a JVM system property in the `java` command line, e. g. 
  ``` 
  java -Dazure.storage.account=mystorageaccountname -jar catgenome.jar
  ``` 
- as a process environment variable for supported properties. See [Environment Credential](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.environmentcredential?view=azure-dotnet) for details.

Find the relevant sets of properties and their description below. 

> **Note:** The presence of properties triggers a single way of authentication, e.g. trying an access key for a storage account. Upon failure, no fall-through attempt is made using any other properties.  

> **Note:** The `azure.storage.account` property is mandatory to connect to Azure. If left blank, other Azure related properties and environment variables are ignored and no connection attempt is made at all.
> 
> The value of `azure.storage.account` is the short name, **not the URL**.

##### Access Key authentication
```
azure.storage.account=mystorageaccountname
azure.storage.key=IyXn/qBiqNS7uvIEi...
```
All properties are _mandatory_. Access key credentials are not available for Azure Data Lake Gen2 containers.
#### Service Principal authentication
```
azure.storage.account=mystorageaccountname
azure.storage.tenant_id=13babfaa-a14c-11ec-b909-0242ac120002
azure.storage.client_id=2db7cd12-345d-sdf6-b454-0242ac163820
azure.storage.client_secret=k.yjs-fhgk-sj;sfg-zudu
```
All properties are _mandatory_. (Here, the tenant id _cannot_ be set as an environment variable for service principal authentication.)
#### Identity platform
```
azure.storage.account=mystorageaccountname
azure.storage.tenant_id=13babfaa-a14c-11ec-b909-0242ac120002
azure.storage.managed_identity_id=73a340aa-a150-11ec-b909-0242ac179836
```
- `azure.storage.account`: Mandatory
- `azure.storage.tenant_id`: Optional. Required if authentication is attempted through an account (e.g. during software development) that has access to more than one tenant. Can alternatively be specified via environment variable. 
- `azure.storage.managed_identity_id`: Optional. NGB is required to run in an Azure resource that supports Managed identities and has at least one managed identity with suitable access to the storage account assigned. Required if more than one Managed Identity is assigned.

See [Environment Credential](https://docs.microsoft.com/en-us/dotnet/api/azure.identity.environmentcredential?view=azure-dotnet) as an alternative option to specify Azure connectivity information. 
