import { put } from 'redux-saga/effects';
import { actions } from '@talend/react-cmf';

function* fetch() {
	yield put(
		// TODO Call Backend to get all the product list
		actions.http.get('http://localhost:8888/api/version', {
			cmf: {
				collectionId: 'products',
			},
			transform() {
				return [
					{
						icon: 'talend-tdp-colored',
						key: 'talend-data-preparation',
						label: 'Data Preparation',
						href: 'https://talend-cloud-integration-tdp.datapwn.com/',
					},
					{
						icon: 'talend-tdp-colored',
						key: 'talend-data-preparation2',
						label: 'Data Preparation 2',
						href: 'https://talend-cloud-integration-tdp.datapwn.com/',
					},
				];
			},
		}),
	);
}


export default {
	fetch,
};
