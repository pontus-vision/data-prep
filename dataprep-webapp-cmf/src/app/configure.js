import { api, sagaRouter } from '@talend/react-cmf';
import { registerAllContainers } from '@talend/react-containers/lib/register';
import dataset from '@talend/dataset';
import rating from '@talend/rating';
import { all, call, fork } from 'redux-saga/effects';
import redirect from './actions/redirect';
import { fetchPreparationsOnEnter, openPreparation } from './actions/preparation';

import App from './components/App.container';

import { helpSagas, preparationSagas } from './saga';
import { OPEN_ABOUT } from './constants';

const registerActionCreator = api.action.registerActionCreator;
const registerComponent = api.route.registerComponent;
const registerRouteFunction = api.route.registerFunction;

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
		 * Register route functions
		 */
		registerRouteFunction('preparation:fetch', fetchPreparationsOnEnter);

		/**
		 * Register action creators in CMF Actions dictionary
		 */
		registerActionCreator('preparation:open', openPreparation);
		registerActionCreator('redirect', redirect);

		registerActionCreator('help:tour', () => { alert('TODO'); return { type: 'none' }; });
		registerActionCreator('help:about:open', () => ({ type: OPEN_ABOUT }));
		registerActionCreator('help:feedback:open', () => { alert('TODO'); return { type: 'none' }; });

		registerActionCreator('preparation:add:open', () => { alert('TODO'); return { type: 'none' }; });
		registerActionCreator('folder:add:open', () => { alert('TODO'); return { type: 'none' }; });
	},

	runSagas(sagaMiddleware, history) {
		function* rootSaga() {
			yield all([
				fork(sagaRouter, history, {} /*TODO sagas per route*/),
				...helpSagas.map(saga => call(saga)),
				...preparationSagas.map(saga => call(saga)),
			]);
		}
		sagaMiddleware.run(rootSaga);
	},
};
