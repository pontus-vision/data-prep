import { combineReducers } from 'redux';
// import { reducers as semanticReducers } from '@talend/data-quality-semantic-ee/lib/app/reducers';

const rootReducer = {
	app: combineReducers({
		// semanticApp: semanticReducers.SemanticAppReducer,
		// semanticForm: semanticReducers.SemanticFormReducer,
	}),
};

export default rootReducer;
