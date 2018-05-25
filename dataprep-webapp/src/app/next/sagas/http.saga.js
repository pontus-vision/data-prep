import { put, select, takeLatest } from 'redux-saga/effects';
import { HTTP_STATUS } from '@talend/react-cmf/lib/middlewares/http/constants';
import { REDIRECT_WINDOW } from '../constants';

function* handleError(event) {
	if (!event.error || !event.error.stack) {
		return;
	}

	switch (event.error.stack.status) {
		case HTTP_STATUS.UNAUTHORIZED: {
			console.log('unauthorized');
			const settings = yield select(state => state.cmf.collections.get('settings'));
			console.log('settings', settings.toJS());
			yield put({
				type: REDIRECT_WINDOW,
				payload: {
					url: settings.get('uris').get('login'),
				},
			});
			break;
		}
		case HTTP_STATUS.FORBIDDEN:
		case HTTP_STATUS.NOT_FOUND: {
			console.log('404/403');
			console.log({ event });
		}
		// yield put({
		// 	type: '@@router/CALL_HISTORY_METHOD',
		// 	payload: {
		// 		method: 'replace',
		// 		args: [`/${event.error.stack.status}`],
		// 	},
		// });
	}
}

export default function* httpHandler() {
	console.log('httpHandler');
	yield takeLatest('@@HTTP/ERRORS', handleError);
}