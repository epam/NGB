import webpack, { optimize } from 'webpack';
import HtmlWebpackPlugin from 'html-webpack-plugin';
import ExtractTextPlugin from 'extract-text-webpack-plugin';

const DEV = global.buildOptions.dev;
const globals = {
    __ENV__: `"${global.buildOptions.env}"`,
    __DEV__: String(DEV),
    __DESKTOP__: !!global.buildOptions.desktop,
    __API_URL__: JSON.stringify(global.buildOptions.publicPath || '/')
};

const devPlugins = [
    new webpack.NoErrorsPlugin()
];
// export const extractTextPlugin = new ExtractTextPlugin('[name].css');
export const extractTextPlugin = new ExtractTextPlugin({
  filename: '[name].css'
  // disable: false,
  // allChunks: true
});

const prodPlugins = [
    extractTextPlugin,
    new optimize.UglifyJsPlugin({
        mangle: false
    }),
    // new optimize.DedupePlugin(),
    new optimize.AggressiveMergingPlugin()
];


export default [
    new webpack.DefinePlugin(globals),
    new HtmlWebpackPlugin({
        favicon: './client/app/assets/icons/ngb-logo.png',
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
