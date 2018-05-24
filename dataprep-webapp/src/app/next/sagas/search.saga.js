import { delay } from 'redux-saga';
import { call, cancel, fork, take, all, put } from 'redux-saga/effects';
import http from '@talend/react-cmf/lib/sagas/http';
import { actions } from '@talend/react-cmf';
import {
	SEARCH,
	DEFAULT_SEARCH_PAYLOAD,
	DEBOUNCE_TIMEOUT,
	DOCUMENTATION_SEARCH_URL,
	TDP_SEARCH_URL,
} from '../constants';

function* documentation(query) {
	const { data } = yield call(http.post, DOCUMENTATION_SEARCH_URL, {
		...DEFAULT_SEARCH_PAYLOAD,
		query,
	});

	return { documentation: data.results };
}

function* dataprep(query) {
	const { data } = yield call(http.get, `${TDP_SEARCH_URL}${query}`);
	const { datasets, folders, preparations } = JSON.parse(data);
	return { datasets, folders, preparations };
}

function* process(payload) {
	yield delay(DEBOUNCE_TIMEOUT);
	const [doc, tdp] = yield all([
		call(documentation, payload),
		call(dataprep, payload),
	]);
	yield put(actions.collections.addOrReplace('search', { ...doc, ...tdp }));
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
