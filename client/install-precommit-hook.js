'use strict';

/* global __dirname:false */
/* eslint angular/log: 0 */

const fsExtra = require('fs-extra');
const path = require('path');

const srcFile = path.join(__dirname, 'pre-commit.sh');
const trgFile = path.join(__dirname, '.git', 'hooks', 'pre-commit');

fsExtra.copySync(srcFile, trgFile);


