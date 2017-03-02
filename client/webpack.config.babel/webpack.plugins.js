import webpack, { optimize } from 'webpack';
import HtmlWebpackPlugin from 'html-webpack-plugin';
import ExtractTextPlugin from 'extract-text-webpack-plugin';
import FaviconsWebpackPlugin from 'favicons-webpack-plugin';

const DEV = global.buildOptions.dev;
const globals = {
    __ENV__: `"${global.buildOptions.env}"`,
    __DEV__: String(DEV),
    __DESKTOP__: !!global.buildOptions.desktop
};

const devPlugins = [
    new webpack.NoErrorsPlugin()
];
export const extractTextPlugin = new ExtractTextPlugin('[name].css');

const prodPlugins = [
    extractTextPlugin,
    new optimize.UglifyJsPlugin({
        mangle: false
    }),
    new optimize.DedupePlugin(),
    new optimize.AggressiveMergingPlugin()
];


export default [
    new webpack.DefinePlugin(globals),
    new FaviconsWebpackPlugin({
        logo: './client/app/assets/icons/ngb-logo.png',
        prefix: 'icons-[hash]/',
        emitStats: false,
        persistentCache: true,
        inject: true,
        icons: {
            android: false,
            appleIcon: false,
            appleStartup: false,
            coast: false,
            favicons: true,
            firefox: false,
            opengraph: false,
            twitter: false,
            yandex: false,
            windows: false
        }
    }),
    new HtmlWebpackPlugin({
        template: './client/index.html.ejs',
        inject: 'body'
    }),
    function () { // sets build to failure if a compilation error occurred
        this.plugin("done", function (stats) {
            if (stats.compilation.errors && stats.compilation.errors.length) {
                console.log(stats.compilation.errors);
                process.exit(1);
            }
        });
    },
    ...(DEV ? devPlugins : prodPlugins)
];