# User management
The security policy in NGB is based on next principal entities: **Users**, **Groups**, **Roles** and **Permissions**.

## Users
The **Users** entity defines the full list of the NGB users (accounts). Note that only authorized users that were registered early can work in NGB. The user can be added or deleted from NGB users list only by another user that have the *ROLE_ADMIN* role. Users names represent the authentical domain accounts (SAML/OAuth/OpenID), e.g. e-mails. 

## Groups
The **Groups** entity defines the users grants for access to the specific datasets. Each **Group** should present a specific group of users (e.g. project team members), for which are granted or refused permissions for viewing specific datasets. The group can be created/deleted from NGB groups list or assigned to the user only by user with the *ROLE_ADMIN* role. Each NGB user can be in one or several groups or not in any of them.

## Roles
The **Roles** entity defines the users grants for managing security access to the objects. Roles are predefined by the system, none of the users can create new roles or delete existing. The role can be assigned to the user only by user with the *ROLE_ADMIN* role. Each NGB user must be assigned one or several roles.

Actual system roles list:

Role | Description
------------ | -------------
ROLE_ADMIN | Administrator. Allows to create/delete users, create/delete/assign groups, assign roles. Users with this role have the full access to the system and all types of objects
ROLE_USER | Basic user. Sets to all users by default
ROLE_REFERENCE_MANAGER | Allows user to have access and control the reference track
ROLE_BAM_MANAGER | Allows user to have access and control the BAM files and tracks
ROLE_VCF_MANAGER | Allows user to have access and control the VCF files and tracks
ROLE_GENE_MANAGER | Allows user to have access and control the GENE (GFF/GTF/GTF3) files and tracks
ROLE_BED_MANAGER | Allows user to have access and control the BED files and tracks
ROLE_WIG_MANAGER | Allows user to have access and control the WIG files and tracks
ROLE_SEG_MANAGER | Allows user to have access and control the SEG files and tracks

## Permissions
The **Permissions** entity defines what actions with files and datasets are allowed to user. The permission settings are divided into the following options which can be combined:
- Read
- Write

## User management from GUI
The user management panel helps to manage users, groups and system roles.
> Note: this panel is available only for users with the *ROLE_ADMIN* role.

To open user management panel click on ![NGB User Management](images/um-overview-1.png) icon on the main toolbar of the application.

Setting permissions on specific files and datasets can be done through CLI.