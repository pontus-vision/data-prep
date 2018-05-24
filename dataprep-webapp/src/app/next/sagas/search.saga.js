import { all, takeLatest, call, delay } from 'redux-saga/effects';
import http from '@talend/react-cmf/lib/sagas/http';
import { SEARCH_FOR } from '../constants';

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

function* search({ payload }) {
	yield delay(500);
	const [doc, tdp] = yield all([
		yield call(...getDocumentationSearchParameters(payload)),
		yield call(...getDataprepSearchParameters(payload)),
	]);

	console.log('[NC] aaa: ', doc.data);
	console.log('[NC] bbb: ', tdp.data);
}

/*
let task
while (true) {
  const { input } = yield take('INPUT_CHANGED')
  if (task) {
	yield cancel(task)
  }
  task = yield fork(handleInput, input)
}
*/

function* searchFor() {
	while (true) {
		yield takeLatest(SEARCH_FOR, search);
		console.log('[NC] take !');
	}
}

export default {
	searchFor,
};
