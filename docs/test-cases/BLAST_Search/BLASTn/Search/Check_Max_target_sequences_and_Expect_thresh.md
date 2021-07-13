# Check Max target sequences and Expect thresh additional parameters
Test verifies that
 - number of aligned sequences displayed in results corresponds the Max target sequences
 - all results have E Value less than Expect threshold

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
| 3 | Select **BLAST** panel | **BLAST** panel is displayed first at the right side in the additional panels |
| 4 | Enter sequence from **Prerequisites** in the **Query sequence** field | **Query sequence** field is filled by sequence from **Prerequisites**|
| 5 | Select **blastp** search selector | **blastp** value is displayed in **Algorithm** field by default  |
| 6 | Fill **Task title** field by any value (e.g. "Check Max target sequences") | |
| 7 | Select **Refseq Selected Proteins** database from the dropdown in the **Database** field | |
| 8 | Expand ***Additional parameters*** section | <li> Default value `0.05` is set for ***Expect threshold*** <li> Default value `100` is set for  ***Max target sequences***|
| 9 | Set `-0.05` value to ***Expect threshold*** |  |
| 10 | Click **Search** button |  Error message `Unappropriated value for expectedThreshold.` appears |
| 11 | Set `0.05e-10` value to ***Expect threshold*** |  |
| 12 |  Click **Search** button |<li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID |
| 13 | Wait until status changed to **Done** | Task ID becomes a hyperlink |
| 14 | Click on last search ID in the Task ID column	| <li> Collapsed Blast parameters section is displayed <li> The corresponding search results are displayed in the Sequence table of History sub-tab below Blast parameters section <li> The table is sorted by E value column (ascending) <li> All sequences have `E Value` that less or equal value specified at step 11  <li> There are more than 10 rows in the table|
| 15 | Expand ***Additional parameters*** section | **Additional parameters** section contains option ***Max target sequences: 100*** and ***Expect threshold: 0.05e-10*** |
| 16 | Click **Rerun** button for the last task | **Search** sub-tab is opened with the parameters of certain existing search task |
| 17 | Expand ***Additional parameters*** section | |
| 18 | Set `-10` value to ***Max target sequences***|  |
| 19 | Click **Search** button|  Error message `Unappropriated value for maxTargetSequence.` appears |
| 20 | Set `10` value to ***Max target sequences*** |  |
| 21 | Click **Search** button| <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task is created with auto-generated ID |
| 22 | Click on last search ID in the Task ID column | <li> The corresponding search results are displayed in the Sequence table of History sub-tab below Blast parameters section <li> There are 10 or less rows in the table  |
| 23 | Expand BLAST parameters section |  |
| 24 | Expand ***Additional parameters*** section | **Additional parameters** section contains option ***Max target sequences: 10*** and ***Expect threshold: 0.05e-10*** |