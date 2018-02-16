import { call, take, put, select } from 'redux-saga/effects';
import http from '@talend/react-cmf/lib/sagas/http';
import { api, actions } from '@talend/react-cmf';
import {
	CANCEL_RENAME_PREPARATION,
	RENAME_PREPARATION,
	SET_TITLE_EDITION_MODE,
	PREPARATION_DUPLICATE,
} from '../constants';

const defaultHttpConfiguration = {
	headers: {
		Accept: 'application/json, text/plain, */*',
		'Content-Type': 'application/json',
	},
};

export function* duplicate() {
	while (true) {
		const prep = yield take(PREPARATION_DUPLICATE);
		const newName = `test${Math.random()}`;

		yield call(
			http.post,
			`http://localhost:8888/api/preparations/${prep.payload.id}/copy?destination=Lw==&newName=${newName}`,
			{},
			defaultHttpConfiguration,
		);
		yield api.saga.putActionCreator('preparation:fetchAll');
	}
}

export function* rename() {
	while (true) {
		const { payload } = yield take(RENAME_PREPARATION);

		yield call(
			http.put,
			`http://localhost:8888/api/preparations/${payload.id}`,
			{ name: payload.name },
			defaultHttpConfiguration,
		);
		yield api.saga.putActionCreator('preparation:fetchAll');
	}
}

export function* cancelRename() {
	while (true) {
		const { payload } = yield take(CANCEL_RENAME_PREPARATION);
		const preparations = yield select(state => state.cmf.collections.get('preparations'));
		const updated = preparations.update(
			preparations.findIndex(val => val.get('id') === payload),
			val => val.set('display', 'text')
		);
		yield put(actions.collections.addOrReplace('preparations', updated));
	}
}

export function* setTitleEditionMode() {
	while (true) {
		const { payload } = yield take(SET_TITLE_EDITION_MODE);
		const preparations = yield select(state => state.cmf.collections.get('preparations'));
		const updated = preparations.update(
			preparations.findIndex(val => val.get('id') === payload),
			val => val.set('display', 'input')
		);
		yield put(actions.collections.addOrReplace('preparations', updated));
	}
}
