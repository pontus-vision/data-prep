import * as help from './help.saga';
import * as preparation from './preparation.saga';
// import * as version from './version.saga';

export default {
	help: Object.keys(help.default).map(k => help.default[k]),
	preparation: Object.keys(preparation.default).map(k => preparation.default[k]),
	// version: Object.keys(version).map(k => version[k]),
};
