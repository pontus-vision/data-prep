import { SEARCH } from '../constants';

function searchFor({ target }) {
	return { type: SEARCH, payload: target.value };
}

export default {
	searchFor,
};
