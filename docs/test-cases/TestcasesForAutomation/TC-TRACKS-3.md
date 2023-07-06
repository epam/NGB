# Basic test for GENE track

**Prerequisites**:

Add dataset with GENE file:
- dataset = **Fruitfly**

**Preparations**
1. Login to NGB
2. Check that **Browser** and **Datasets** panels open. If any of them is missing click the **Views** menu located on the main toolbar and select needed panel

| Steps | Actions | Expected results |
| :---: | --- |---|
| 1 | Login to NGB | |
| 2 | On the **Datasets** panel select dataset from the Prerequisites | |
| 3 | Select a chromosome **`X`** from the chromosome selector dropdown | <li> _GENE_ track is shown as a histogram in the **Browser** panel <li> Tooltip with message `At this scale - features density is shown as a histogram. Zoom-in to see exact features.` appears |
| 4 | Set coordinates `X: 124135 - 126948` and click Enter | One transcript is displayed for a gene `CG17636` and has exon-intron structure <br> (where _exons_ are nucleic acid coding sequenceas displaying on as rectangles; <br> _introns_ are the non-coding sequences displaying as a lines connecting exons) |
| 5 | Hover over the gene `CG17636` at the track | The tooltip with details info appears |
| 6 | Click _Transcript view_ link in the Genes track's header menu | |
| 7 | Select _Expanded_ option | 3 gene's transcript variants `CG17636-RA`, `CG17636-RB`, `CG17636-RC` with exon-intron structure are shown |
