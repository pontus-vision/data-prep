import { update } from 'immutable';
import { actions } from '@talend/react-cmf';
import { put, take, select } from 'redux-saga/effects';
import { RENAME_PREPARATION, SET_TITLE_EDITION_MODE } from '../constants';


export function* renamePreparationSaga() {
	while (true) {
		const test = yield take(RENAME_PREPARATION);
		console.log('[NC] RENAME !', test);
	}
}

export function* setTitleEditionModeSaga() {
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
