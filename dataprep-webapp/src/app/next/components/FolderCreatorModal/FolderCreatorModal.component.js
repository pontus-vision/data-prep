import React from 'react';
import { ConfirmDialog } from '@talend/react-components';
import Immutable from 'immutable';
import { actionAPI } from '@talend/react-containers';
import cmf from '@talend/react-cmf/lib/index';

export default class FolderCreatorModal extends React.Component {
	constructor(props) {
		super(props);
		this.onChange = this.onChange.bind(this);
	}

	onChange() {
		const state = this.props.state;
		const model = state.get('model', new Immutable.Map());
		this.props.dispatchActionCreator('folder:add', event, model.toJS());
	}
	render() {
		const state = { ...this.props };
		if (!state.validateAction || !state.cancelAction) {
			return null;
		}
		const context = {
			registry: cmf.registry.getRegistry(),
			store: {
				getState: () => state,
			},
		};
		context.store.dispatch = this.props.dispatch;
		state.validateAction = actionAPI.getProps(context, this.props.validateAction);
		state.cancelAction = actionAPI.getProps(context, this.props.cancelAction);

		return (
			<ConfirmDialog {...state}>
				<form>
					<div className="form-group">
						<input className="form-control" id="add-folder-input" type="text" autoFocus onChange={this.onChange} />
						<label className="control-label" htmlFor="add-folder-input">Enter folder name</label>
					</div>
				</form>
			</ConfirmDialog>
		);
	}
}
FolderCreatorModal.displayName = 'FolderCreatorModal';

