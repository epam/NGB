# Users
> Note: user management panel is available only for the users with the *ADMIN* role.

In user management panel select "**Users**" tab:
![NGB User Management](images/um-users-1.png)

On this tab you can view the full NGB users list, their groups and roles. Here you can add new user or delete existing, also here you can grant/refuse roles or assign groups to users.

You can search specific user or limit displayed list by typing user name in query string in upper side of the tab.

## Add new user
To add new user click "**Create user**" button on the right side of the panel.

In opened pop-up window: input user authentical account into the "**User name**" field (**1**), if you want to assign to user specific role(s) or group(s) select them from the dropdown list (**2**) and click "**Add**" button for confirmation (**3**). Then click "**Create**" button (**4**) to finish adding new user:

![NGB User Management](images/um-users-2.png)
> Note: the *ROLE_USER* role is assigned to all users by default, you cannot delete it while adding a user.

After that created user will be appeared in the NGB users list:

![NGB User Management](images/um-users-3.png)

> You also could create this user through CLI using the command:
> ```
> $ ngb reg_user test_test@example.com --groups REFERENCE_MANAGER
> ```

## Edit user
To edit a user click ![NGB User Management](images/um-users-4.png) icon opposite the user name (account).

In opened pop-up window: assign to the user specific role(s) or group(s) by selecting them from the dropdown list (**1**) and clicking "**Add**" button for confirmation (**2**), delete unnecessary role(s) or group(s) by clicking "**Recycle bin**" icon opposite the role/group name (**3**). After all changes were done click "**Save**" button (**4**) to confirm:

![NGB User Management](images/um-users-5.png)

> You also could assign this user to the existing group/role from the example above through CLI using the command:
> ```
> $ ngb add_group VCF_MANAGER -u test_test@example.com
> ```

## Delete user
To delete a user click ![NGB User Management](images/um-users-4.png) icon opposite the user name (account).

In opened pop-up window: click "**Delete**" button in the left downer corner:

![NGB User Management](images/um-users-6.png)

Then confirm deleting in appeared window by clicking "**Ok**" button:

![NGB User Management](images/um-users-7.png)

> You also could delete that user through CLI using the command:
> ```
> $ ngb del_user test_test@example.com
> ```