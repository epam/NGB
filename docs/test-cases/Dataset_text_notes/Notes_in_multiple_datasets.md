# 

> Test verifies

- the test checks that notes are not displayed in the additional menu in Browser when opening multiple datasets at the same time

**Prerequisites**

- Dataset = **SV_Sample1** and **SV_Sample2**
- [Markdown text](Note_data/Markdown_example.md)

| Steps | Actions| Expected results|                                                                                                                                                                                                                                                                                                                                                                                                                                                                                
| :-----: | ---- | ---- |
|   1   | Login to NGB||
|   2   | Go to **DATASETS** panel||
|   3   | Select **SV_Sample1** dataset| <li> Dataset is selected with all related files <li>**Description** view is displayed in **BROWSER** header <br><br> ***Summary** view should be displayed in the BROWSER header, if there is **no Description** file in Dataset*|
|   4   | Click on **Description** view in the header of **BROWSER** panel | Additional control menu is expanded with following values:<li>**Description** (check-box) - *selected by default* <li>**Summary** (check-box) <li> **+ Add note** (button in blue color) <br><br>Previously added notes can be displayed in alphabetical order below the **+ Add note** button |                                                                     
|   5   | Click **+ Add note** button| The following values are displayed in the **BROWSER** window:<br><li> **Note title** field - ***mandatory** text field to specify a note title* <li> Empty text field below "Note title" - ***optional** field to specify a note description (content) (support **Markdown** formatting)* <li> **SAVE** and **CANCEL** buttons are located in the right upper corner of Browser window <li> **SAVE** button is disabled by default <br><br>**Add note** view display instead **Description** view in the BROWSER header |
|   6   | Fill **Note title** field by any value (e.g. **Note1**)| <li>**Note title** field is filled <li> **SAVE** button is enabled in blue color|
|   7   | Click **SAVE** button|<li>Title of new note (**Note1**) display in the Browser header instead of **Add note** value|
|   8   | Repeat 2-7 steps for **SV_Sample2**| Title of new note display in the Browser header instead of **Add note** value|
|   9   | Go to **DATASETS** panel||
|   10  | Set checkbox in **SV_Sample1** dataset| **SV_Sample1** and **SV_Sample2** datasets are selected|
|   11  | Go to header of Browser panel||
|   12  | Click on **Description** view in the header of **BROWSER** panel| Additional control menu is expanded with following values:<li>**Description** (check-box) - *selected by default* <li>**Summary** (check-box) <br><br>**No** "**+ Add note**" button and **notes** in the expanded additional menu |