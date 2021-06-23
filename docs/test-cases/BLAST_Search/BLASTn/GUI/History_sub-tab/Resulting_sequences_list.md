# View job details: Resulting sequences list

Test verifies
 - that User has the ability to view the specific BLAST search task results

**Prerequisites**
 - Sequence for search:
`GAAATTGTCCAAAGATAGTTACCTCTCATAGGACCCCTCACTGACAGCATCCCCTAGCCGCACGTGACTAGTTAACTTAATTGAAAGTAAACGTTTAAAATTCTGTTCTTGAGTCGCGCTTCCCCCGTTTCAAATGCTTCATGTGGCTAGTGGCGACTCCGTTGGACAGCACAAACACGGAACGCTCCCATCCTCGCAGTGAGTTCAGCTACCGTCCCAAAAGATA`
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed after Variants panel|
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default |
| 4 | Enter sequence from **Prerequisites** in the **Query sequence** field | **Query sequence** field is filled by sequence from **Prerequisites**|
| 5 | Fill **Task title** field by any value (e.g. "Resulting sequences list") | |
| 6 | Select **Homo_sapiens.GRCh38** database from the dropdown in the **Database** field||
| 7 | Select **blastn** value in the **Algorithm** dropdown field| |
| 8 | Click **Search** button and wait until status changed to **Done**| <li> **History** sub-tab is opened automatically <li> A new search task created with auto-generated ID |
| 9 | Click on last search ID in the **Task ID** column |The corresponding search results are displayed in the **Sequence table** of **History** sub-tab below **Blast parameters** section and contains the following columns: <li> *Sequence ID* <li> *Organism* <li> *TaxID* <li> *Max score* <li> *Total score* <li> *Query cover* <li> *E value* <li> *Percent identity* <li> *Matches* <br> Table is sorted by E value column (ascending) by default|
| 10 | Change the columns order | |
| 11 | Check that table can be sorted by any column | The table is sortable by any column |
| 12 | Sort the table by any column | |
| 13 | Close BLAST tab | |
| 14 | Go to  **Views** menu on the main toolbar | |
| 15 | Select **BLAST** panel | |
| 16 | Click on last search ID in the **Task ID** column | The corresponding search results are displayed in the **Sequence table** of **History** sub-tab below **Blast parameters** section <li> Table is sorted by E value column (ascending) <li> Column order specified at step 10 is kept |