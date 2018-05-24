import React from 'react';
import PropTypes from 'prop-types';
import { IconsProvider } from '@talend/react-components';
import { Notification, ShortcutManager } from '@talend/react-containers';
import AppLoader from '@talend/react-containers/lib/AppLoader/index';
import AboutModal from './AboutModal';
import PreparationCreatorModal from '../../components/preparation-creator/index';

export default function App(props) {
	return (
		<AppLoader>
			<IconsProvider />
			<ShortcutManager view="shortcuts" />
			<Notification />
			<AboutModal />
			<PreparationCreatorModal />
			{props.children}
		</AppLoader>
	);
}

App.displayName = 'App';
App.propTypes = {
	children: PropTypes.element.isRequired,
};
