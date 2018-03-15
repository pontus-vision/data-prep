import React from 'react';
import PropTypes from 'prop-types';
import { Drawer } from '@talend/react-components';
import { Button } from 'react-bootstrap';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { addOpen } from '../actions/preparation';

function SecondDrawer({ goToAddPreparation, ...restProps }) {
	return (
		<Drawer.Animation className={'tc-with-drawer-wrapper'} onClose={goToAddPreparation} >
			{({ close }) => (
				<Drawer.Container id="second-drawer" stacked>
					<Drawer.Title title={'Second drawer'} />
					<div>
						This is the second drawer, deal with it
						<pre>{JSON.stringify(restProps, undefined, 4)}</pre>
					</div>
					<Drawer.Footer>
						<Button onClick={close}>Close</Button>
					</Drawer.Footer>
				</Drawer.Container>
			)}
		</Drawer.Animation>
	);
}
SecondDrawer.displayName = 'SecondDrawer';
SecondDrawer.propTypes = {
	goToAddPreparation: PropTypes.func,
};

function mapDispatchToProps(dispatch) {
	return {
		goToAddPreparation: bindActionCreators(addOpen, dispatch),
	};
}

export default connect(null, mapDispatchToProps)(SecondDrawer);
