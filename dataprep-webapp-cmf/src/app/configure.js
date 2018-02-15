import { api, sagaRouter } from '@talend/react-cmf';
import { registerAllContainers } from '@talend/react-containers/lib/register';
import dataset from '@talend/dataset';
import rating from '@talend/rating';
import { all, call, fork } from 'redux-saga/effects';
import redirect from './actions/redirect';
import { fetchPreparations } from './actions/preparation';

import App from './components/App.container';

import { helpSagas } from './saga';
import { OPEN_ABOUT } from './constants';

const registerComponent = api.route.registerComponent;
const registerActionCreator = api.action.registerActionCreator;

export default {
	initialize() {
		dataset.configure();
		rating.configure();

		/**
		 * Register components in CMF Components dictionary
		 */
		registerAllContainers();
		registerComponent('App', App);

		/**
		 * Register action creators in CMF Actions dictionary
		 */
		registerActionCreator('preparation:fetchAll', fetchPreparations);
		registerActionCreator('redirect', redirect);

		registerActionCreator('help:tour', () => { alert('TODO'); return { type: 'none' }; });
		registerActionCreator('help:about:open', () => ({ type: OPEN_ABOUT }));
		registerActionCreator('help:feedback:open', () => { alert('TODO'); return { type: 'none' }; });
	},

	runSagas(sagaMiddleware, history) {
		function* rootSaga() {
			yield all([
				fork(sagaRouter, history, {} /*TODO sagas per route*/),
				...helpSagas.map(saga => call(saga)),
			]);
		}
		sagaMiddleware.run(rootSaga);
	},
};
