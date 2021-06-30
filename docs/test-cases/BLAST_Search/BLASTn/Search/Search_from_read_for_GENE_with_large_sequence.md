# BLASTn search from read (GENE)

Test verifies
 - BLASTn search from GENE track with large sequence
  
**Prerequisites**:

 - dataset = **Felis_catus**, .gtf = **[Felis_catus.Felis_catus_9.0.94.sorted.gtf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/Felis_catus.Felis_catus_9.0.94.sorted.gtf)**
.bam = **[SRR5373742-1m.bam](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.SRR5373742-1m.bam)**
 - [Sequence of BRCA2-201 transcript for BRCA2 gene](Sequence_data/Sequence_of_BRCA-201_transcript.md) 
 - Go to NGB
 - Close **Blast** panel if it is opened

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** tab  | |
| 3 | Select dataset, bam file from **Prerequisites** | <li> Bam file (**SRR5373742-10m.bam**) is selected in dataset (**Felis_catus**)|
| 4 | Go to **Browser header**| 
| 5 | Set checkbox **Felis_catus.Felis_catus_9.0.94.sorted.gtf** in the **FELIS_CATUS** dropdown list (if it is not set)  | 
| 6 | Go to Coordinates and search input control at right of tab's header|  | 
| 7 | Enter **A1: 11534536 - 11650701** in the **TYPE COORDINATES** and click **Enter**| **BRCA2** gene displays in the Browser|
| 8 | At the gene-track, select **'Expanded'** view| | 
| 9 | Click on **BRCA2-201** transcript of **BRCA2** gene | <li> Context menu is displayed <li> **BLASTn Search** and **BLASTp Search** display between **Show Info** and **Show 3D Structure** <li> **BLASTn Search** and **BLASTp Search** are disabled <li> **Query maximum lenght (50000bp) exceeded** displays below **BLASTn search**|
| 10 | Click on **BLASTn Search** in the context menu | The BLAST panel is not opened|
| 11 | Go to **Views** ||
| 12 | Click on **BLAST** | **BLAST** panel is opened| 
| 13 | Set sequence from **Preconditions** (Sequence of BRCA2-201 transcript for BRCA2 gene) in the **Query sequence** field | <li> Error message is displayed under Query sequense line: **Sequence length should be less than or equal to 50000** <li> **Search** button is disabled|