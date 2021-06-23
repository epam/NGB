# BLAST - Results: Rerun job from History tab
Test verifies
 - that user has the ability to "rerun" any BLAST search task from History tab.

**Prerequisites**:
 - dataset = **dm6_data**
 - Sequence for search:
 `MTEYKLVVVGAGGVGKSALTIQLIQNHFVDEYDPTIEDSYRKQVVIDGETCLLDILDTAGQEEYSAMRD`
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed after Variants panel|
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  |
| 6 | Select **blastp** search selector | **blastp** value is displayed in **Algorithm** field by default |
| 7 | Enter sequence from **Prerequisites** in the **Query sequence** field | **Query sequence** field is filled by sequence from **Prerequisites**|
| 8 | Select **Refseq Selected Proteins** database from the dropdown in the **Database** field||
| 9 | Type and select **Homo sapiens (taxid: 9606)** in **Organism** field| |
| 10 | Set **Exclude** checkbox for **Organisms** | |
| 11 | Fill **Task title** field by any value (e.g. "Rerun job")  | |
| 12 | Expand ***Additional parameters*** section | <li> Default value `0.05` is set for ***Expect threshold*** <li> Default value `100` is set for  ***Max target sequences*** |
| 13 | Set  `0.05e-15` value to ***Expect threshold*** field |  |
| 14 | Set `10` value to ***Max target sequences*** field |  |
| 15 | Set `-word_size 3` value into ***Options*** field |
| 16 | Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 6 is displayed in the **Task title** column <li> Current state is **Searching...** |
| 17 | Wait until status changed to **Done** | <li>Current state changed to Done <li> Task ID becomes a hyperlink |
| 18 | Click ***Rerun*** button for the row with last search ID in the Task ID column | <li>The ***Search*** sub-tab of the ***BLAST*** panel is opened and <li> All parameters at the "Search" sub-tab are set the same as they were set at the task which is opened to "rerun" <ul><li>***Tool*** : **blastp** <li> **Query sequence** field is filled by sequence from **Prerequisites** <li> ***Task title*** is equal ***Task title*** specified at step 11 <li> ***Database***  is equal ***Database*** specified at step 8 <li> ***Organism*** is equal organism specified at step 9 and ***Exclude*** checkbox is checked <li> ***Additional parameters*** have values specified at steps 13-16. |
| 19 | Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically |
| 20 | Wait until status changed to **Done** and click on last search ID in the Task ID column | Search results are the same as search results for task which was opened to "rerun" |