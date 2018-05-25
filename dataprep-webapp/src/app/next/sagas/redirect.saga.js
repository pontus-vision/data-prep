import { takeEvery } from 'redux-saga/effects'

export default function* redirectHandler() {
	yield takeEvery('REDIRECT', (action) => {
		window.location.assign(action.payload.url);
	});
}