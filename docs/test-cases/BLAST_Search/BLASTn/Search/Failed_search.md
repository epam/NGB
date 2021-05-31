# BLAST search is failed

Test verifies
 - manual BLASTn search from Views is failed

**Prerequisites**:


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> New **BLAST** panel is displayed in the Views menu |
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default |
| 4 | Look at **Search** tab| <li> **blastn** search selector chosen by default <li> **Query sequence** field is blank <li> **Task title** field is blank <li> **** value displays in the **Database** field by default <li> **Organism** field is blank <li> **megablast** value is displayed in **Algorithm** field by default  <li> **Additional parameters** section is collapsed 
| 5 | Enter value in the **Query sequence** field | | 
| 6 | Enter any title in the **Task title** field | | 
| 7 | Select **Human Genome** in the **Database** field|| 
| 8 | Type **Cat** Select **Cat** in the **Organism** dropdown field| |
| 9| Click on **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically |
| 10|  Look at **History** sub-tab | <li> A new search task created  <li> New task ID is auto generated <li> Title of the certain search task (if it was specified before the search) displays in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> button to cancel search (conventionally shown by a cross-button) displays to the right of the  <li>  button to open the search again in the "Search" sub-tab (conventionally shown by a reverse arrow-button) |
| 11| Wait until state changed to **Failed**| <li> **Current state** changed to **Failed** <li> button to open the search again in the "Search" sub-tab (reverse arrow-button) displays |
| 12| Click on last search id in the **Task ID** column with Failed state| <li> No table with corresponding results <li> **Blast parameters** collapsible section (collapsed by default) is displayed <li> The following message is displayed in red color under **BLAST parameters** collapsed block: **Search was failed. ERROR: An error has occured on the server.** <li> Download results button is hidden |