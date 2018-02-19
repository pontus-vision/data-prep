import { api, sagaRouter } from '@talend/react-cmf';
import { registerAllContainers } from '@talend/react-containers/lib/register';
import dataset from '@talend/dataset';
import rating from '@talend/rating';
import { all, call, fork } from 'redux-saga/effects';

import actions from './actions';
import sagas from './saga';

import App from './components/App.container';


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
		registerActionCreator('preparation:rename', actions.preparation.setTitleEditionMode);
		registerActionCreator('preparation:duplicate', actions.preparation.duplicate);
		registerActionCreator('preparation:fetchAll', actions.preparation.fetchAll);

		registerActionCreator('preparation:edit:submit', actions.preparation.rename);
		registerActionCreator('preparation:edit:cancel', actions.preparation.cancelRename);

		registerActionCreator('help:about:open', actions.help.openAbout);

		registerActionCreator('help:tour', () => ({ type: 'ALERT', payload: 'help:tour' }));
		registerActionCreator('help:feedback:open', () => ({ type: 'ALERT', payload: 'help:feedback:open' }));
		registerActionCreator('preparation:open', () => ({ type: 'ALERT', payload: 'preparation:open' }));
		registerActionCreator('folder:open', () => ({ type: 'ALERT', payload: 'folder:open' }));

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
