import {
	api,
	store as cmfstore,
	sagaRouter,
	actions as cmfActions,
} from '@talend/react-cmf';
import reduxLocalStorage from '@talend/react-cmf/lib/reduxstorage/reduxLocalStorage';
import { registerAllContainers } from '@talend/react-containers/lib/register';
import dataset from '@talend/dataset';
import { browserHistory } from 'react-router';
import { routerMiddleware } from 'react-router-redux';
import createSagaMiddleware from 'redux-saga';
import { all, call, fork } from 'redux-saga/effects';
import { FETCH_PREPARATIONS } from './constants';
import actions from './actions';
import components from './components';
import sagas from './saga';
import appReducer from './reducers';

import App from './components/App.container';

const registerActionCreator = api.actionCreator.register;
const registerComponent = api.component.register;
const registerComponents = api.component.registerMany;
const registerExpressions = api.expression.registerMany;
const registerRouteFunction = api.route.registerFunction;

const dataprepLocalStorageKey = 'data-prep-app';

function* rootSaga() {
	yield all([
		fork(sagaRouter, browserHistory, {}),
		...sagas.help.map(call),
		...sagas.preparation.map(call),
		...sagas.version.map(call),
	]);
}

/**
 * Initialize CMF configuration
 * - Register your components in the CMF dictionary
 * - Register action creators in CMF actions dictionary
 */
export default function initialize() {
	function appFactory(storage = {}) {
		const { initialState, engine } = storage;

		// const socketMiddleware = createWebsocketMiddleware();

		cmfstore.addPreReducer([
			dataset.preReducers.notificationReducer,
			...dataset.hors,
		]);

		/**
		 * Register react-router-redux router reducer (see https://github.com/reactjs/react-router-redux)
		 */
		cmfstore.setRouterMiddleware(routerMiddleware(browserHistory));

		/**
		 * Register your app reducers
		 */
		const sagaMiddleware = createSagaMiddleware();

		const store = cmfstore.initialize(appReducer, initialState, undefined, [
			sagaMiddleware,
		]);

		sagaMiddleware.run(rootSaga);

		api.registerInternals();
		/**
		 * Register route functions
		 */
		registerRouteFunction('preparation:fetch', ({ router, dispatch }) => dispatch({
			type: FETCH_PREPARATIONS,
			folderId: router.nextState.params.folderId,
		}));

		registerAllContainers();
		dataset.configure();
		/**
		 * Register expressions in CMF expressions dictionary
		 */
		registerExpressions(api.expressions);
		/**
		 * Register components in CMF Components dictionary
		 */
		registerComponent('App', App);
		registerComponents(components);
		/**
		 * Register action creators in CMF Actions dictionary
		 */
		registerActionCreator('preparation:fetchAll', actions.preparation.fetchAll);
		registerActionCreator('preparation:duplicate', actions.preparation.duplicate);
		registerActionCreator('preparation:edit:submit', actions.preparation.rename);
		registerActionCreator('preparation:edit:cancel', actions.preparation.cancelRename);
		registerActionCreator('preparation:open', actions.preparation.openPreparation);
		registerActionCreator('preparation:rename', actions.preparation.setTitleEditionMode);
		registerActionCreator('preparation:add:open', actions.preparation.openCreator);
		registerActionCreator('help:about:open', actions.help.openAbout);
		registerActionCreator('help:tour', () => ({ type: 'ALERT', payload: 'help:tour' }));
		registerActionCreator('help:feedback:open', () => ({ type: 'ALERT', payload: 'help:feedback:open' }));
		registerActionCreator('redirect', actions.redirect);
		registerActionCreator('version:fetch', actions.version.fetch);

		/**
		 * Fetch the CMF settings and configure the CMF app
		 */
		store.dispatch(cmfActions.settingsActions.fetchSettings('/settings.json'));
		reduxLocalStorage.saveOnReload({ engine, store });
		return {
			store,
			browserHistory,
		};
	}

	return reduxLocalStorage
		.loadInitialState({
			key: dataprepLocalStorageKey,
			whitelist: [
				['cmf', 'components', 'Container(SidePanel)'],
				['cmf', 'components', 'Container(List)', 'preparations'],
				['cmf', 'components', 'Container(List)', 'datasets'],
				['cmf', 'components', 'Container(List)', 'datastores'],
			],
		})
		.then(
			storage => appFactory(storage),
			(error) => {
				console.error(error); // eslint-disable-line no-console
				return appFactory();
			},
		);
}
