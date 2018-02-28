const webpack = require('webpack');
const ReactCMFWebpackPlugin = require('@talend/react-cmf-webpack-plugin');

const config = require('./webpack.config');

config.devtool = 'inline-source-map';
config.plugins.push(
	new webpack.DefinePlugin({
		'process.env.NODE_ENV': JSON.stringify('development'),
	}),
	new ReactCMFWebpackPlugin({
		watch: true,
	}),
);

config.watchOptions = {
	aggregateTimeout: 300,
	poll: 1000,
};

config.devServer = {
	port: 3000,
	setup(app) {
		app.get('/assets/config/config.json', function (req, res) {
			const configFile = require('./../src/assets/config/config.json');
			configFile.serverUrl = 'http://localhost:8888';
			res.json(configFile);
		});
	},
	proxy: {
		'/api/v1/stream-websocket': {
			target: process.env.API_URL || 'http://localhost',
			ws: true,
		},
		'/api': {
			target: process.env.API_URL || 'http://localhost',
			changeOrigin: true,
			secure: false,
		},
	},
	stats: {
		children: false,
	},
	historyApiFallback: true,
};

module.exports = config;
