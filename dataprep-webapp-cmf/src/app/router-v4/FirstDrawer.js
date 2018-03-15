import React from 'react';
import PropTypes from 'prop-types';
import { Drawer } from '@talend/react-components';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { Button } from 'react-bootstrap';
import { addOpenAddDataset, menuPreparation } from '../actions/preparation';

function FirstDrawer({ goToPreparations, addDataset, ...restProps }) {
	return (
		<Drawer.Animation className={'tc-with-drawer-wrapper'} onClose={goToPreparations} >
			{({ close }) => (
				<Drawer.Container id="first-drawer" stacked>
					<Drawer.Title title={'First drawer'} />
					<div>
						This is the first drawer, deal with it
						<pre>{JSON.stringify(restProps, undefined, 4)}</pre>
					</div>
					<Drawer.Footer>
						<Button onClick={close}>Close</Button>
						<Button bsStyle={'info'} onClick={addDataset}>Add Dataset</Button>
					</Drawer.Footer>
				</Drawer.Container>
			)}
		</Drawer.Animation>
	);
}
FirstDrawer.displayName = 'FirstDrawer';
FirstDrawer.propTypes = {
	addDataset: PropTypes.func,
	goToPreparations: PropTypes.func,
};

function mapDispatchToProps(dispatch) {
	return {
		addDataset: bindActionCreators(addOpenAddDataset, dispatch),
		goToPreparations: bindActionCreators(menuPreparation, dispatch),
	};
}

export default connect(null, mapDispatchToProps)(FirstDrawer);
