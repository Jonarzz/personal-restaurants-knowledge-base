describe('restaurants page', () => {

  it('create new restaurant entry', () => {
    cy.visit("/");

    cy.getSubmitButton()
      .click();
  });

});