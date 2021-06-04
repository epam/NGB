# BLASTn search from Gene (exon only)

Test verifies
 - BLASTn search from GENE (exon only) track 

**Prerequisites**:

dataset = **dm6_data**, .gtf = **[dmel-all-r6.06.sorted.gtf.gz](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/dmel-all-r6.06.sorted.gtf.gz)**
.vcf = **[agnts3.09-28.trim.dm606.realign.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.dm606.realign.vcf.gz)**

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to  **Variants** panel | |
| 3 | Find Variation on Position **chr ?**|  |
| 4 | Click on Variation on Position **chr ?**| BAM and VCF tracks are opened in the Browser window |
| 5 | Go to GENE track| | 
| 6 | Click on GENE alignment **specific or any**? | Context menu is displayed <li> **BLASTn Search** and **BLASTp Search** display between **BLAT Search** and **Copy info to clipboard** |
| 7 | Click on **BLASTn Search** in the context menu | 2 sub-items appear to the right of the context menu: <li> **Exon only** <li> **All transcript info** |
| 8 | Select **Exon only** | <li> **BLAST** panel is opened first at the right side in the additional panels <li> **blastn** search tool is selected by default|
| 9 | Look at **Query Sequence** field | The corresponding sequence displays in the **Query Sequence** field|
| 9 | Go to **Task title** field | | 
| 10| Fill **Task titile** field by any value (e.g. Test BAM nucleotide) | | 
| 11 | Go to **Database** field| **** databace chosen by default|
| 12| Select **** database from the dropdown| |
| 13| Go to **Organism** -> Select **** in the dropdown list| |
| 14| Click on **+Organism** button | New **Organism** field is appered with **Exclude** checkbox under **Organism** field
| 15| Select **** in the dropdown list of Organismm field| |
| 16| Set checkbox in **Exclude** | |
| 17| Go to **Algorithm** field| **megablast** value is displayed by default |
| 18| Go to **Additional parameters** | ??? |
| 19| Click on **Search** button | <li> Search is started <li> **History** sub-tab is opened automatically <li> A new search task created <li> Current state  |
| 20| Look at the sub-tab **History** at the end of the search| Search results are displayed in the **History** sub-tab |
| 21| Click on the first sequence ID in the **Sequence ID** column |  form with details about all matches (alignments) of the search query to the certain sequence shall be opened. <li> "Alignments" form should be opened in the same tab ("History"), |
| 22| Click on **View at track** button| The pop-up for an alignment of the nucleotide sequence|
