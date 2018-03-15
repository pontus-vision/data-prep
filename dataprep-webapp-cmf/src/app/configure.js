import { api, sagaRouter } from '@talend/react-cmf';
import { registerAllContainers } from '@talend/react-containers/lib/register';
import { all, call, fork } from 'redux-saga/effects';

import actions from './actions';
import sagas from './saga';

import App from './components/App.container';
import FirstDrawer from './router-v4/FirstDrawer';
import SecondDrawer from './router-v4/SecondDrawer';


const registerActionCreator = api.actionCreator.register;
const registerComponent = api.component.register;
const registerRouteFunction = api.route.registerFunction;

export default {
	initialize() {

		/**
		 * Register components in CMF Components dictionary
		 */
		registerAllContainers();
		registerComponent('App', App);
		registerComponent('FirstDrawer', FirstDrawer);
		registerComponent('SecondDrawer', SecondDrawer);

		/**
		 * Register route functions
		 */
		registerRouteFunction('preparation:fetch', actions.preparation.fetchPreparationsOnEnter);

		/**
		 * Register action creators in CMF Actions dictionary
		 */
		registerActionCreator('preparation:add:open', actions.preparation.addOpen);
		registerActionCreator('preparation:duplicate', actions.preparation.duplicate);
		registerActionCreator('preparation:edit:submit', actions.preparation.rename);
		registerActionCreator('preparation:edit:cancel', actions.preparation.cancelRename);
		registerActionCreator('preparation:open', actions.preparation.openPreparation);
		registerActionCreator('preparation:rename', actions.preparation.setTitleEditionMode);

		registerActionCreator('help:about:open', actions.help.openAbout);
		registerActionCreator('help:tour', () => ({ type: 'ALERT', payload: 'help:tour' }));
		registerActionCreator('help:feedback:open', () => ({ type: 'ALERT', payload: 'help:feedback:open' }));

		registerActionCreator('redirect', actions.redirect);
	},

	runSagas(sagaMiddleware, history) {
		function* rootSaga() {
			yield all([
				fork(sagaRouter, history, {}),
				...sagas.help.map(call),
				...sagas.preparation.map(call),
			]);
		}
		sagaMiddleware.run(rootSaga);
	},
};
