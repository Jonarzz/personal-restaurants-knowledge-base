describe('restaurants page', () => {

  const pizzeriaName = 'Trattoria Napoli';
  const burgerJointName = 'Super Tasty Burger';

  it('initially an empty table is rendered on search', () => {
    cy.visit('/');

    cy.intercept('GET', '/restaurants*')
      .as('restaurantsFetch');

    cy.getSubmitButton()
      .click();

    cy.wait('@restaurantsFetch')
      .then(interception => {
        expect(interception.response?.statusCode).is.equal(200);
      });
    cy.get('table')
      .contains('No Data');
  });

  it('create not visited restaurant entry', () => {
    cy.clickButton('Add')
      .typeInNameModalField(pizzeriaName)
      .typeInCategoriesModalField('piz')
      .typeInCategoriesModalField('past')
      .clickButton('Create')
      .should('not.exist')
      .wait(100); // TODO fixup responsive rendering issues

    cy.clickSubmitButton()
      .verifyTableRows([
        [pizzeriaName, 'Pasta, pizza', '', '', '', ''],
      ]);
  });

  it('create visited restaurant entry', () => {
    const rating = 6;
    const review = 'It was OK';
    const notes = [
      'Horrible fries',
      'Pretty good meat',
      'Great buns',
    ];

    cy.clickButton('Add')
      .typeInNameModalField(burgerJointName)
      .typeInCategoriesModalField('burg')
      .typeInCategoriesModalField('fast')
      .clickTriedBeforeModalSwitch()
      .selectRatingInModal(rating)
      .typeInReviewModalField(review)
      .typeInNotesModalField(notes)
      .clickButton('Create')
      .should('not.exist')
      .wait(100); // TODO fixup responsive rendering issues

    cy.clickTriedBeforeSearchButton()
      .clickSubmitButton()
      .verifyTableRows([
        [burgerJointName, 'Burger, fast food', '', '' + rating, 'Show review', 'Show notes'],
      ])
      .verifyReviewTooltip(review)
      .verifyNotesTooltip(...notes);
  });

  it('edit not visited restaurant to visited', () => {
    const rating = 9;
    const review = 'Very good pizza';
    const notes = ['Red pizza is better than white pizza'];

    cy.clickTriedBeforeSearchButton()
      .clickSubmitButton();

    cy.clickButton(pizzeriaName)
      .clickTriedBeforeModalSwitch()
      .selectRatingInModal(rating)
      .typeInReviewModalField(review)
      .typeInNotesModalField(notes)
      .clickButton('Update')
      .should('not.exist')
      .wait(100); // TODO fixup responsive rendering issues

    cy.clickTriedBeforeSearchButton()
      .clickSubmitButton()
      .verifyTableRows([
        [burgerJointName, 'Burger, fast food', '', '6', 'Show review', 'Show notes'],
        [pizzeriaName, 'Pasta, pizza', '', '' + rating, 'Show review', 'Show notes'],
      ])
      .verifyRowReviewTooltip(2, review)
      .verifyRowNotesTooltip(2, ...notes);
  });

  it('edit visited restaurant to not visited', () => {
    cy.clickButton(burgerJointName)
      .clickTriedBeforeModalSwitch()
      .getNotesModalField()
      .clear()
      .clickButton('Update')
      .should('not.exist');

    cy.clickSubmitButton()
      .verifyTableRows([
        [burgerJointName, 'Burger, fast food', '', '', '', ''],
      ]);
  });

});