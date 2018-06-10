import list from './list';

function getAddPreparation() {
	return cy.get('#preparation\\:add\\:open');
}

function getAddFolder() {
	return cy.get('#folder\\:add\\:open');
}

export default {
	getActionInRow: list.getActionInRow,
	getList: list.getList,
	getAddPreparation,
	getAddFolder,
};
