import { call, take } from 'redux-saga/effects';
import http from '@talend/react-cmf/lib/sagas/http';
import { api } from '@talend/react-cmf';

const defaultHttpConfiguration = {
	headers: {
		Accept: 'application/json, text/plain, */*',
		'Content-Type': 'application/json',
	},
};

export function* duplicatePreparation() {
	while (true) {
		const prep = yield take('PREPARATION_DUPLICATE');

		const newName = `test${Math.random()}`;

		yield call(http.post, `http://localhost:8888/api/preparations/${prep.payload.id}/copy?destination=Lw==&newName=${newName}`, {}, defaultHttpConfiguration);
		yield api.saga.putActionCreator('preparation:fetchAll');
	}
}
