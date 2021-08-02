# Selecting multiple datasets with description files
Test verifies
 - that the Description file is displayed in the Browser when the dataset (with description) (or any file in dataset) is selected. 

**Prerequisites**:

1. dataset = **SV_Sample1** with  Description file = **[desc.html](https://epam-my.sharepoint.com/:u:/p/dmitrii_krasnov/EcgG29WpqCNLr08gnQiCK-IBiY0MiWZ8NvW8vO9QXwdZAQ?OR=teams)**
2. dataset = **SV_Sample2** with  Description file = **[desc2.html](https://epam-my.sharepoint.com/:u:/p/dmitrii_krasnov/EcgG29WpqCNLr08gnQiCK-IBiY0MiWZ8NvW8vO9QXwdZAQ)**
3. dataset = **Lactobacillus** without description file
4. Go to CLI and use the following command for adding description to necessary dataset: <br> *template:* `ngb add_description <dataset name or ID>  <path to local file>` <br> *example:* <br> 1) `ngb add_description SV_Sample1 desc.html` <br> 2) `ngb add_description SV_Sample2 desc2.html`


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB  | |
| 2 | Go to **Datasets** tab| |
| 3 | Select **SV_Sample1** dataset| <li> Dataset is selected with all related files <li> The browser window displays info from **html** file in **Prerequisites** <li>**Description** value displays in the header of **Browser** panel|
| 4 | Select **SV_Sample2** dataset| <li> **Summary** view with files list, variant diagrams (for VCF files) is displayed in the Browser window <li> Additional control with **Summary, Description** values don't display in the header of **Browser** panel|