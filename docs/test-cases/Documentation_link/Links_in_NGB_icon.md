# Documentation link in NGB icon

Test verifies
- documentation link in the NGB icon in the main menu

| Steps | Actions | Expected results |
| :---: | --- | --- |
| 1 | Login to NGB | |
| 2 | Go to main menu | |
| 3 | Click **NGB** icon (first in the menu)| Tooltip is appeared with values in the following order: <li> Application version number in the upper right corner <li> **NEW GENOME BROWSER** (name of application) <li> **Report issues: https://github.com/epam/NGB/issues** *(goes to NGB issues in GitHub)* <li> **Documentation: http://oss-1204092014.us-east-1.elb.amazonaws.com:8080/catgenome/docs/index.html**. |
| 4 | Click on the link in the **Documentation** field| <li> HTML page is opened with NGB documentation in a **new** browser window |