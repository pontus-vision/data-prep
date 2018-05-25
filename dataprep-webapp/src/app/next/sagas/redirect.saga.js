import { takeEvery } from 'redux-saga/effects';
import { REDIRECT_WINDOW, OPEN_WINDOW } from '../constants/actions';

function* redirect() {
	yield takeEvery(REDIRECT_WINDOW, (action) => {
		debugger;
		window.location.assign(action.payload.url);
	});
}

function* open() {
	yield takeEvery(OPEN_WINDOW, (action) => {
		window.open(action.payload.url, '_blank');
	});
}

export default {
	redirect,
	open,
};
