# Export data from GENES table 
Test verifies
 - that it is possible to download .csv and tsv files with data from GENES table 
  - that downloaded file has the same data and order of display as in the UI
   - that it is possible ti export file with/without header row

**Prerequisites**:
1. Dataset = **NC_003071**
2. Open GENES panel (if it was not opened)


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB  | |
| 2 | Go to **Datasets** tab| |
| 3 | Select **NC_003071** dataset|
| 4 |Go to **GENES** panel|<li> **GENES** panel is opened <li>**Download** button displays the first in the right upper-corner of GENES panel|
| 5 |Apply Asc sotring to **Id** column||
| 6 |Apply **mrna** filter in **Type** field||
| 7 |Add **Id(attr)** column from additional columns||
| 8 |Swap **Start** and **Type** columns||
| 9 |Click **Download** button in the right upper-corner| **Download table data** modal window is opened with the following elements inside: <li>**Format** dropdown field with 2 availiable values: ***CSV*** and ***TSV*** (***CSV*** value is chosen by default) <li> **Include header row** checkbox is displayed in the window (not selected by default)<li>**Download** button in blue color is displayed in the lower right corner <li>**Cancel** button in black color is displayed to the right of 'Download' button <li>**x** (close) button is displayed in the right upper corner| 
| 10 |Set scheckbox in **Include header row**||
| 11 |Click **Download** button||
| 12 |Select a location on computer where to download the file and save it|The table data is automatically downloaded on computer in selected format (CSV)|
| 13 |Open downloaded file| File is successfully opened with the following format of name: **GENES-97.csv**, <br> where '97' = 'referenceid'|
| 14 |Look at header row in the table| The headers and their order are corresponded to the headers indicated on the UI: <li>Chr <li>Name <li>Id <li> Position From <li>Feature?? <li> Position To <li>Strand<li>Id(attr) <br> **Info** column isn't displayed in the file|
| 15 | Look at data in the file| Downloaded file is contained only data according all "configured" settings in 5-8 steps: <li>The table is filerest by **mrna** value <li> The table is Asc sorted by **Id** column <li>**Feature** and **Position From** columns are swapped <li>**Id(attr)** column with values is displayed in the file <li> No **Info** values in the file|
| 16 |Go to back to **GENES** panel| |
| 17 |Click **Download** button in the right upper-corner|**Download table data** modal window is opened <li> **CSV** value is displayed in **Format** dropdown field <li> Checkbox is not set in **Include header row**|
| 18 |Select **TSV** value in the **Format** dropdown field|
| 19 | Click **Download** button||
| 20 |Select a location on computer where to download the file and save it|The table data is automatically downloaded on computer in selected format (CSV)|
| 21|Open downloaded file| File is successfully opened with the following format of name: **GENES-97.tsv**, <br> where '97' = 'referenceid'|
| 22 | Look at data in the file| <li> No row headers in the file <li> Downloaded file is contained only data according all "configured" settings in 5-8 steps <li> No **Info** values in the file.|