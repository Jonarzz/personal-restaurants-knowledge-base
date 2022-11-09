describe('restaurants page', () => {

  it('create new restaurant entry', () => {
    cy.visit("/");

    cy.intercept('GET', '/restaurants*')
      .as('restaurantsFetch');

    cy.getSubmitButton()
      .click();

    cy.wait('@restaurantsFetch')
      .then(interception => {
        expect(interception.response?.statusCode).is.equal(200);
      });
  });

});