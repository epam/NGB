# Basic test for regular VCF track

Test verifies displaying variations with different types on the VCF track.

**Prerequisites:**
- Dataset : **Fruitfly**,
- VCF file : **agnX1.09-28.trim.dm606.realign.vcf**

**Preparations**
1. Login to NGB
2. Check that **Browser** and **Datasets** panels open. If any of them is missing click the **Views** menu located on the main toolbar and select needed panel

| Steps | Actions | Expected results |
|:---:| :--- |:---|
| 1 | Login to NGB | |
| 2 | On the **Datasets** panel click on the arrow icon near the dataset from the Prerequisites |  |
| 3 | In the files list select VCF file from the Prerequisites |  |
| 4 | Go to **Browser** panel | Dataset Summary view is shown and contains: <li> VCF file from the Prerequisites <li> 3 charts depicting the variations' breakdown as follows: <ul><li> **_Variants by chromosome_** that contains 1 bar : for **_chrX_** with value `85` <li> **_Variants type_** that contains 3 bars: **DEL** (43), **SNV** (36), **INS** (6) <li> **_Variants quality_** that shows variants statistic along chromosome coordinates |
| 5 | Click the bar on the **_Variants by chromosome_** chart | <li> _REFERENCE_ track, _GENE_ track and _VCF_ track for VCF file from the Prerequisites are displayed in the Browser panel on whole chromosome **`X`** scale <li> Message `Zoom in to see variants. Minimal zoom level is at 500kBP` is shown for VCF track |
| 6 | Set coordinates `12570701 - 12604582` and click Enter | The variations are shown as 7 bubbles with numbers (`2, 5, 9, 4, 13, 21, 29`) showing the number of variations grouped under each bubble |
| 7 | Set coordinate `12591700` and click Enter | Variation with _SNV_ type has <li> the label `->` above bar <li> `alt` and `ref` letters (`G->T`) above label |
| 8 | Set coordinate `12592107` and click Enter | Variation with _DEL_ type is shown in the `12592108` position and has <li> the label **`-`** above bar <li>  letter `A` above label | 
| 9 | Set coordinate `12590807` and click Enter | Variation with _INS_ type is shown in the `12590807` position and has <li> the label **`+`** above bar <li>  letters `CT` above label |
