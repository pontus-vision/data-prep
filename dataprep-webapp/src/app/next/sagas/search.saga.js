import { delay } from 'redux-saga';
import { call, cancel, fork, take, all, put, select } from 'redux-saga/effects';
import http from '@talend/react-cmf/lib/sagas/http';
import { actions } from '@talend/react-cmf';
import { SEARCH, SEARCH_SELECT } from '../constants/actions';
import { preparation, folder } from '../actions';
import {
	DEFAULT_SEARCH_PAYLOAD,
	DEBOUNCE_TIMEOUT,
	DOCUMENTATION_SEARCH_URL,
	TDP_SEARCH_URL,
} from '../constants/search';

function* goTo() {
	while (true) {
		const { payload } = yield take(SEARCH_SELECT);
		const results = yield select(state => state.cmf.collections.get('search'));
		const item = results
			.get(payload.sectionIndex)
			.get('suggestions')
			.get(payload.itemIndex)
			.toJS();

		switch (item.inventoryType) {
		case 'preparation':
			yield put(preparation.open(null, { type: 'preparation', id: item.id }));
			break;
		case 'dataset':
			// yield put();
			break;
		case 'folder':
			yield put(folder.open(null, { id: item.id }));
			break;
		case 'documentation':
			yield put();
		}

		// console.log('[NC] item: ', item);
	}
}

function* documentation(query) {
	const { data } = yield call(http.post, DOCUMENTATION_SEARCH_URL, {
		...DEFAULT_SEARCH_PAYLOAD,
		query,
	});

	return data;
}

function* dataprep(query) {
	const { data } = yield call(http.get, `${TDP_SEARCH_URL}${query}`);
	return JSON.parse(data);
}

function* process(payload) {
	yield delay(DEBOUNCE_TIMEOUT);
	const [tdp, doc] = yield all([call(dataprep, payload), call(documentation, payload)]);
	const categories = tdp.categories;

	const results = [...adaptTDPResults(tdp), ...adaptSearchResult(doc)];
	const items = categories
		.filter(({ type }) => results.some(result => result.inventoryType === type))
		.map((inventory) => {
			const suggestions = results.filter(result => result.inventoryType === inventory.type);
			let label = inventory.type;
			if (categories) {
				label = categories.find(category => category.type === inventory.type).label;
			}

			return {
				title: label,
				icon: {
					name: 'braaaaaa', // inventory.iconName,
					title: label,
				},
				suggestions,
			};
		});

	yield put(actions.collections.addOrReplace('search', items));
}

function* search() {
	let task;
	while (true) {
		const { payload } = yield take(SEARCH);
		if (task) {
			yield cancel(task);
		}
		task = yield fork(process, payload);
	}
}

function adaptTDPResults(data) {
	let items = [];
	const mapping = [
		{
			key: 'datasets',
			inventory: 'dataset',
		},
		{
			key: 'preparations',
			inventory: 'preparation',
		},
		{
			key: 'folders',
			inventory: 'folder',
		},
	];

	mapping.forEach((type) => {
		data[type.key].forEach((item) => {
			item.inventoryType = type.inventory;
			item.tooltipName = item.name;
			// FIXME [NC]:
			item.title = item.name;
			// itemToDisplay.model = item;
		});

		items = items.concat(data[type.key]);
	});

	return items;
}

function adaptSearchResult(data) {
	const normalize = (str) => {
		const dom = document.createElement('p');
		dom.innerHTML = str.replace(/(<[^>]*>)/g, '');
		return dom.innerText;
	};

	return data.results.map(topic => ({
		inventoryType: 'documentation',
		description: normalize(topic.htmlExcerpt),
		title: normalize(topic.htmlTitle),
		url: topic.occurrences[0].readerUrl,
	}));
}

export default {
	search,
	goTo,
};
