# Requirements

**[Node.js >= 6.9.5](https://nodejs.org/en/download/package-manager/)** 

# How to build NGB client

Obtain the source code from github:
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB/client
```

Update dependencies
```
$ npm install
```

Serve client on [http://localhost:8080/](http://localhost:8080/)
```
$ npm run serve
```

Or build it
```
$ npm run build
```

# NPM scripts

In addition to `serve` and `build`, the following npm scripts are available

All scripts are run with `npm run [script]`, for example: `npm run lint:eslint`.

* `build` - will run build:prod
* `build:dev` - will build the app to the dist folder with dependencies with developer configuration (`http://localhost:8080/` will be used as API endpoint)
* `build:prod` - will build the app to the dist folder with dependencies (relative url will be used as API endpoint, to be ran from war file)
* `serve` - will run serve:prod 
* `serve:dev` - start development server with dev configuration, try it by opening `http://localhost:8080`
* `serve:prod`- start development server with production configuration
* `lint:eslint` - will run es linter
* `lint:scsslint` - will run scss linter (currently used for checks only)

See what each script does by looking at the `scripts` section in [package.json](./package.json)
