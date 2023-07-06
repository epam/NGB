# Basic test for BAM track

Test verifies that
- bars on the Coverage track get colored only if a certain threshold is exceeded.

**Prerequisites:**
- dataset : **Fruitfly**,
- BAM file : **CantonS.09-28.trim.dm606.realign.bam**

**Preparations**
1. Login to NGB
2. Check that **Browser** and **Datasets** panels open. If any of them is missing click the **Views** menu located on the main toolbar and select needed panel
3. Go to **_Settings_** >> **_Alignments_**. In the **_Coverage Options_** section set _Allele frequency threshold_ setting to `0.1`.

| Steps | Actions | Expected results |
|:---:| :--- |:---|
| 1 | Login to NGB | |
| 2 | On the **Datasets** panel click on the arrow icon near the dataset from the Prerequisites |  |
| 3 | In the files list select BAM file from the Prerequisites |  |
| 4 | Go to **Browser** panel | Dataset Summary view with files list including BAM file from the Prerequisites is shown |
| 5 | Select a chromosome **`X`** from the chromosome selector dropdown | _REFERENCE_ track, _GENE_ track and _BAM_ track for BAM file from the Prerequisites are displayed in the **Browser** panel on whole chromosome **`X`** scale `1 - 23542271` |
| 6 | Set coordinate `12584341` and click Enter | |
| 7 | Zoom in to ~100bp using +/- buttons located in the right part of the track view |  |
| 8 | On the Coverage track hover over under the coordinate `12584341` | The tooltip with the following basic information appears: <br> <code> Count 171 <br> A: 8(4.68%) <br> C: 160(93.57%) <br> G: 0(0%) <br> T: 3(1.76%) <br> N: 0(0%) <br> DEL: 3 <br> INS: 33</code> <br> |
| 9 | Check bar for coordinate `12586062` on the Coverage track | Corresponding bar has default colour only |
| 10 | Check coordinate `12584341` on the _REFERENCE_ track | Letter **`C`** is shown |
| 11 | Set coordinate `12586062` and click Enter |  |
| 12 | On the Coverage track hover over under the coordinate `12586062` | The tooltip with the following basic information appears: <br> <code> Count 341 <br> A: 166(48.69%) <br> C: 174(51.03%) <br> G: 0(0%) <br> T: 1(0.3%) <br> N: 0(0%) <br> DEL: 0 <br> INS: 0</code> <br> |
| 13 | Check that bar for coordinate `12586062` is colored with mismatches distribution at a specific locus on the Coverage track | Corresponding bar is colored in red by 51% |
| 14 | Check coordinate `12586062` on the _REFERENCE_ track | Letter **`A`** is shown |
