import React from 'react';
import { cmfConnect } from '@talend/react-cmf';
import { Map } from 'immutable';
import FolderCreatorModal from './FolderCreatorModal.component';

export function FolderCreatorContainer(props) {
	const state = props.state;
	const validateAction = state.get('validateAction');
	const cancelAction = state.get('cancelAction');
	const newProps = {
		...state.toJS(),
		validateAction: {
			...validateAction,
			onClick: (event, data) => props.dispatchActionCreator(validateAction.actionCreator, event, data),
		},
		cancelAction: {
			...cancelAction,
			onClick: (event, data) => props.dispatchActionCreator(cancelAction.actionCreator, event, data),
		},
	};

	return <FolderCreatorModal {...newProps} />;
}

FolderCreatorContainer.displayName = 'FolderCreatorModal';
FolderCreatorContainer.propTypes = {
	...cmfConnect.propTypes,
};

export default cmfConnect({
	componentId: 'default',
	defaultState: new Map({ show: false }),
})(FolderCreatorContainer);
