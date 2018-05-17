import { call } from 'redux-saga/effects';
import { default as productSaga } from '../product.saga';

function* fetchAll() {
	yield call(productSaga.fetch);
}


export default {
	'HeaderBar#handle': fetchAll,
};
