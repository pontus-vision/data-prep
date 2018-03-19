// TODO Clean it to just keep color definitions
const SASS_DATA = `
$brand-primary: #4F93A7;

$brand-primary-t7: #00A1B3;
$brand-secondary-t7: #168AA6;

$tc-drawer-content-max-width: 100%;
@mixin flex-full-height(){
	display: flex;
	flex-direction: column;
	flex-grow: 1;
}
@mixin flex-scroll(){
	min-height: 0;
	overflow-y: auto;
}

@import '~@talend/bootstrap-theme/src/theme/guidelines';
`;

module.exports = SASS_DATA;
