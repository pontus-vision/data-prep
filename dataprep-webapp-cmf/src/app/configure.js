import { api, sagaRouter } from '@talend/react-cmf';
import { registerAllContainers } from '@talend/react-containers/lib/register';
import dataset from '@talend/dataset';
import rating from '@talend/rating';
import { all, call, fork } from 'redux-saga/effects';
import redirect from './actions/redirect';
import { helpSagas } from './saga';
import { fetchPreparations, setTitleEditionMode } from './actions/preparation';

import App from './components/App.container';

import { openAboutSaga } from './saga/help.saga';
import { duplicatePreparation, renamePreparationSaga, setTitleEditionModeSaga } from './saga/preparation.saga';
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
		registerActionCreator('preparation:rename', setTitleEditionMode);
		registerActionCreator('preparation:duplicate', (event, element) => ({
			type: 'PREPARATION_DUPLICATE',
			payload: {
				id: element.model.id,
			},
		}));
		registerActionCreator('preparation:fetchAll', fetchPreparations);
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
				fork(sagaRouter, history, {}),
				...helpSagas.map(saga => call(saga)),
				call(openAboutSaga),
				call(renamePreparationSaga),
				call(duplicatePreparation),
				call(setTitleEditionModeSaga),
			]);
		}
		sagaMiddleware.run(rootSaga);
	},
};
