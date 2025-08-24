# BLAST History - Clear jobs History

Test verifies that
 - user have the ability to clear all his/her history of BLAST searches
 - all searches performing at the moment are also canceled  and simultaneously removed from the history list too

**Prerequisites**
 - Sequence for search:
`TCGTAACCCGGAAGGGGAATATTTTTCTGGCTATTGTGTTGTTATTTTCAAGCTGCTGCAGTTTCTGTGGCCAAGGGAACCGTCGGGGAAGGATGGTGTGCGAAAAATGTGAGTTAAGAGGCCGCATCTTTGTGAGAAGGAGGCTAGGAGGAGCTAACTGGGCTTCTTCGGGTCCTTGTCTTTTGTTTATTTTTCTTTCAGCCTCAGTTCTCGTGTTCCAGCCTCTGTCCTCTTACCCTTTTTGACCATTCCTGTCCCTCCCCAACGATCGTATATTCGTTTTGCGTTCAGAGTGGTCTCAGTTTTGTAGATACTTGCTTATTTCCTTTGCCTTCCCCTCCAACCCGGGATCTTATCCCATACAGTTCTTGACTGGCTCCTTCGCTTTAACAGACCCTCAGGGTTGGACTGGATCTTAAAAGTCCTAGAGTTGCCAGATTTAACAGACATTTAAAAAGAGGAATACCTTTTTTCTAGAAGTATGTCCCATGCAATATTTGAGACATGTCTTTGCGTGCTCTTCCTCACTACTGAAAGTACTTTTGACTGACTTTTAGGACATGACAGTACTGTTGTTTTGAAATTAAAACATTCGTTGTTTTGAAATTAAAAATTCACAGGGCATCCTATATTTTAACTAGCTACCCTAAGTTAGCATCCTATCCAATGGCTTAGATACTCTTTGCATGCATACCTGTGGTATTGAGGCAGGGTTCAGAAAGTCTCACCACCTTCAAAGCAGTCTGTGTAAATGTCACTAACTTTTAGAAAAGGCTTTCCACCCTGTAGTCGCCATCTGGTGATTCTAATGCAAACCCTTAAGGTTTCTAGAAAGTTTCTTACGATTTTTTGATATCCTAGACTGTCTATACGTTTTAAAACGGCTGTCATATCCCTAAGTCTTTTTATTCCTCAGCCTCTCTGGCTCTTCTAGGCAAGGTTTCAAGTCCTCTTATCTGTGTTAACATTAACATGATCAATTTTGTTTATATCCCCCCTAGTAGGTAAAACTTTGAACACAGGCACCCATAATTTTGAATCACAGTTCTTATATTAGGTAAAAGAATAACCATGGGCAAGTTACCTAATATATCTTGGCCCCAGTTTCCTCATCTGAAAAATGAGAATAATACATAATTTATGGGGTTCAGATAATGTAGGTAAAGCCTGATATAAAAGTGCCTTCGCGTATACTCAAAAACTGGTTATTTCTATCATAACCCCCAGTATGTGGCACTGAGAGTTAAACTCTCCTTCATTTTATGAGGGGTAAAGTGGAAATAATGTATCCATTTTTCTATGCTTTATATCATCATCTCACATCAAGCTTTAAAAAAATTGTTGATGGGTGCCTGGGTGGCTCAGTCAGTTAAGTGTCTGGCTCTTGATTTCAGCTTGGGTCATGATCTCACTGTTTGTGAGTTTGAGCCCCACATTGGGCTCTGCGCTAACAGTGAAGAGCCTGCTTGGGATTCTCTCTCTCCCCTCTCTCTCTGCCCCTACCCTGCTTGTTCTCTCTTTCTCAAAATAAATAAACTTAAAAAAATTTTTTTAATTAAAAAAATAAAATAAAAAATTATTGCAATGTAATTGACATAGGTACAATTCACCCATTTAAAATGTATCGTTTTGTTATTTTTAGTATGTCCAGAGGGCTATGCAACCATCACCACTATCGATTTTAGAATATTTTCATCACTCTGAAAAGTACATTAAGCTTTTTGACATTGATGTCCTTTAATTCTATTCTATCCTGTACTTAAGCAGTTTTTTGGTTTTGCTTGTTTGAGTAAATATGGCTTACACATTTAGATTATACACCTTCCGTGATACCTTTTCTCTCATATAGTGTAGTTCTAAACATATAAATTTGTCTTTCTATGTAACTTGTGTTAATTAACTTGTGTTAATTCTGTACATTTTAGCTTTAGAGGACGGGTATTCTGCCTGGTTTAATTTGTGTGATATGCTTTACACCATAAGTGCTTTTTCCCAGGGAAACTAAGGATACTGAGCAAATTGTGAGAGTTAATTTCCTAAGGTACATAAAAATTTTGA`
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Views** menu on the main toolbar| <li> **Views** menu is displayed <li> **BLAST** panel is displayed after Variants panel|
| 3 | Select **BLAST** panel | <li>**BLAST** panel is displayed first at the right side in the additional panels <li> **BLAST** panel has 2 sub-tabs: **Search** and **History** <li> **Search** sub-tab is opened by default  <li> **blastn** search selector chosen by default <li> **Query sequence** , **Task title** ,**Database**, **Organism** fields are blank <li> **megablast** value is displayed in **Algorithm** field by default  <li> **Additional parameters** section is collapsed with blank values inside |
| 4 | Enter sequence from **Prerequisites** in the **Query sequence** field | **Query sequence** field is filled by sequence from **Prerequisites**|
| 5 | Fill **Task title** field by any value (e.g. "Clear jobs History") | |
| 6 | Select **Homo_sapiens.GRCh38** database from the dropdown in the **Database** field||
| 7 | Click **Search** button|  <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task is created with auto-generated ID |
| 8 | Wait until status changed to **Done**| <li> **Current state** changed to **Done** <li> Button **to open the search again** in the "Search" sub-tab (reverse arrow-button) displays **only**|
| 9 | Click **Rerun** button for the last task | **Search** sub-tab is opened with the parameters of certain existing search task |
| 10 | Click **Search** button| <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task is created with auto-generated ID |
| 11 | Click ***Clear History*** button in the right corner of **History** sub-tab | Confirmation dialog with message ***Clear all history?*** appears| |
| 12 | Confirm clearing | <li>All history of BLAST searches is removed<li> Search performing at the moment is also canceled  and removed from the history list too |