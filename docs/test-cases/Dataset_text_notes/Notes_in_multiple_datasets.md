# Notes in multiple datasets

> Test verifies

- the test checks that notes are displayed in the additional menu in Browser when opened multiple datasets related to one reference 

**Prerequisites**

- Dataset = **SV_Sample1** and **SV_Sample2**
- [Markdown text](Note_data/Markdown_example.md)

| Steps | Actions| Expected results|                                            
| :-----: | ---- | ---- |
|   1   | Login to NGB||
|   2   | Go to **DATASETS** panel||
|   3   | Select **SV_Sample1** dataset| <li> Dataset is selected with all related files <li>**Description** view is displayed in **BROWSER** header <br><br> ***Summary** view should be displayed in the BROWSER header, if there is **no Description** files in Dataset*|
|   4   | Click on **Description** view in the header of **BROWSER** panel | Additional control menu is expanded with following values:<li>**Summary** (check-box) <li>**Description** html files (check-boxes) - *selected by default if exist*<br><br><li>Previously added notes can be displayed in alphabetical order below the **Description** files (check-boxes) <li> **+ Add note** (button in blue color) displays at the bottom of the list |  
|   4   | Select **SV_Sample2** dataset| <li> Dataset is selected with all related files <br><br> **Summary** view is displayed in the BROWSER header|
|   5   | Click on **Summary** view in the header of **BROWSER** panel | Additional control menu is expanded with following values: <li>**Summary** (check-box)  - *selected by default* <li> The 2 selected datasets are displayed below, visually separated by a blue line (**SV_Sample1** and **SV_Sample2**) <li> In each of the displayed datasets, Description htmk files (if exists), notes (if exists) and the **+Add** note button are displayed related to a specific dataset |  
|   6   | Go to **DATASETS** panel ||
|   7   | Deselect **SV_Sample1** dataset| ** SV_Sample1 ** dataset not selected|
|   8   | Click on **Summary** view in the header of **BROWSER** panel | Additional control menu is expanded with following values:<li>**Summary** (check-box) *selected by default if no html description files* <br><br><li>Previously added notes can be displayed in alphabetical order below the **Description/Summary**  (check-boxes) <li> **+ Add note** (button in blue color) displays at the bottom of the list |  
