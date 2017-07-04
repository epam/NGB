CLI integration tests
---------------------------------

This folder contains:
 - `cli-tests.gradle` - test launcher script
 - `testcases.csv` - list of test cases to run
 
 In order to launch CLI integration tests run gradle task `integrationCliTest` from the root NGB folder.
 
 Task's parameters are passed through gradle property `-P`. The following parameters should be specified:
 - _cliPath_ - full path to ngb-cli executable (e.g.: `-PcliPath=/path/to/build/ngb-cli/bin/ngb`)
 - _resourcePath_ - absolute path to the test data folder. Every `$resourcePath` substring in CSV with test cases will be replaced by the given path
 - _resourceRelativePath_ - relative path to the test data folder. Every `$resourceRelativePath` substring in CSV with test cases will be replaced by the given path
 - _testCases_ [OPTIONAL] - the path to CSV file with test cases, if not specified `testcases.csv` in a current disrectory will be used
 
Example:
```shell
$ ./gradlew integrationCliTest -PcliPath=/ngb/dist/ngb-cli/bin/ngb -PresourcePath=/ngb/testdata
```

CSV structure
---------------------------------

CLI integration tests imply the following conventions and CSV format for test cases description:
 - Comment lines start with `@` symbol. Comment lines are ignored during tests execution along with empty lines.
 - Each line representing a test case contains the following fields separated by `,` symbol:
     1. Test case name. If name starts with `#` symbol, test will be skipped
     2. CLI commands to launch **before** the test (separated by `;` symbol, `$resourcePath` can be used)
     3. Test command (`$resourcePath` can be used)
     4. CLI commands to launch **after** the test (separated by `;` symbol, `$resourcePath` can be used)
     5. Regex of expected standard output of the test, leave empty if no standard output is expected
     6. Regex of expected error output message of the test, leave empty if no error output is expected
 - Each test case is expected to be independent from other cases, so make sure that all the necessary preparations are made in
 **before** command section and everything is cleaned up in **after** command section.
