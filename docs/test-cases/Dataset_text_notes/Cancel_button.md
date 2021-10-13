# Cancel button

 Test verifies
-  that it is possible to cancel changes applied to a note 

**Prerequisites**

- Dataset = **SV_Sample1**

| Steps | Actions| Expected results| 
| :-----: | ---- | ---- |
|   1   | Login to NGB||
|   2   | Go to **DATASETS** panel||
|   3   | Select **SV_Sample1** dataset| <li> Dataset is selected with all related files <li>**Description** view is displayed in **BROWSER** header<br><br> ***Summary** view should be displayed in the BROWSER header, if there is **no Description** file in Dataset*|
|   4   | Click on **Description** view in the header of **BROWSER** panel | Additional control menu is expanded |                                                                     
|   5   | Click **+ Add note** button| Edit mode view display in the BROWSER with the following values: <br><li> **Note title** field - ***mandatory** <li> Empty text field below "Note title" - ***optional*** <li> **SAVE** and **CANCEL** buttons are located in the right upper corner of Browser window <li> **SAVE** button is disabled by default <br><br>**Add note** view display instead **Description** view in the BROWSER header |
|   6   | Fill **Note title** field by **Test1** value| <li> **SAVE** button is enabled in blue color|
|   7   | Click **SAVE** button| <li>BROWSER window is empty <li> **EDIT** button display in the right upper corner of BROWSER window <li>Title of new note (**Test**) display in the Browser header instead of **Add note** value|  
|   8   | Repeat 4-6 steps for **Test2** note title||
|   9   | Go to BROWSER header||
|  10   | Select **Test1** note in the additional menu|Warning message is displayed: <br> **There is an unsaved changes. Save it or cancel editing** <br><br>**OK** button display in the bottom left corner|
|  11   | Click **OK** button| Warning message is closed|
|  12   | Click **CANCEL** button| <li> Edit mode of note window is closed <li>The previous opened page is displayed in the BROWSER window and header: **Test1** note <li> No **Test2** note in the Browser window and header|