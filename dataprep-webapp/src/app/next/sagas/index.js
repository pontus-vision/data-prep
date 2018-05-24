import { default as help } from './help.saga';
import { default as preparation } from './preparation.saga';
import { default as search } from './search.saga';
import { default as appLoader } from './appLoader.saga';

export default {
	help: Object.keys(help).map(k => help[k]),
	preparation: Object.keys(preparation).map(k => preparation[k]),
	search: Object.keys(search).map(k => search[k]),
	appLoader,
};
