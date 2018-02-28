import { put, take } from 'redux-saga/effects';
import http from '@talend/react-cmf/lib/sagas/http';
import { FETCH_VERSION } from '../constants';

export function* fetch() {
	while (true) {
		yield take(FETCH_VERSION);
		yield put(
			http.get('http://localhost:8888/api/version',
				{
					cmf: {
						collectionId: 'versions',
					},
				}
			),
		);
	}
}
