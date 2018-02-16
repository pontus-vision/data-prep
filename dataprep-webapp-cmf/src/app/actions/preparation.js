import { FETCH_PREPARATIONS, OPEN_FOLDER } from '../constants';

export function fetchPreparationsOnEnter({ router, dispatch }) {
	dispatch({
		type: FETCH_PREPARATIONS,
		folderId: router.nextState.params.folderId,
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
