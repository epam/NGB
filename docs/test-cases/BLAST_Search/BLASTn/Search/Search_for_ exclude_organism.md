# Search for exclude organism
Test verifies
 - that it is possible to set organisms that will be excluded from the search

**Prerequisites**:
 - Sequence for search:
 `MTEYKLVVVGAGGVGKSALTIQLIQNHFVDEYDPTIEDSYRKQVVIDGETCLLDILDTAGQEEYSAMRD`
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed after Variants panel|
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default <li> **Query sequence** , **Task title** ,**Database**, **Organism** fields are blank <li> **megablast** value is displayed in **Algorithm** field by default  <li> **Additional parameters** section is collapsed with default values inside |
| 4 | Enter sequence from **Prerequisites** in the **Query sequence** field | **Query sequence** field is filled by sequence from **Prerequisites**|
| 5 | Select **blastp** search selector | **blastp** value is displayed in **Algorithm** field by default  |
| 6 | Fill **Task title** field by any value (e.g. "exclude organism") | |
| 7 | Select **Refseq Selected Proteins** database from the dropdown in the **Database** field||
| 8 | Type and select **Homo sapiens (taxid: 9606)** in **Organism** field| |
| 9 | Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID <li>  Title specified at step 6 is displayed in the **Task title** column <li> Current state is **Searching...** |
| 10 | Wait until status changed to **Done**	| <li>Current state changed to Done <li> Task ID becomes a hyperlink |
| 11 |	Click on last search ID in the Task ID column	| <li> The name of the task is displayed at the top of the tab <li> Collapsed Blast parameters section is displayed <li> The corresponding search results are displayed in the Sequence table of History sub-tab below Blast parameters section <li> **Tax ID** = 9606 only displays in the Organism column |
| 12 | Expand BLAST parameters section | BLAST parameters section contains option **Organisms**: Homo sapiens (taxid: 9606) |
| 13 | Click **Rerun** button for the last task | **Search** sub-tab is opened with the parameters of certain existing search task |
| 14 | Set **Exclude** checkbox for **Organisms** | |
| 15 | Click **Search** button| <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task is created with auto-generated ID |
| 16 | Click on last search ID in the Task ID column | <li> The name of the task is displayed at the top of the tab <li> Collapsed Blast parameters section is displayed <li> The corresponding search results are displayed in the Sequence table of History sub-tab below Blast parameters section <li> **Tax ID** = 9606 isn't shown in the Organism column |
| 17 | Expand BLAST parameters section | BLAST parameters section contains option **Excluded Organisms**: Homo sapiens (taxid: 9606) |