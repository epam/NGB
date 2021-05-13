# Highlighting variations depending on the set conditions

Test verifies
 - highlighting background of variants in Variants panel and VCF track on Browser in dependence of specified simple conditions in JSON file

**Prerequisites**:

1. dataset = **dm6_data**, .vcf = **[agnts3.09-28.trim.dm606.realign.vcf](https://ngb-oss-builds.s3.amazonaws.com/public/data/demo/ngb_demo_data/agnts3.09-28.trim.dm606.realign.vcf.gz)**
2. **interest_profiles.json** that contains **'Complex Conditions'** profile with conditions
```
{
  "Simple Conditions": {
    "is_default": false,
    "conditions": [
      {
        "highlight_color": "bc32d0",
        "condition": "BaseQRankSum == -0.058"
      },
      {
        "highlight_color": "ffff00",
        "condition": "AN != 1"
      },
      {
        "highlight_color": "add8e6",
        "condition": "FS > 96.900"
      },
      {
        "highlight_color": "dddddd",
        "condition": "MQ >= 91.95"
      },
      {
        "highlight_color": "78df72",
        "condition": "BaseQRankSum < -11.900"
      },
      {
        "highlight_color": "d3ca3b",
        "condition": "QD <= 0.32"
      },
      {
        "highlight_color": "f1eefb",
        "condition": "AC  in [1,1,3]"
      },
      {
        "highlight_color": "e89baa",
        "condition": "MLEAC notin [1]"
    }
    ]
  }
}
```

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB as system admin user | |
| 2 | Go to  **Settings** -> **VCF** tab | 'VCF' tab is displayed in the Settings' window|
| 3 | Set checkbox in **Enabled** field (if it is not set)| **select the profile** field is displayed |
| 4 | Choose **Complex Conditions** profile in dropdown list of **'select the profile'** field **(if it is not default)** | 
| 5 | Click on **Save** button | <li> Changes successfully saved <li> 'Settings' window is closed | 
| 6 | Go to **Datasets** tab | 
| 7 | Choose dataset and vcf file from **Prerequisites** | Vcf file (**agnts3.09-28.trim.dm606.realign.vcf**) is chosen in dataset (**dm6_data**) 
| 8 | Go to **Variants** panel | |
| 9 | Find Variation with **Position** = **12585001**| Background of variation is highlighted in the **'bc32d0'** color |
| 10| Click on variation with position = 12585001 |Browser window is opened with vcf track |
| 11| Look at variation on vcf track in Browser  | Variation is highlighted with the color  = **'bc32d0'** on full genome variation
| 12| Go steps 8-12 for every variation's position shown in the table below |   | 
 


| Steps | Condition | Position | Expected results |
| :---: | --- | --- | --- | 
| 1 | BaseQRank Sum == **- 0.058** | 12585001| <li> 'DEL' variation is highlighted by **'bc32d0'** color in variants panel and browser<li> BaseQRankSum = - 0.058 value | 
| 2 | An != **1** | 12584271 | <li>'DEL' variation is highlighted by **'ffff00'** color in Variants Panel and VCF tracker in Browser <li>An = 2 |
| 3 | Fs > 96.900 | 12590846 | <li>'INS' variation is highlighted by **'add8e6'** color in Variants Panel and  VCF tracker in Browser <li>Fs = 97.989|
| 4 | Mq >= 91.95 | <li>12585943 <li>12587867 | <li> INS variation is highlighted by **dddddd**, MQ = 91.95 <li> DEL variation is highlighted by **'dddddd'**, MQ = 92.2
| 5 | BaseQRankSum < -11.900| 12591450 | <li>DEL variation is highlighted by .**'78df72'** <li>BaseQRankSum = - 11.917
| 6 | Qd <= 0.32| <li> 12589261 <li>12591635 | <li> DEL variation is highlighted by **'d3ca3b'**, Qd = 0.32 <li>DEL variation is highlighted by **'d3ca3b'**, Qd=0.26 
| 7 | Ac  in [1,1,3] | 12591755 | <li> DEL variation is highlighted by **' f1eefb'** <li> AC = [1,1] |
| 8 | MLEAC notin [1]| 12586560 | <li> SNV variation is highlighted by **'e89baa'** <li> MLEAC = 2