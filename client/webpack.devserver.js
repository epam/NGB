const port = 8080;
const webpack = require('webpack');
const config = require('./webpack.config');
for (const key of Object.keys(config.entry))
    config.entry[key].unshift(`webpack-dev-server/client?http://localhost:${port}/`, 'webpack/hot/dev-server');

config.plugins.push(new webpack.HotModuleReplacementPlugin());
const compiler = webpack(config);
const DevServer = require('webpack-dev-server');

new DevServer(compiler, {
    contentBase: './dist',

    hot: true,
    inline: true,
    quiet: false,
    noInfo: false,
    lazy: false,

    stats: {
        assets: false,
        colors: true,
        version: true,
        hash: false,
        timings: true,
        chunks: false,
        chunkModules: false
    },

    proxy: {
        '/api/epam': {
            pathRewrite: {'^/api/epam' : ''},
            target: process.env.NGB_API_EPAM || '/',
        },
        '/api/opensource': {
            pathRewrite: {'^/api/opensource' : ''},
            target: process.env.NGB_API_OPENSOURCE || '/',
        }
    }
})
    .listen(port, 'localhost', () => console.log('webpack dev server started on http://localhost:8080'));

