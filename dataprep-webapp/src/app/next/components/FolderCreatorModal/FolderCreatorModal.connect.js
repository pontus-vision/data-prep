import cmf, { cmfConnect } from '@talend/react-cmf';
import { actionAPI } from '@talend/react-containers';

import { Map } from 'immutable';

import FolderCreatorModal from './FolderCreatorModal.component';

export function mapStateToProps(state, props, cmfProps) {
	const context = {
		registry: cmf.registry.getRegistry(),
		store: {
			getState: () => state,
		},
	};
	const validateAction = cmfProps.state ? cmfProps.state.get('validateAction') : undefined;
	const cancelAction = cmfProps.state ? cmfProps.state.get('cancelAction') : undefined;
	const onChangeAction = cmfProps.state ? cmfProps.state.get('onChangeAction') : undefined;
	const model = cmfProps.state ? cmfProps.state.get('model') : cmfProps.model;
	return {
		...cmfProps.state && cmfProps.state.toJS(),
		validateAction: actionAPI.getProps(context, validateAction, model),
		cancelAction: actionAPI.getProps(context, cancelAction, model),
		onChangeAction: actionAPI.getProps(context, onChangeAction, model),
	};
}

export default cmfConnect({
	componentId: 'default',
	defaultState: new Map({ show: false }),
	mapStateToProps,
})(FolderCreatorModal);
