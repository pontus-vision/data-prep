import { actions } from '@talend/react-cmf';
import { take, put } from 'redux-saga/effects';
import { FETCH_PREPARATIONS } from '../constants';

export function* fetchPreparations() {
	while (true) {
		const { folderId = 'Lw==' } = yield take(FETCH_PREPARATIONS);
		yield put(
			actions.http.get(`http://localhost:8888/api/folders/${folderId}/preparations`, {
				cmf: {
					collectionId: 'preparations',
				},
				transform({ folders, preparations }) {
					const adaptedFolders = folders.map(folder => ({
						id: folder.id,
						type: 'folder',
						name: folder.name,
						author: folder.ownerId,
						icon: 'talend-folder',
					}));
					const adaptedPreparations = preparations.map(prep => ({
						id: prep.id,
						type: 'preparation',
						name: prep.name,
						author: prep.author,
						icon: 'talend-dataprep',
						datasetName: prep.dataset.dataSetName,
						nbSteps: prep.steps.length - 1,
					}));
					return adaptedFolders.concat(adaptedPreparations);
				},
			})
		);
	}
}
