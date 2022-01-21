# Motifs search for whole Reference

Test verifies
- Motifs search for whole Reference, displaying results in new MOTIFS panel and on tracks (positive and negative strands)

**Prerequisites**:

- dataset = **sample1**, .vcf = **sample_1-lumpy.vcf**
- Sequence for search: `gggttcatgaggaagggcaggaggagggtgtgggatggtg`

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **Datasets** panel  | |
| 3 | Select dataset, vcf file from **Prerequisites** | VCF file (**sample_1-lumpy.vcf**) is selected in dataset (**sample1**)|
| 4 | Go to **Browser** panel|
| 5 | Set **CHR: 1** in coordinates | **REFERENCE** track is displayed |
| 6 | Click **General** dropdown menu in **REFERENCE** track|  | 
| 7 | Select **Motifs search** in the context menu  |  |
| 8 | Fill the fields in the window: <li>**Pattern** = sequence from **Prerequisites** <li> Mark *Search whole reference* checkbox | <li> **Titile** field is empty <li> **Search** button is enabled|
| 9 | Click **SEARCH** button|<li> **Motifs search** pop-up is closed <li> **MOTIFS** panel with details is displayed first at the right side in the additional panels <li> The search process is displayed at the top of the panel as a loader with the heading **Loading results** |
| 10 | Wait until the search process is completed  | The loader is disappeared. Motif search results are displayed in the table: <li> Search results match to expected result from *Test data* table from the Prerequisites for corresponding Search Sequence <li> Search results contains 8 rows for different chromosomes (expected search results are shown under this table)  <li> Search results are sorted ascending by *Chromosome* column and by *Start* column inside the one chromosome | 
| 11 | Go to **BROWSER** panel| Two tracks are displayed in the panel: <ul><li> `<Motif>_positive` <li> `<Motif>_negative`, <br> where `<Motif>` is the search sequence from **Prerequisites** specified at step 8 </ul>  |
| 12 | Look at `<Motif>_positive` and `<Motif>_negative` tracks | <li> `<Motif>_positive`: <ul><li> The Sequence matching the specified search pattern is displayed on the track <li> The sequence is shown as colorized rectangle (blue color by default) <li> The rectangle has white arrows inside in the direction of the positive strand (left to right)</ul><li> `<Motif>_positive` <li> **General** context menu is displayed in both track's headers |
| 13 | Click the row with other Chromosome value in this Search results details table | <li> Chromosome in **Browser** coordinates is changed to the Chromosome value from selected row of details table <li> `<Motif>_positive` and `<Motif>_negative` tracks aren't closed <li> Track corresponding to the selected row is shown in the `<Motif>_positive` (or `<Motif>_negative`) tracks |
| 14 |Click **<** button| The table contains search task runned at step 9 with follow values: <li>**Name** is empty <li>**Motif** displays search pattern from **Prerequisites** <li>**Search type** is ***Reference*** 

**Expected search results:**

| Chromosome | Start | End | Strand | Gene |
| :---: | --- | --- | --- | --- |
| 1 | 19328 | 19367 | + | WASH7P |
| 1 | 189851 | 489890 | + | FO538757.2 |
| 15 | 101971594 | 101971633 | - | WASH3P |
| 16 | 19017 | 19056 | + |Z84812.4 |
| 19 | 60936 | 60975 | + | WASH5P |
| 2 | 113594060 | 113594099 | - | WASH2P |
| 9 | 19441 | 19480 | + | WASH1 |
| X | 156020694 | 156020733 | - | AJ271736.10 |