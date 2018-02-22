import React from 'react';
import { IconsProvider } from '@talend/react-components';
import { Notification } from '@talend/react-containers';

import AboutModal from './about';
import PreparationCreatorModal from './preparation-creator';

export default function App(props) {
	/**
	 * Instanciate all global components here
	 * Ex : we register @talend/react-components <IconsProvider />
	 * so that all icons are available in each view
	 */
	return (
		<div>
			<IconsProvider />
			<Notification />
			<AboutModal />
			<PreparationCreatorModal />
			{props.children}
		</div>
	);
}

App.propTypes = {
	children: React.PropTypes.element,
};
