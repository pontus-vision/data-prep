import { takeEvery } from 'redux-saga/effects';

import { OPEN_WINDOW, REDIRECT_WINDOW } from '../constants/actions';

/**
 * Open new tab
 * @returns {IterableIterator<*|ForkEffect>}
 */
function* open() {
	yield takeEvery(OPEN_WINDOW, (action) => {
		window.open(action.payload.url, '_blank');
	});
}

/**
 * Redirect window to
 * @returns {IterableIterator<*|ForkEffect>}
 */
function* redirect() {
	yield takeEvery(REDIRECT_WINDOW, (action) => {
		window.location.assign(action.payload.url);
	});
}

export default {
	open,
	redirect,
};
