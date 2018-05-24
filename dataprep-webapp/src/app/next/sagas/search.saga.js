import { actions } from '@talend/react-cmf';
import { take, put, select } from 'redux-saga/effects';

function* toggle() {
	const path = ['Typeahead', 'headerbar:search'];

	while (true) {
		yield take('TOGGLE_SEARCH');
		const state = yield select(state => state.cmf.components.getIn(path));
		yield put(actions.components.mergeState(...path, { docked: !state.get('docked') }));
	}
}

export default {
	toggle,
};
