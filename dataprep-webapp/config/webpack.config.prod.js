const configure = require('./webpack.config');

module.exports = function() {
	return configure({
                publicPath: "pontus-extract-discovery-gui/",
		env: 'prod',
		entryOutput: true,
		minify: true,
		stripComments: true,
                output: { publicPath: "/pontus-extract-discovery-gui/" }
	});
};
