import { all, call, take } from 'redux-saga/effects';
import {
	PREPARATION_COPY,
	PREPARATION_MOVE,
	RENAME_PREPARATION,
	REMOVE_PREPARATION,
	REMOVE_FOLDER,
	FETCH_PREPARATIONS,
	OPEN_COPY_MODAL,
	OPEN_MOVE_MODAL,
	CLOSE_COPY_MOVE_MODAL,
	SET_TITLE_EDITION_MODE,
	OPEN_PREPARATION_CREATOR,
	CANCEL_RENAME_PREPARATION,
} from '../../constants/actions';
import * as effects from '../effects/preparation.effects';

function* cancelRename() {
	while (true) {
		const { payload } = yield take(CANCEL_RENAME_PREPARATION);
		yield call(effects.cancelRename, payload);
	}
}

function* fetch() {
	while (true) {
		const { payload } = yield take(FETCH_PREPARATIONS);
		yield call(effects.refresh, payload);
	}
}

function* rename() {
	while (true) {
		const { payload } = yield take(RENAME_PREPARATION);
		yield call(effects.rename, payload);
	}
}

function* removePreparation() {
	while (true) {
		const { payload } = yield take(REMOVE_PREPARATION);
		yield call(effects.removePreparation, payload);
	}
}

function* removeFolder() {
	while (true) {
		const { payload } = yield take(REMOVE_FOLDER);
		yield call(effects.removeFolder, payload);
	}
}

function* copy() {
	while (true) {
		const { payload } = yield take(PREPARATION_COPY);
		yield call(effects.copy, payload);
	}
}

function* move() {
	while (true) {
		const { payload } = yield take(PREPARATION_MOVE);
		yield call(effects.move, payload);
	}
}

function* setTitleEditionMode() {
	while (true) {
		const { payload } = yield take(SET_TITLE_EDITION_MODE);
		yield call(effects.setTitleEditionMode, payload);
	}
}

function* openPreparationCreatorModal() {
	while (true) {
		yield take(OPEN_PREPARATION_CREATOR);
		yield call(effects.openPreparationCreatorModal);
	}
}

function* openCopyModal() {
	while (true) {
		const { payload } = yield take(OPEN_COPY_MODAL);
		yield all([call(effects.fetchTree), call(effects.openCopyMoveModal, payload, 'copy')]);
	}
}

function* openMoveModal() {
	while (true) {
		const { payload } = yield take(OPEN_MOVE_MODAL);
		yield all([call(effects.fetchTree), call(effects.openCopyMoveModal, payload, 'move')]);
	}
}

function* closeCopyMoveModal() {
	while (true) {
		yield take(CLOSE_COPY_MOVE_MODAL);
		yield call(effects.closeCopyMoveModal);
	}
}

export default {
	'preparation:copy': copy,
	'preparation:move': move,
	'preparation:fetch': fetch,
	'preparation:remove': removePreparation,
	'preparation:folder:remove': removeFolder,
	'preparation:rename:submit': rename,
	'preparation:rename:cancel': cancelRename,
	'preparation:rename': setTitleEditionMode,
	'preparation:copy:open': openCopyModal,
	'preparation:move:open': openMoveModal,
	'preparation:copy:move:cancel': closeCopyMoveModal,
	'preparation:creator:open': openPreparationCreatorModal,
};
