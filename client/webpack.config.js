global.buildOptions = require('minimist')(process.argv.slice(2));
global.buildOptions.dev = !global.buildOptions.prodBuild;

module.exports = require('./webpack.config.babel/'); 