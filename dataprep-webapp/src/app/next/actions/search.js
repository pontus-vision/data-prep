import { SEARCH_FOR } from '../constants';

function searchFor({ target }) {
	return { type: SEARCH_FOR, payload: target.value };
}

export default {
	searchFor,
};
