import { SEARCH, SEARCH_SELECT } from '../constants/actions';

function start({ target }) {
	return { type: SEARCH, payload: target.value };
}

function select(event, { itemIndex, sectionIndex }) {
	return { type: SEARCH_SELECT, payload: { sectionIndex, itemIndex } };
}

export default {
	start,
	select,
};
