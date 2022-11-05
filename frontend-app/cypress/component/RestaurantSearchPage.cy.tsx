import {RestaurantSearchPage} from '../../src/features/restaurant/RestaurantSearchPage';

const DEVICES = {
  Desktop: {
    width: 1200,
    height: 600,
  },
  Mobile: {
    width: 360,
    height: 700,
  },
};

describe('Restaurant search page', () => {

  const criteria = {
    nameBeginsWith: 'Burger King',
    category: 'BURGER',
    ratingAtLeast: '3',
  };

  beforeEach(() => {
    cy.fixture('restaurants')
      .then(json => {
        cy.intercept({
          method: 'GET',
          pathname: '/restaurants',
          query: {
            triedBefore: 'false',
          },
        }, json);
        cy.intercept({
          method: 'GET',
          pathname: '/restaurants',
          query: {
            ...criteria,
            triedBefore: 'true',
          },
        }, [json[1]]);
      });
  });

  describe(`Common tests`, () => {

    Object.entries(DEVICES)
          .forEach(([device, {width, height}]) => {

            describe(device, () => {

              beforeEach(() => {
                cy.viewport(width, height);
              });

              it('form without table is rendered on load', () => {
                cy.mount(<RestaurantSearchPage/>);

                cy.getNameSearchField()
                  .should('exist')
                  .getCategorySearchField()
                  .should('exist')
                  .getRatingSearchField()
                  .should('exist')
                  .getTriedBeforeSearchButton()
                  .should('have.text', 'Not tried before')
                  .getSubmitButton()
                  .should('have.text', 'Search')
                  .getTable()
                  .should('not.exist');
              });

              it('create new restaurant', () => {
                const name = 'Trattoria Napoli',
                  triedBefore = true,
                  rating = 9,
                  review = 'Great pizza!',
                  notes = ['I have to try all of their pizzas', 'Great value for money'],
                  categories = ['PIZZA', 'PASTA'];
                cy.intercept('POST', '/restaurants', req => {
                  req.reply({ name, categories, triedBefore, rating, review, notes })
                }).as('restaurantCreation')

                cy.mount(<RestaurantSearchPage/>)
                  .clickButton('Add');

                cy.typeInNameModalField(name)
                  .typeInCategoriesModalField('piz')
                  .typeInCategoriesModalField('past')
                  .clickTriedBeforeModalSwitch()
                  .selectRatingInModal(rating)
                  .typeInReviewModalField(review)
                  .typeInNotesModalField(notes)
                  .clickButton('Create');

                cy.wait('@restaurantCreation')
                  .then(interception => {
                    assert.deepEqual(interception.request.body, {
                      name, categories, triedBefore, rating, review, notes
                    });
                  });
              });

              it('restaurant not visited before edited after trying out', () => {
                const name = 'Super Tasty Burger',
                  triedBefore = true,
                  rating = 7,
                  review = 'This is a test review',
                  notes = ['This is the first note', 'This is the second note', 'And this is the third note'],
                  categories = ['BURGER', 'SANDWICH'];
                cy.intercept('PATCH', '/restaurants/' + encodeURIComponent(name), req => {
                  req.reply({ name, categories, triedBefore, rating, review, notes })
                }).as('restaurantUpdate')

                cy.mount(<RestaurantSearchPage/>)
                  .clickSubmitButton();

                cy.clickButton(name)
                  .clickTriedBeforeModalSwitch()
                  .selectRatingInModal(rating)
                  .typeInReviewModalField(review)
                  .typeInNotesModalField(notes)
                  .clickButton('Update')

                cy.wait('@restaurantUpdate')
                  .then(interception => {
                    assert.deepEqual(interception.request.body, {
                      name, categories, triedBefore, rating, review, notes
                    });
                  });
              });

              // TODO edit with rename
              // TODO verify that button is disabled when name input is empty
              // TODO verify available options for category and rating

            });
          });
  });

  describe('Desktop', () => {

    beforeEach(() => {
      const {height, width} = DEVICES.Desktop;
      cy.viewport(width, height);
    });

    it('restaurant elements are rendered in the table on search without criteria', () => {
      cy.mount(<RestaurantSearchPage/>)
        .clickSubmitButton();

      cy.verifyTableHeaderCells('Name', 'Categories', 'Tried before', 'Rating', 'Review', 'Notes')
        .verifyTableRows([
          ['Super Tasty Burger', 'Burger, sandwich', '', '', '', ''],
          ['Burger King City Centre', 'Burger, fast food', '', '5', 'Show', 'Show'],
        ])
        .verifyReviewTooltip(`Not the greatest burger, but it's all right for a fast food I guess`)
        .verifyNotesTooltip(
          `Whooper has plenty of white onion`,
          `Double cheeseburger is OK for the price`,
          `Fries can be great, but can also be mediocre - it's a lottery`,
        );
    });

  });

  describe('Mobile', () => {

    beforeEach(() => {
      const {height, width} = DEVICES.Mobile;
      cy.viewport(width, height);
    });

    it('brief restaurants data is rendered in the table', () => {
      const {nameBeginsWith, category, ratingAtLeast} = criteria;

      cy.mount(<RestaurantSearchPage/>)
        .typeInNameSearchField(nameBeginsWith)
        .selectSearchCategory(category)
        .selectSearchRating(ratingAtLeast)
        .clickTriedBeforeSearchButton()
        .should('have.text', 'Tried before')
        .clickSubmitButton();

      cy.verifyTableHeaderCells('Name', 'Rating')
        .verifyTableRows([
          ['Burger King City Centre', '5']
        ]);
    });

    it('full restaurants data is available in modal after click', () => {
      cy.mount(<RestaurantSearchPage/>)
        .clickSubmitButton();

      cy.clickButton('Super Tasty Burger')
        .get('.ant-modal-header > .ant-modal-title')
        .should('have.text', 'Super Tasty Burger')
        .getNameModalField()
        .should('have.value', 'Super Tasty Burger')
        .get('.ant-modal-body .ant-select-selection-item-content')
        .should('contain.text', 'Burger', 'Sandwich')
        .getTriedBeforeModalSwitch()
        .should('have.text', 'Not tried before')
        .getRatingModalField()
        .should('not.have.value')
        .should('be.disabled')
        .getReviewModalField()
        .should('not.have.value')
        .should('be.disabled')
        .should('have.attr', 'placeholder', 'Review')
        .getNotesModalField()
        .should('not.have.value')
        .should('not.be.disabled')
        .should('have.attr', 'placeholder', 'Notes (separated with newlines)')
        .clickButton('Cancel');
    });

  });

});