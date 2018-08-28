import React from 'react';
import { ConfirmDialog } from '@talend/react-components';

export default class FolderCreatorModal extends React.Component {
	constructor(props) {
		super(props);
		this.onSubmit = this.onSubmit.bind(this);
	}
	onSubmit(event) {
		const folderName = this.folderNameInput.value;
		this.props.validateAction.onClick(event, { name: folderName });
	}
	render() {
		const newProps = {
			...this.props,
			validateAction: {
				...this.props.validateAction,
				onClick: this.onSubmit,
			},
		};
		return (
			<ConfirmDialog {...newProps}>
				<form className="form-group">
					<input
						className="form-control"
						id="add-folder-input"
						type="text"
						autoFocus
						ref={(input) => {
							this.folderNameInput = input;
						}}
					/>
					<label className="control-label" htmlFor="add-folder-input">
						Enter folder name
					</label>
				</form>
			</ConfirmDialog>
		);
	}
}
FolderCreatorModal.displayName = 'FolderCreatorModal';

