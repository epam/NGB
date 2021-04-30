const path = require('path');

const loaders = require('./webpack.loaders');

const plugins = require('./webpack.plugins');
const postcss = require('./webpack.postcss');

const DEV = global.buildOptions.dev;

const context = path.resolve(__dirname, '../');

const entry = (() => {
    const items = {

        app: [
            'expose?$!jquery',
            'expose?jQuery!jquery',
            // './client/babel-polyfill-custom.js',
            'jquery-mousewheel',
            './client/app/app.ts'
        ]
    };

    return items;
})();

const output = {
    path: path.resolve(context, 'dist'),
    publicPath: global.buildOptions.publicPath || '/',
    desktopBuild: !!global.buildOptions.desktop,
    filename: '[name].bundle.js',
    chunkFilename: '[id].[name].js'
};
const resolve = {
    extensions: ["",".wasm", ".ts", ".tsx", ".mjs", ".cjs", ".js", ".json"],
    // modules: ["client", "node_modules"],
    mainFiles: ['index'],
    alias: {
        'pixi.js': 'pixi.js/bin/pixi.js',
    }
};
const devtool = DEV ? 'source-map' : undefined;
const cache = DEV;

module.exports = {
    output,
    resolve,
    cache,
    module: {loaders},
    devtool,
    context,
    plugins,
    postcss,
    entry
}