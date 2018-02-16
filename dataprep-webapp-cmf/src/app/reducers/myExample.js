import { ALERT } from '../constants';

const defaultState = {};

export default function myExampleReducer(state = defaultState, action) {
	switch (action.type) {
		case ALERT:
			alert(action.payload);
			return state;
		default:
			return state;
	}
}
