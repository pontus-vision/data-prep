import { SEARCH } from '../constants/actions';

function search({ target }) {
	return { type: SEARCH, payload: target.value };
}

export default {
	search,
};
