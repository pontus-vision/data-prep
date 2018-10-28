describe('Folder', () => {
	const folderName = `cypress-folder-${new Date()}`;

	before(() => {
		cy
			.login('fabien@dataprep.com', 'fabien');
		// .server()
		// .route('/api/version', 'fixture:versions');
	});
	describe('add', () => {
		beforeEach(() => {
			cy
				.get('#folder\\:add\\:open')
				.click();
		});

		afterEach(() => {
			cy
				.get('.modal')
				.within(() => {
					cy
						.get('.btn-default')
						.click();
				});
		});

		it('should render', () => {
			cy
				.get('.modal')
				.should('exist');
		});

		it('should create folder', () => {
			cy
				.get('.modal')
				.should('exist')
				.get('input#add-folder-input')
				.type(`${folderName}{enter}`);
		});

		it('should disappear while pressing escape key', () => {
			cy
				.get('.modal')
				.should('exist')
				.get('.modal')
				.type('{esc}')
				.get('.modal')
				.should('not.exist');
		});

		it('should disappear while clicking on cancel button', () => {
			cy
				.get('.modal')
				.should('exist')
				.get('.btn-default')
				.click()
				.get('.modal')
				.should('not.exist');
		});

		it('should disappear while clicking on modal backdrop', () => {
			cy
				.get('.modal')
				.should('exist')
				.get('.modal-backdrop')
				.click({ force: true })
				.get('.modal')
				.should('not.exist');
		});
	});

	describe('remove', () => {
	});
});
