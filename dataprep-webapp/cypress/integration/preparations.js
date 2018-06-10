import home from '../objects/pages/home';

describe('Preparations', () => {
	before(() => {
		cy.server();
		cy.route('**/api/folders/Lw==/preparations', 'fixture:preparations').as('fetch');
		cy.visit('http://localhost:3000/preparations');
		cy.wait('@fetch');
	});

	it('should list', () => {
		home.preparations.getList().should('have.length', 4);
	});

	context('folder', () => {
		it('should rename', () => {
			// click is forced because it's hidden
			home.preparations.getActionInRow(0, '#preparation\\:rename').click({ force: true });
			cy.get('#list-0-title-cell input').should('have.value', 'test folder 1');
			// cy.get('#list-0-title-cell input').type('test folder 1.1{enter}');
		});
	});
});
