import * as help from './help.saga';
import * as preparation from './preparation.saga';
import * as version from './version.saga';

export default {
	help: Object.keys(help).map(k => help[k]),
	preparation: Object.keys(preparation).map(k => preparation[k]),
	version: Object.keys(version).map(k => version[k]),
};
