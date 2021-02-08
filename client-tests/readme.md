## Description

This folder contains visual regression tests for [client](./../client).

There is a few `examples` in `tests/acceptance` folder.


## Usage


Install packages in this folder, using:

```sh
yarn
```



### Basic usage

Run dev backend and tests:

```sh
yarn ci
```

### Usage with already running dev server

Run tests:

```sh
yarn test
```


## Folder structure

`./test/` - all files, related to test setup.

`./test/test-support/` - includes test setup files`

`./test/test-support/data-storage/` - tracks list for every test case.

`./test/test-support/page-object/` - shared selectors for dom nodes, used inside tests.

`./test/test-support/types/` - types for tracks objects.

`./utils/` - list of useful utils.

## Requirements

* `node >= 12`
* `yarn < 2`


## Utils

### `url-converter`

Used to get state information from url (and use it in tests)

Extracting `referenceId`, `chromosome`, `start`, `end` and `tracks` properties from provided list of urls.


1. Put list of urls into `./utils/url-converter/input.txt`
1. Run `node ./utils/url-converter/index.js`
1. Check `./utils/url-converter/output.json`


### `har-extractor`

Used to extract json payload from har file, and map it to endpoints.
Accept any `*.har` file in dir.
Result will be saved in `output.json`


1. Copy har file into `./utils/har-extractor/`
1. Run `node ./utils/har-extractor/index.js`
1. Check `./utils/har-extractor/output.json`

## Credits

* [@playwright/test](https://github.com/microsoft/playwright-test) - test runner

