# Removing Description from Dataset
Test verifies
 - that the Description file is not displayed in the Browser after removing it from the dataset.

**Prerequisites**:

1. dataset = **SV_Sample1**, .vcf = [sample_1-lumpy.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/sample_1-lumpy.vcf.gz) with Description file
2. Description file = **[desc.html](https://epam-my.sharepoint.com/:u:/p/dmitrii_krasnov/EcgG29WpqCNLr08gnQiCK-IBiY0MiWZ8NvW8vO9QXwdZAQ?OR=teams)**
3. Go to CLI and use the following command for removing description to necessary dataset: <br> *template* `ngb remove_description <dataset name or ID> ` <br> *example* `ngb remove_description SV_Sample1`


| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB  | |
| 2 | Go to **Datasets** tab| |
| 3 | Select **sample_1-lumpy.vcf** in the **SV_Sample1** dataset| <li> File is selected <li> The browser window displays **Summary** view with files list, variant diagrams (for VCF files) <li> Additional control with **Summary, Description** values don't display in the header of **Browser** panel|
| 4 | Select **SV_Sample1** dataset| <li> Dataset is selected with all related files <li> The browser window displays **Summary** view with files list, variant diagrams (for VCF files) <li> Additional control with **Summary, Description** values don't display in the header of **Browser** panel|

