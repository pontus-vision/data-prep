import { actions } from '@talend/react-cmf';
import {
	RENAME_PREPARATION,
	CANCEL_RENAME_PREPARATION,
	SET_TITLE_EDITION_MODE,
	PREPARATION_DUPLICATE,
} from '../constants';

export function fetchAll() {
	return actions.http.get('http://localhost:8888/api/folders/Lw==/preparations', {
		cmf: {
			collectionId: 'preparations',
		},
		transform({ folders, preparations }) {
			const adaptedFolders = folders.map(folder => ({
				author: folder.ownerId,
				className: 'list-item-folder',
				icon: 'talend-folder',
				id: folder.id,
				name: folder.name,
			}));
			const adaptedPreparations = preparations.map(prep => ({
				author: prep.author,
				className: 'list-item-preparation',
				datasetName: prep.dataset.dataSetName,
				icon: 'talend-dataprep',
				id: prep.id,
				name: prep.name,
				nbSteps: prep.steps.length - 1,
			}));

			return adaptedFolders.concat(adaptedPreparations);
		},
	});
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
