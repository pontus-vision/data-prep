/**
 * Export all your constants here
 * Ex: the actions types that are used by action creators and reducers
 */

export const DOCUMENTATION_SEARCH_URL = 'https://www.talendforge.org/find/api/THC.php';
export const TDP_SEARCH_URL = 'http://localhost:3000/api/search?path=/&name=';
export const DEBOUNCE_TIMEOUT = 500;
export const DEFAULT_SEARCH_PAYLOAD = {
	contentLocale: 'en',
	filters: [
		{ key: 'version', values: ['2.1'] },
		{ key: 'EnrichPlatform', values: ['Talend Data Preparation'] },
	],
	paging: { page: 1, perPage: 5 },
};

export const ALERT = 'ALERT';
export const CANCEL_RENAME_PREPARATION = 'CANCEL_RENAME_PREPARATION';
export const FETCH_PREPARATIONS = 'FETCH_PREPARATIONS';
export const OPEN_ABOUT = 'OPEN_ABOUT';
export const FETCH_VERSION = 'FETCH_VERSION';
export const OPEN_FOLDER = 'OPEN_FOLDER';
export const PREPARATION_DUPLICATE = 'PREPARATION_DUPLICATE';
export const RENAME_PREPARATION = 'RENAME_PREPARATION';
export const SET_TITLE_EDITION_MODE = 'SET_TITLE_EDITION_MODE';
export const OPEN_PREPARATION_CREATOR = 'OPEN_PREPARATION_CREATOR';
export const HELP = 'HELP';
export const SEARCH = 'SEARCH';
