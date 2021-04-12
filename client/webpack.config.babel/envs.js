'use strict';

const fs = require('fs');
const path = require('path');

console.log('here2', global.buildOptions);

let envMode = 'production';
const isTestMode = global.buildOptions.env === 'test';
if (global.buildOptions.dev) {
    envMode = 'development';
}
const dotenvFiles = [
    path.resolve(__dirname, `../.env.${envMode}.local`),
    path.resolve(__dirname, `../.env.${envMode}`),
    // Don't include `.env.local` for `test` environment
    // since normally you expect tests to produce the same
    // results for everyone
    !isTestMode && path.resolve(__dirname, '../.env.local'),
    path.resolve(__dirname, '../.env')
].filter(Boolean);

// Load environment variables from .env* files. Suppress warnings using silent
// if this file is missing. dotenv will never modify any environment variables
// that have already been set.  Variable expansion is supported in .env files.
// https://github.com/motdotla/dotenv
// https://github.com/motdotla/dotenv-expand
dotenvFiles.forEach(dotenvFile => {
    if (fs.existsSync(dotenvFile)) {
        require('dotenv-expand')(
            require('dotenv').config({
                path: dotenvFile
            })
        );
    }
});
