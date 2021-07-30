# Export data from VARIANTS table 
Test verifies
 - that it is possible to download .csv and tsv files with data from VARIANTS table 
  - that downloaded file has the same data and order of display as in the UI
   - that it is possible ti export file with/without header row

**Prerequisites**:
1. Dataset = **SV_Sample1** with **sample_1-lumpy.vcf** file
2. Open VARIANTS panel (if it was closed)


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB  | |
| 2 | Go to **Datasets** tab| |
| 3 | Select **SV_Sample1** dataset|
| 4 |Go to **Variants** panel|<li> **VARIANTS** panel is opened <li>**Download** button displays the first in the right upper-corner of GENES panel|
| 5 |Apply Desc sotring to **Position** column||
| 6 |Apply **BND** filter in **Type** field||
| 7 |Add **AC** column from additional columns||
| 8 |Swap **Chr** and **Gene** columns||
| 9 |Click **Download** button in the right upper-corner| **Download table data** modal window is opened with the following elements inside: <li>**Format** dropdown field with 2 availiable values: ***CSV*** and ***TSV*** (***CSV*** value is chosen by default) <li> **Include header row** checkbox is displayed in the window (not selected by default)<li>**Download** button in blue color is displayed in the lower right corner <li>**Cancel** button in black color is displayed to the right of 'Download' button <li>**x** (close) button is displayed in the right upper corner| 
||Select **TSV** value in the **Format** dropdown field
| 10 |Set scheckbox in **Include header row**||
| 11 |Click **Download** button||
| 12 |Select a location on computer where to download the file and save it|The table data is automatically downloaded on computer in selected format (TSV)|
| 13 |Open downloaded file| File is successfully opened with the following format of name: **VariantsTable-1.tsv**, <br> where '97' = 'referenceid'|
| 14 |Look at header row in the table| The headers and their order are corresponded to the headers indicated on the UI: <li>Type <li>Chr <li>Gene <li> Position From <li>AC <br> **Info** column isn't displayed in the file|
| 15 | Look at data in the file| Downloaded file is contained only data according all "configured" settings in 5-8 steps: <li>The table is filerest by **BND** value <li> The table is Asc sorted by **Position** column <li>**Chr** and **Gene** columns are swapped <li>**AC** column with values is displayed in the file <li> No **Info** values in the file|
| 16 |Go to back to **GENES** panel| |
| 17 |Click **Download** button in the right upper-corner|**Download table data** modal window is opened <li> **CSV** value is displayed in **Format** dropdown field <li> Checkbox is not set in **Include header row**|
| 18 | Click **Download** button||
| 19 |Select a location on computer where to download the file and save it|The table data is automatically downloaded on computer in selected format (CSV)|
| 20|Open downloaded file| File is successfully opened with the following format of name: **VariantsTable-1.csv**, <br> where '97' = 'referenceid'|
| 21 | Look at data in the file| <li> No row headers in the file <li> Downloaded file is contained only data according all "configured" settings in 5-8 steps <li> No **Info** values in the file|