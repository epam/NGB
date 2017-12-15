require("babel-core/register");
require('babel-polyfill');
global.buildOptions = require('minimist')(process.argv.slice(2));
global.buildOptions.dev = !global.buildOptions.prodBuild;

module.exports = require('./webpack.config.babel/');

// module.exports = function(env) {
//   require("babel-core/register");
//   require('babel-polyfill');
//   global.buildOptions = require('minimist')(process.argv.slice(2));
//   global.buildOptions.dev = !global.buildOptions.prodBuild;
//   return config;
// };