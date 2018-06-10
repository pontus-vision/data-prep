function getActionInRow(rowIndex, actionSelector) {
	return cy.get(`#list-${rowIndex}-title-cell`)
		.within(() => {
			return cy.get(actionSelector);
		});
}

function getList() {
	return cy.get('.ReactVirtualized__Table__row');
}

export default {
	getActionInRow,
	getList,
};
