import path from 'path';

import loaders from './webpack.loaders';

export {default as plugins} from './webpack.plugins';
export {default as postcss} from './webpack.postcss';

const DEV = global.buildOptions.dev;

export const context = path.resolve(__dirname, '../');

export const entry = (() => {
    const items = {

        app: [
            'expose?$!jquery',
            'expose?jQuery!jquery',
            './client/babel-polyfill-custom.js',
            'jquery-mousewheel',
            'babel-polyfill',
            './client/app/app.js'
        ]
    };

    return items;
})();

export const output = {
    path: path.resolve(context, 'dist'),
    publicPath: global.buildOptions.publicPath || '/',
    desktopBuild: !!global.buildOptions.desktop,
    filename: '[name].bundle.[hash].js',
    chunkFilename: '[id].[name].js'
};
export const devtool = DEV ? 'source-map' : undefined;
export const module = {loaders};
export const cache = DEV;
