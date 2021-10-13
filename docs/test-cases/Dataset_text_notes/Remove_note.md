# Remove note

> Test verifies

- removing notes in BROWSER panel

**Prerequisites**

- Dataset = **SV_Sample1** 

| Steps | Actions| Expected results|                                            
| :-----: | ---- | ---- |
|   1   | Login to NGB||
|   2   | Go to **DATASETS** panel||
|   3   | Select **SV_Sample1** dataset| <li> Dataset is selected with all related files <li>**Description** view is displayed in **BROWSER** header<br><br> ***Summary** view should be displayed in the BROWSER header, if there is **no Description** file in Dataset*|
|   4   | Click on **Description** view in the header of **BROWSER** panel | Additional control menu is expanded |                                                                     
|   5   | Click **+ Add note** button| Edit mode view display in the BROWSER with the following values: <br><li> **Note title** field - ***mandatory** <li> Empty text field below "Note title" - ***optional*** <li> **SAVE** and **CANCEL** buttons are located in the right upper corner of Browser window <li> **SAVE** button is disabled by default <br><br>**Add note** view display instead **Description** view in the BROWSER header |
|   6   | Fill **Note title** field by **Remove_note** value| <li> **SAVE** button is enabled in blue color|
|   7   | Click **SAVE** button| <li>BROWSER window is empty <li> **EDIT** button display in the right upper corner of BROWSER window <li>Title of new note (**Test**) display in the Browser header instead of **Add note** value|                                                                                                                          
|   8   | Click **EDIT** button| Edit mode view display in the BROWSER window with **Note title** and description fields <li> **REMOVE**(in red color), **SAVE**(in blue color), **Cancel** (in black color) buttons are display in the right upper corner of BROWSER window|
|   9   | Click **Remove** button|The window with the editing of the note is closed. <li> Note is deleted <li>The Descriptions window is displayed in the Browser window|
|  10   | Click on **Description** view in the header of **BROWSER** panel| <li>Additional expanded menu is opened <li> No **Remove_note** value in the notes below **+Add note** button|