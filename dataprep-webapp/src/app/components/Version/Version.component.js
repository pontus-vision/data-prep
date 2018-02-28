import React from 'react';
import PropTypes from 'prop-types';
import Icon from '@talend/react-components';

export default class Version extends React.PureComponent {

	render() {
		return (
			<div>
				<p><Icon name="talend-tdp-colored" /></p>
				<dl className="dl-horizontal">
					<dt>VERSION</dt>
					Ò
					<dd>{this.props.displayVersion}</dd>
					{
						this.props.services && this.props.services.map(s => (
							<div>
								<dt>{s.serviceName}</dt>
								<dd>{`${s.versionId} (${s.buildId})`}</dd>
							</div>
						))
					}
				</dl>
			</div>
		);
	}
}
Version.displayName = 'Version';
Version.propTypes = {
	displayVersion: PropTypes.string,
	services: PropTypes.array,
};
