const {extractTextPlugin} = require('./webpack.plugins');
const DEV = global.buildOptions.dev;

const wrapStyleLoader = loader => DEV
    ? `style?sourceMap!${loader}`
    : extractTextPlugin.extract('style?sourceMap', loader);

    // 'ts-loader?transpileOnly', 
    // 'babel-loader?cacheDirectory'
const appJsLoader = {
    test: /\.m?(j|t)sx?$/,
    loaders: ['babel-loader?cacheDirectory=true'],
    include: /client/,
    exclude: [/3rd-party/, /node_modules/]
};
//actually only used for htmlWebpackPlugin
const ejsLoader = {
    test: /\.ejs$/,
    loaders: ['ejs']
};

const rawJsLoader = {
    test: /\.js$/,
    loaders: [],
    noParse: [/3rd-party/, /node_modules/],
    include: [/3rd-party/, /node_modules/],
    exclude: /client/,
};

const thirdPartyStyleLoader = {
    test: /\.css$/,
    include: [/node_modules/],
    loader: DEV
        ? wrapStyleLoader('css?-loader&-url&sourceMap')
        : wrapStyleLoader('css?-loader&-url')

};
const thirdPartyStyleLoaderMDI = {
    test: /\.css$/,
    include: [/3rd-party/],
    loader: wrapStyleLoader('css?-loader&-url')
};

const commonStyleLoader = {
    test: /\.css$/,
    include: /client/,
    exclude: [/3rd-party/, /node_modules/],
    loader: DEV
        ? wrapStyleLoader('css?modules&localIdentName=[hash:base64:5]&sourceMap!postcss')
        : wrapStyleLoader('css?modules&localIdentName=[hash:base64:5]!postcss')
};

const imagesLoader = {
    // ASSET LOADER
    // Reference: https://github.com/webpack/file-loader
    // Copy png, jpg, jpeg, gif, svg, woff, woff2, ttf, eot files to output
    // Rename the file using the asset hash
    // Pass along the updated reference to your code
    // You can add here any file extension you want to get copied to your output
    test: /\.(png|jpg|jpeg|gif|svg)$/,
    loader: 'url-loader?name=[name].[ext]'
};

const sassLoader = {
    // Sass Loader
    test: /\.scss$/,
    loader: DEV
        ? wrapStyleLoader('css?sourceMap!postcss!sass?sourceMap')
        : wrapStyleLoader('css!postcss!sass')
};

const HTMLLoader = {
    test: /\.html$/,
    exclude: /node_modules/,
    loader: 'html?attrs[]=md-icon:md-svg-src',
};

module.exports = [
    appJsLoader,
    rawJsLoader,
    ejsLoader,
    thirdPartyStyleLoader,
    thirdPartyStyleLoaderMDI,
    commonStyleLoader,
    HTMLLoader,
    imagesLoader,
    sassLoader,
    {test: /\.woff$/, loader: 'url?limit=65000&mimetype=application/font-woff&name=[name].[ext]'},
    {test: /\.woff2$/, loader: 'url?limit=65000&mimetype=application/font-woff2&name=[name].[ext]'},
    {test: /\.[ot]tf$/, loader: 'url?limit=65000&mimetype=application/octet-stream&name=[name].[ext]'},
    {test: /\.eot$/, loader: 'url?limit=65000&mimetype=application/vnd.ms-fontobject&name=[name].[ext]'}


];
