# Adding description to Dataset or Displaying Dataset's Description in Browser
Test verifies
 - that the Description file is displayed in the Browser when the dataset (with description) (or any file in dataset) is selected. 

**Prerequisites**:

1. dataset = **SV_Sample1**, .vcf = [sample_1-lumpy.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/sample_1-lumpy.vcf.gz)
2. Description file = **[desc.html](https://epam-my.sharepoint.com/:u:/p/dmitrii_krasnov/EcgG29WpqCNLr08gnQiCK-IBiY0MiWZ8NvW8vO9QXwdZAQ?OR=teams)**
3. Go to CLI and use the following command for adding description to necessary dataset: <br> *template* `ngb add_description <dataset name or ID>  <path to local file>` <br> *example* `ngb add_description SV_Sample1 desc.html`


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB  | |
| 2 | Go to **Datasets** tab.| |
| 3 | Select **sample_1-lumpy.vcf** in the **SV_Sample2** dataset| <li> File is selected <li> The browser window displays Description file from **Prerequisites** <li> **Description** value displays in the header of **Browser** panel|
| 4 | Select **SV_Sample1** dataset| <li> Dataset is selected with all related files <li> The browser window displays info from **html** file in **Prerequisites** <li>**Description** value displays in the header of **Browser** panel|
| 5 | Click on **Description** view in the header of **Browser** panel| <br> Context menu is expanded with checkboxes with following values: <li> **Description**  - displays info from **html** file in **Prerequisites** <li> **Summary** - displays files list, variant diagrams <br> **Description** value displays by default|
| 6 | Set checkbox in **Summary** view| <li> **Summary** view with files list, variant diagrams (for VCF files) is displayed in the Browser <li> **Summary** value displays in the header of **Browser** panel|
