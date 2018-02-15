import React from 'react';
import PropTypes from 'prop-types';
import { Modal, Button } from 'react-bootstrap';
import ImmutablePropTypes from 'react-immutable-proptypes';

export default function AboutModal({ state, setState }) {
	return (
		<Modal
			show={state && state.get('show')}
			onHide={() => setState({ show: false })}
		>
			<Modal.Header>
				<Modal.Title>Talend Data Preparation</Modal.Title>
			</Modal.Header>

			<Modal.Body>Lol about</Modal.Body>
			<Modal.Footer>
				<Button onClick={() => setState({ show: false })}>Close</Button>
			</Modal.Footer>
		</Modal>
	);
}
AboutModal.displayName = 'AboutModal';
AboutModal.propTypes = {
	state: ImmutablePropTypes.contains({ show: PropTypes.bool }),
	setState: PropTypes.func,
};
