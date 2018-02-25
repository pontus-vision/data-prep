import { actions } from '@talend/react-cmf';
import {
	CANCEL_RENAME_PREPARATION,
	FETCH_PREPARATIONS,
	OPEN_FOLDER,
	PREPARATION_DUPLICATE,
	RENAME_PREPARATION,
	SET_TITLE_EDITION_MODE,
} from '../constants';

export function fetchPreparationsOnEnter({ router, dispatch }) {
	dispatch({
		type: FETCH_PREPARATIONS,
		folderId: router.match.params.folderId,
	});
}

export function openPreparation(event, { id, type }) {
	switch (type) {
		case 'folder':
			return {
				type: OPEN_FOLDER,
				id,
				cmf: {
					routerPush: `/preparations/${id}`,
				},
			};
		case 'preparation':
			/* TODO
			- get current url
			- pass it in the url as 'return' query param
			- playground must redirect to this return param on close
			*/
			window.location.href = `http://localhost:3000/#/playground/preparation?prepid=${id}`;
			break;
		default:
			break;
	}
}

export function duplicate(event, { model }) {
	return {
		type: PREPARATION_DUPLICATE,
		payload: {
			id: model.id,
		},
	};
}

export function rename(event, data) {
	return {
		type: RENAME_PREPARATION,
		payload: {
			id: data.model.id,
			name: data.value,
		},
	};
}

export function cancelRename(event, { id }) {
	return {
		type: CANCEL_RENAME_PREPARATION,
		payload: id,
	};
}

export function setTitleEditionMode(event, { model }) {
	return {
		type: SET_TITLE_EDITION_MODE,
		payload: model.id,
	};
}
