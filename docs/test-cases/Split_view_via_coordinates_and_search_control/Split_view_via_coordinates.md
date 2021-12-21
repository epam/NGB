# Splitting browser for location via the coordinates and search control

Test verifies
 - splitting browser for any location of interest - via the unified coordinates and search control.

**Prerequisites**:

Dataset = **SV_Sample2**, .vcf = **[sample_2-lumpy.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/sample_2-lumpy.vcf.gz)**,
gene = [Homo_sapiens.GRCh38.gtf.gz](https://ngb-oss-builds.s3.amazonaws.com/public/data/genome/grch38/Homo_sapiens.GRCh38.gtf.gz)


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to **DATASETS** panel||
| 3 | Select **SV_Sample2** dataset| **Summary** view is displayed in the **BROWSER** panel|
| 4 | Go to coordinates control in the **BROWSER** header | |
| 5 | Set 2 coordinates separated by space: **2: 29223860** **X: 276322 - 303356**| |
| 6 | Click **Enter** button| <li> BROWSER panel is splitted on two panels:  <br>main one by default,<br>and the additional panel|
| 7 | Look at main BROWSER window| <li> **Chr 2: 29223860 - 29223860** coordinate displays in the BROWSER <li> The unified coordinates and search control are avaliable to changing | 
| 8 | Look at additional BROWSER window|<li> **Chr X: 276322 - 303356** coordinate displays in the BROWSER <li> The unified coordinates and search control are disabled to changing <li>**Expand** and **Close** buttons are display in the header of additional BROWSER only| 
| 9 | Repeat steps 4-9 for every 2 pair of coordinates shown in the table below| 
 <br>

| Steps | Coordinates Format |First Coordinate| Second Coordinate| Expected results |
| :---: | --- | --- | --- | --- | 
| 1 | <br>Chr:Coordinate <br>Chr: Start-Finish coordinate| 2: 29223860 | X: 276322 - 303356 |<li>Main Browser =  Chr 2: 29223760-29223960 <br> Central Position =  **29223860**<li>Additional Browser = **Chr X: 276322 - 303356**|
| 2 | <br>Chr: Start-Finish coordinate <br> Feature Name | 1: 11869 - 14409 | KRAS | <li>Main Browser =  **Chr 1: 11869 - 144090** <li>Additional Browser = Chr 12: 25204789 - 25250936 <br>**KRAS** gene displays in the **GENE** track |
| 3 | <br> Feature Name <br> Coordinate | BRCA1| 11869 | <li>Main Browser = Chr 17: 43044295 - 43170245 <br> **BRCA1**  gene displays in the **GENE** track  <li>Additional Browser = Chr 12: 11769 - 11969 <br> Central position = **11869** <br>*(the location is opened on the current chromosome)*|
| 4 | <br> Coordinate <br> Start-Finish Coordinate | 31358993 | 1020123 - 1056118| <li>Main Browser =  Chr 17: 31358893 - 31359093 <br> Central Position = **31358993** <br>*(the location is opened on the current chromosome)* <li>Additional Browser = **Chr 17: 1020123 - 1056118**  <br>*(the location is opened on the current chromosome)*|
| 5 | Chr: Coordinate | 4: 25665006 | - | <li> The main window is displayed only. Additional window is closed. <li>Main Browser = Chr 4: 25664906 - 25665106 <br>Central Position = **25665006** |