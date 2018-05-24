import { delay } from 'redux-saga';
import { call, cancel, fork, take, all } from 'redux-saga/effects';
import http from '@talend/react-cmf/lib/sagas/http';
import { SEARCH, DEBOUNCE_TIMEOUT } from '../constants';

function getDocumentationSearchParameters(term) {
	return [
		http.post,
		'https://www.talendforge.org/find/api/THC.php',
		{
			contentLocale: 'en',
			filters: [
				{ key: 'version', values: ['2.1'] },
				{ key: 'EnrichPlatform', values: ['Talend Data Preparation'] },
			],
			paging: { page: 1, perPage: 5 },
			query: term,
		},
	];
}

function getDataprepSearchParameters(term) {
	return [http.get, `http://localhost:3000/api/search?path=/&name=${term}`];
}

function* process(payload) {
	yield delay(DEBOUNCE_TIMEOUT);
	const [doc, tdp] = yield all([
		call(...getDocumentationSearchParameters(payload)),
		call(...getDataprepSearchParameters(payload)),
	]);

	console.log('[NC] aaa: ', doc.data);
	console.log('[NC] bbb: ', JSON.parse(tdp.data));
}

function* search() {
	let task;
	while (true) {
		const { payload } = yield take(SEARCH);
		if (task) {
			yield cancel(task);
		}
		task = yield fork(process, payload);
	}
}

export default {
	search,
};
