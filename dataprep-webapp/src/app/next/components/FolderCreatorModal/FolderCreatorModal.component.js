import React from 'react';
import { ConfirmDialog } from '@talend/react-components';

export default class FolderCreatorModal extends React.Component {
	constructor(props) {
		super(props);
		this.onChange = this.onChange.bind(this);
	}

	onChange() {
		const name = this.folderNameInput.value;
		const validateAction = { ...this.props.state.validateAction };
		validateAction.disabled = !name;
		this.props.setState({ name, error: '', validateAction });
	}

	render() {
		return (
			<ConfirmDialog {...this.props.state}>
				<form>
					<div className="form-group field field-string">
						<input
							className="form-control"
							id="add-folder-input"
							type="text"
							autoFocus
							value={this.props.state.name}
							ref={(input) => {
								this.folderNameInput = input;
							}}
							onChange={this.onChange}
						/>
						<label className="control-label" htmlFor="add-folder-input">
							Enter folder name
						</label>
					</div>
				</form>
			</ConfirmDialog>
		);
	}
}
FolderCreatorModal.displayName = 'FolderCreatorModal';

