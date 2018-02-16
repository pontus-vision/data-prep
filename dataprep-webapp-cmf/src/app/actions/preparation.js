import { actions } from '@talend/react-cmf';
import { RENAME_PREPARATION, SET_TITLE_EDITION_MODE, PREPARATION_DUPLICATE } from '../constants';

export function fetchPreparations() {
	return actions.http.get('http://localhost:8888/api/folders/Lw==/preparations', {
		cmf: {
			collectionId: 'preparations',
		},
		transform({ folders, preparations }) {
			const adaptedFolders = folders.map(folder => ({
				id: folder.id,
				name: folder.name,
				author: folder.ownerId,
				icon: 'talend-folder',
			}));
			const adaptedPreparations = preparations.map(prep => ({
				id: prep.id,
				name: prep.name,
				author: prep.author,
				icon: 'talend-dataprep',
				datasetName: prep.dataset.dataSetName,
				nbSteps: prep.steps.length - 1,
			}));

			return adaptedFolders.concat(adaptedPreparations);
		},
	});
}

export function duplicatePreparation(event, { model }) {
	return {
		type: PREPARATION_DUPLICATE,
		payload: {
			id: model.id,
		},
	};
}

export function renamePreparation(event, { model }) {
	return {
		type: RENAME_PREPARATION,
	};
}

export function setTitleEditionMode(event, { model }) {
	return {
		type: SET_TITLE_EDITION_MODE,
		payload: model.id,
	};
}
