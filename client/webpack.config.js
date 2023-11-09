const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
require('dotenv').config();

module.exports = function (env, argv) {
    const development = argv.mode === 'development';
    const production = !development;
    const desktop = !!env.desktop;
    let publicUrl = process.env.PUBLIC_URL || '/';
    if (publicUrl && !publicUrl.endsWith('/')) {
        publicUrl = publicUrl.concat('/');
    }
    let apiUrl = process.env.API || publicUrl;
    if (apiUrl && !apiUrl.endsWith('/')) {
        apiUrl = apiUrl.concat('/');
    }
    console.log('Building NGB Client');
    console.log('  mode:      ', production ? 'production' : 'development', desktop ? '(desktop)' : '');
    console.log('  public url:', `"${publicUrl}"`, publicUrl === process.env.PUBLIC_URL ? '' : `(environment variable PUBLIC_URL="${process.env.PUBLIC_URL || ''}" was corrected)`);
    console.log('  api url:   ', `"${apiUrl}" (${apiUrl}restapi/)`);
    console.log('');
    const styleLoader = development ? 'style-loader' : MiniCssExtractPlugin.loader;
    return {
        entry: {
            app: path.resolve(__dirname, './index.js')
        },
        output: {
            path: path.resolve(__dirname, './dist'),
            filename: '[name].bundle.[contenthash].js',
            chunkFilename: '[id].[contenthash].js',
            clean: true,
            publicPath: publicUrl
        },
        mode: development ? 'production' : 'development',
        devtool: development ? 'source-map' : false,
        module: {
            rules: [
                {
                    test: /\.(js)$/,
                    exclude: /node_modules/,
                    use: ['babel-loader']
                },
                {
                    test: /\.s[ac]ss$/i,
                    use: [
                        styleLoader,
                        'css-loader',
                        'sass-loader',
                    ],
                },
                {
                    test: /\.css$/i,
                    use: [
                        styleLoader,
                        'css-loader',
                    ],
                },
                {
                    test: /\.html?$/i,
                    loader: 'html-loader',
                    options: {
                        esModule: false,
                        minimize: false,
                        sources: {
                            list: [
                                // All default supported tags and attributes
                                "...",
                                {
                                    tag: "md-icon",
                                    attribute: "md-svg-src",
                                    type: "src",
                                },
                            ],
                        }
                    }
                },
                {
                    test: /\.(svg|png)$/i,
                    type: 'asset/resource',
                    generator: {
                        filename: 'static/images/[hash][ext][query]'
                    }
                },
                {
                    test: /\.(woff|woff2|ttf|eot)$/i,
                    type: 'asset/resource',
                    generator: {
                        filename: 'static/fonts/[hash][ext][query]'
                    }
                },
            ],
        },
        resolve: {
            extensions: ['.js'],
            fallback: {
                buffer: require.resolve('buffer/')
            }
        },
        optimization: {
            minimize: false,
            runtimeChunk: {
                name: 'runtime',
            },
            splitChunks: {
                cacheGroups: {
                    vendor: {
                        test: /[\\/]node_modules[\\/]/,
                        name: 'vendors',
                        chunks: 'all',
                    },
                },
            },
        },
        plugins: [
            new MiniCssExtractPlugin({
                filename: '[name].[contenthash].css',
                chunkFilename: '[id].[contenthash].css',
            }),
            new HtmlWebpackPlugin({
                favicon: path.resolve(__dirname, './client/app/assets/icons/ngb-logo.png'),
                template: path.resolve(__dirname, './client/index.html'),
                title: 'NGB',
                base: `${publicUrl}`,
            }),
            new webpack.ProvidePlugin({
                $: 'jquery',
                jQuery: 'jquery',
                'window.jQuery': 'jquery',
                Buffer: ['buffer', 'Buffer']
            }),
            new webpack.DefinePlugin({
                'process.env.__ENV__': JSON.stringify(production ? 'production' : 'development'),
                'process.env.__API_URL__': JSON.stringify(apiUrl),
                'process.env.__DEV__': JSON.stringify(development),
                'process.env.__DESKTOP__': JSON.stringify(desktop)
            }),
        ]
    };
}
