const path = require('path');
const webpack = require('webpack');

const config = require('./webpack.config');

config.plugins = config.plugins.concat([
	new webpack.DefinePlugin({
		'process.env.NODE_ENV': JSON.stringify('production'),
	}),
	new webpack.optimize.UglifyJsPlugin({
		compress: {
			warnings: false,
		},
		comments: false,
		mangle: true,
		minimize: true,
	}),
]);

config.module.loaders.push({
	test: /\.js$/,
	enforce: 'pre',
	use: 'stripcomment-loader',
	exclude: [
		/node_modules/,
		/\.spec\.js$/,
	],
});

module.exports = config;
