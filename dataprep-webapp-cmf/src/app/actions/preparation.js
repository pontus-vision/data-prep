import { actions } from '@talend/react-cmf';

export function fetchPreparations() {
	return actions.http.get('http://localhost:8888/api/folders/Lw==/preparations', {
		cmf: {
			collectionId: 'preparations',
		},
		transform({ folders, preparations }) {
			const adaptedFolders = folders.map(folder => ({
				name: folder.name,
				author: folder.ownerId,
				icon: 'talend-folder',
			}));
			const adaptedPreparations = preparations.map(prep => ({
				name: prep.name,
				author: prep.author,
				icon: 'talend-dataprep',
			}));
			return adaptedFolders.concat(adaptedPreparations);
		},
	});
}
