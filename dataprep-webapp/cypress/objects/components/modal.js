function getHeader() {
	return cy.get('.modal-header');
}

function getFooterButtons() {
	return cy.get('.modal-footer > .btn');
}

export default {
	getFooterButtons,
};
