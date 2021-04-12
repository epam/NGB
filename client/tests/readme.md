## Description

This folder contains visual regression tests for web client.

There is a few `examples` in `acceptance` folder.


### Basic usage

Run dev backend and tests:

```sh
npm run ci
```

### Usage with already running dev server

Run tests:

```sh
npm run test
```


## Folder structure

`./test-support/` - includes generic test setup files`


In { `test-support`, `acceptance/test-name` } :

`./*/data-storage/` - tracks list for every test case.

`./*/page-object/` - shared selectors for dom nodes, used inside tests.

`./*/test-helpers/` - different test helpers (mocks, url constructors).

same structure may be created per test case ^


`./test-support/types/` - types for tracks objects.

`./utils/` - list of useful utils.

## Requirements

* `node >= 14`


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


## How to add new acceptance test

---

`q:` why we need to create folders for tests?

`a:` folders we could use to have `scoped` data, related for tests (mocks, utils and such)

---

1.) figure out name for test suite, for example `user login`,
1.) create folder, named `user-login` in `tests/acceptance` folder.
1.) create file, named `user-login.spec.ts` in `tests/acceptance/user-login` folder.


basic test file should look like:

* for additional references, check [happy path](./acceptance/happy-path/happy-path.spec.ts) test case.

```ts
import { describe, it, expect } from "../../test-support";
import {
  setupPage
} from "../../test-support/page-object";

describe("Acceptance | User Login |", () => {
  it("is a basic test case ", async ({ page }) => {
    await setupPage(page);

    await page.goto('http://localhost:8080/some-endpoint');

    // wait for dom node to appear
    await page.waitForSelector(".ngb-variant-density-diagram .nvd3-svg g");

    // animations
    await page.waitForTimeout(1000);

    // get element node representation
    const element = await page.$(`.ngb-variant-density-diagram`);

    // create element screenshot
    const screenshot = await element.screenshot();

    // assert it with already saves screenshot for this test
    expect(screenshot).toMatchSnapshot(`density-diagram.png`);
  });

});
```