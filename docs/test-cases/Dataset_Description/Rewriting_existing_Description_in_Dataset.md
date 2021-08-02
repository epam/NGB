# Adding description filr to Dataset with  description
Test verifies
 - that when adding one more description file to the dataset, the old file will be overwritten with the new one.

**Prerequisites**:

1. dataset = **SV_Sample1**, .vcf = [sample_1-lumpy.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/sample_1-lumpy.vcf.gz) with Description = **[desc.html](https://epam-my.sharepoint.com/:u:/p/dmitrii_krasnov/EcgG29WpqCNLr08gnQiCK-IBiY0MiWZ8NvW8vO9QXwdZAQ?OR=teams)**
2. Description file = **[desc2.html](https://epam-my.sharepoint.com/:u:/p/dmitrii_krasnov/EcgG29WpqCNLr08gnQiCK-IBiY0MiWZ8NvW8vO9QXwdZAQ)**
3. Go to CLI and use the following command for adding one more description to necessary dataset: <br> *template* `ngb add_description <dataset name or ID>  <path to local file>` <br> *example* `ngb add_description SV_Sample1 desc2.html`


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB  | |
| 2 | Go to **Datasets** tab| |
| 3 | Select **SV_Sample1** dataset| <li> Dataset is selected with all related files|
| 4 | Look at **Browser** window | <li> New added Description file from **Prerequisites** **[desc2.html](https://epam-my.sharepoint.com/:u:/p/dmitrii_krasnov/EcgG29WpqCNLr08gnQiCK-IBiY0MiWZ8NvW8vO9QXwdZAQ)** displays in the Browser window <li> **Description** value displays in additional control of view in the header of **Browser** panel|
| 5 | Click on **Description** view in the header of **Browser** panel| <br> Additional control menu is expanded with checkboxes with following values: <li> **Description**  - displays info from **html** file in **Prerequisites** <li> **Summary** - displays files list, variant diagrams <br> **Description** value displays by default|
| 6 | Set checkbox in **Summary** view| <li> **Summary** view with files list, variant diagrams (for VCF files) is displayed in the Browser <li> **Summary** value displays in the header of **Browser** panel|
