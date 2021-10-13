# Add note

> Test verifies

- adding note in BROWSER panel.

**Prerequisites**

- Dataset = **SV_Sample1**
- [Markdown text](Note_data/Markdown_example.md) 


| Steps | Actions| Expected results|                                                                                                                                                                                                                                                                                                                                                                                                                                                                                
| :-----: | ---- | ---- |
|   1   | Login to NGB||
|   2   | Go to **DATASETS** panel||
|   3   | Select **SV_Sample1** dataset| <li> Dataset is selected with all related files <li>**Description** view is displayed in **BROWSER** header <br><br> ***Summary** view should be displayed in the BROWSER header, if there is **no Description** file in Dataset*|
|   4   | Click on **Description** view in the header of **BROWSER** panel | Additional control menu is expanded with following values:<li>**Description** (check-box) - *selected by default* <li>**Summary** (check-box) <li> **+ Add note** (button in blue color) <br><br>Previously added notes can be displayed in alphabetical order below the **+ Add note** button|                                                                     
|   5   | Click **+ Add note** button| The following values are displayed in the **BROWSER** window:<br><li> **Note title** field - ***mandatory** text field to specify a note title* <li> Empty text field below "Note title" - ***optional** field to specify a note description (content) (support **Markdown** formatting)* <li> **SAVE** and **CANCEL** buttons are located in the right upper corner of Browser window <li> **SAVE** button is disabled by default <br><br>**Add note** view display instead **Description** view in the BROWSER header |
|   6   | Fill **Note title** field by any value (e.g. **Project_notes**)| <li>**Note title** field is filled <li> **SAVE** button is enabled in blue color|
|   7   | Fill description field by any value (e.g. from  **Markdown text** file in **Prerequisites**)| Description field is filled |                                     
|   8   | Click **SAVE** button| <li>Added description of note is displayed in the BROWSER window <li> Markdown formatting is applied to the text (from Prerequisites)<li> **EDIT** button display in the right upper corner of BROWSER window <li>Title of new note (**Project_notes**) display in the Browser header instead of **Add note** value|                                                                                                                          
|   9   | Click on title name in BROWSER header (e.g. **Project_notes**)|  <li> Additional control menu is expanded  <li> Added note (**Project_notes**) display in the menu below the "Add note" item <li> The flag is set in front of the added note <li> Notes are sorted alphabetically by title|                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       
