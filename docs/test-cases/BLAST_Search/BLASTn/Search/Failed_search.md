# BLAST search is failed

Test verifies
 - that the search fails if there are errors

**Prerequisites**:
  
 - Go to NGB
 - Close **Blast** panel if it is opened


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed in the Views menu |
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default <li> **Query sequence**, **Task title**, **Organism**, **Database** fields are blank <li> **megablast** value is displayed in **Algorithm** field by default  <li> **Additional parameters** section is collapsed with default values inside |
| 4 | Enter *54534* value in the **Query sequence** field | | 
| 5 | Enter any title in the **Task title** field (e.g. Failed search) | | 
| 6 | Select **Homo_sapiens.GRCh38** in the **Database** field|| 
| 7 | Type and select **Homo sapiens (taxid: 9606)** in the **Organism** dropdown field| |
| 8 | Click on **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 5 is displayed in the **Task title** column <li> Current state is **Searching...** <li>  Date and time when the certain search was started is displayed in the **Submitted at** column <li> Duration of the certain search task displays in the **Duration** column <li> Button to **cancel search** (conventionally shown by a **cross-button**) displays to the right of the task <li>  **Rerun** button (conventionally shown by a **reverse arrow-button**) displays to the right of the cross-button|
| 9 | Wait until state changed to **Failure**| <li> **Current state** changed to **Failure** <li> Button to open the search again in the "Search" sub-tab (reverse arrow-button) displays |
| 10| Click on last search id in the **Task ID** column with Failed state| <li> No table with corresponding results <li> Collapsed **Blast parameters** section is displayed with details (parameters/options) of the opened search in the same **History** sub-tab <li> The following message is displayed under **BLAST parameters** collapsed block: **Search failed.** with Description of error. <li>**Rerun** button displays in the upper right corner <li> **Download** results button is hidden|