import { api, sagaRouter } from '@talend/react-cmf';
import { registerAllContainers } from '@talend/react-containers/lib/register';
import { all, call, fork } from 'redux-saga/effects';
import redirect from './actions/redirect';
import { fetchDataSets } from './actions/dataset';
import { fetchDataStores } from './actions/datastore';
import { fetchPreparations } from './actions/preparation';

import App from './components/App.container';

import { openAboutSaga } from './saga/about.saga';
import { OPEN_ABOUT } from './constants';

const registerComponent = api.route.registerComponent;
const registerActionCreator = api.action.registerActionCreator;

export default {
	initialize() {
		/**
		 * Register components in CMF Components dictionary
		 */
		registerAllContainers();
		registerComponent('App', App);

		/**
		 * Register action creators in CMF Actions dictionary
		 */
		registerActionCreator('help:about:open', () => ({ type: OPEN_ABOUT }));
		registerActionCreator('preparation:fetchAll', fetchPreparations);
		registerActionCreator('dataset:fetchAll', fetchDataSets);
		registerActionCreator('datastore:fetchAll', fetchDataStores);
		registerActionCreator('redirect', redirect);
	},

	runSagas(sagaMiddleware, history) {
		function* rootSaga() {
			yield all([
				fork(sagaRouter, history, {} /*routes*/),
				call(openAboutSaga),
			]);
		}
		sagaMiddleware.run(rootSaga);
	},
};
