import { default as help } from './help.saga';
import { default as preparation } from './preparation.saga';

export default {
	help: Object.keys(help).map(k => help[k]),
	preparation: Object.keys(preparation).map(k => preparation[k]),
};
