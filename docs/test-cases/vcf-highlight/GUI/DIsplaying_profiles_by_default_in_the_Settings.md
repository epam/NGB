
# Logic of displaying profiles in the "Select the profile" field by default

Test verifies
 - 
 - logic of displaying of the profile name in the 'select the profile' field by default,depending on the set flag in the 'is default" field in JSON file

**Prerequisites**:
 -  **interest_profiles.json** that contains **2 profiles** with conditions **'Profile 1'** and **'Profile 2'**:
```
 {
     "Profile 1": {
 "is_default": false,
    "conditions": [
      {"highlight_color": "ff0000",
        "condition": "MQ == 2.81"},
      ]
},
"Profile 2": {
 "is_default": false,
    "conditions": [
      {"highlight_color": "fff000",
        "condition": "DP == 572"},
      ]
}
 }
```

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB as system admin user | |
| 2 | Go to  **Settings** -> **VCF** tab | 'VCF' tab is displayed in the Settings' window|
| 3 | Set checkbox in **Enabled** field | <li> **'Select the profile'** field is displayed  |
| 4 | Look at **'Select the profile'** field | <li>**'Select the profile'** field is empty <li> **Save** button is **disabled** |
| 5 | Go to **interest_profiles.json** from **Prerequisites**| |
| 6 | Set **'true'** value in the **"is default"** field for **Profile 2** and save changes | **"is_default": true** in the **Profile 2** | 
| 7 | Repeat steps 1-4| <li> **Profile 2** is displayed by default in the **'Select the profile'** field <li> **Save** button is **enabled**|
| 8 |  Go to **interest_profiles.json** from **Prerequisites**| |
| 9| Set **'true'** value in the **"is default"** field for **Profile 1** and save changes | **"is_default": true** in the **Profile 1** and **Profile 2** | 
| 10| Repeat steps 1-4 | <li>**Profile 1** is displayed by default in the **'Select the profile'** field as first profile in the **interest_profiles.json** file<li> **Save** button is **enabled**|
