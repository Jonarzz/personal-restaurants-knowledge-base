import {Category} from '../../src/api';
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
                  req.reply({name, categories, triedBefore, rating, review, notes});
                }).as('restaurantCreation');

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
                      name, categories, triedBefore, rating, review, notes,
                    });
                  });

                cy.verifyNotificationHeader('Restaurant created')
                  .verifyNotificationContent('You can find the restaurant using the search form')
                  // not closing the notification explicitly - waiting for it to disappear
                  .verifySearchFormEmpty()
                  .getTable()
                  .should('not.exist');
              });

              it('edit restaurant not visited before after trying out', () => {
                const name = 'Super Tasty Burger',
                  triedBefore = true,
                  rating = 7,
                  review = 'This is a test review',
                  notes = ['This is the first note', 'This is the second note', 'And this is the third note'],
                  initialCategories = [Category.Burger, Category.Sandwich],
                  updatedCategories = [Category.Burger, Category.Sandwich, Category.FastFood];
                cy.intercept('GET', '/restaurants?triedBefore=false', req => req.reply([{name, categories: initialCategories}]));
                cy.intercept('PUT', '/restaurants/' + encodeURIComponent(name), req => req.reply({triedBefore}))
                  .as('restaurantUpdate');

                cy.mount(<RestaurantSearchPage/>)
                  .clickSubmitButton();

                cy.clickButton(name)
                  .clickTriedBeforeModalSwitch()
                  .typeInCategoriesModalField('fast')
                  .selectRatingInModal(rating)
                  .typeInReviewModalField(review)
                  .typeInNotesModalField(notes)
                  .clickButton('Update');

                cy.wait('@restaurantUpdate')
                  .then(interception => {
                    assert.deepEqual(interception.request.body, {
                      name, categories: updatedCategories, triedBefore, rating, review, notes,
                    });
                  });

                cy.verifyNotificationHeader('Restaurant updated')
                  .verifyNotificationContent('You can find the restaurant using the search form')
                  .closeNotification()
                  .verifySearchFormEmpty()
                  .getTable()
                  .should('not.exist');
              });

              it('mark visited before restaurant as not visited with rename', () => {
                const name = 'Burger King City Centre',
                  newName = 'New test name',
                  triedBefore = false,
                  newNote = 'New test note',
                  categories = [Category.Burger, Category.FastFood];
                cy.intercept('GET', '/restaurants?triedBefore=true', req => req.reply([{
                  name, categories, triedBefore: true, rating: 6, review: 'Note empty review', notes: ['First', 'Second']
                }]));
                cy.intercept('PUT', '/restaurants/' + encodeURIComponent(name), req => req.reply({triedBefore}))
                  .as('restaurantUpdate');

                cy.mount(<RestaurantSearchPage/>)
                  .clickTriedBeforeSearchButton()
                  .clickSubmitButton();

                cy.clickButton(name)
                  .getNameModalField()
                  .clear()
                  .type(newName)
                  .clickTriedBeforeModalSwitch()
                  .getNotesModalField()
                  .clear()
                  .type(newNote)
                  .getModalTitle()
                  .should('have.text', name)
                  .clickButton('Update');

                cy.wait('@restaurantUpdate')
                  .then(interception => {
                    assert.deepEqual(interception.request.body, {
                      name: newName, categories, triedBefore, notes: [newNote],
                    });
                  });

                cy.verifyNotificationHeader('Restaurant updated')
                  .verifyNotificationContent('You can find the restaurant using the search form')
                  .closeNotification()
                  .verifySearchFormEmpty()
                  .getTable()
                  .should('not.exist');
              });

              it('verify restaurant modal elements interaction', () => {
                cy.mount(<RestaurantSearchPage/>)
                  .clickButton('Add');

                // select all categories
                for (let category in Category) {
                  cy.typeInCategoriesModalField(category.substring(0, 4));
                }
                cy.get('.ant-select-selector .ant-select-selection-item')
                  .should('have.length', Object.keys(Category).length)
                  .get('.ant-select-clear')
                  .click({force: true});

                // select all ratings
                cy.clickTriedBeforeModalSwitch();
                for (let i = 1; i <= 10; i++) {
                  cy.selectRatingInModal(i);
                }

                // button should be disabled without restaurant name and a category typed in
                cy.verifyPrimaryModalButtonDisabled()
                  .typeInNameModalField('Test')
                  .verifyPrimaryModalButtonDisabled()
                  .typeInCategoriesModalField('burg')
                  .verifyPrimaryModalButtonDisabled(false)
                  .getNameModalField()
                  .clear()
                  .verifyPrimaryModalButtonDisabled();
              });

            });
          });
  });

  describe('Desktop', () => {

    beforeEach(() => {
      const {height, width} = DEVICES.Desktop;
      cy.viewport(width, height);

      cy.fixture('restaurants')
        .then(json => {
          cy.intercept({
            method: 'GET',
            pathname: '/restaurants',
            query: {
              triedBefore: 'false',
            },
          }, json);
        });
    });

    it('restaurant elements are rendered in the table on search without criteria', () => {
      cy.mount(<RestaurantSearchPage/>)
        .clickSubmitButton();

      cy.verifyTableHeaderCells('Name', 'Categories', 'Tried before', 'Rating', 'Review', 'Notes')
        .verifyTableRows([
          ['Super Tasty Burger', 'Burger, sandwich', '', '', '', ''],
          ['Burger King City Centre', 'Burger, fast food', '', '5', 'Show review', 'Show notes'],
        ])
        .verifyRowReviewTooltip(2, `Not the greatest burger, but it's all right for a fast food I guess`)
        .verifyRowNotesTooltip(2,
          `Whooper has plenty of white onion`,
          `Double cheeseburger is OK for the price`,
          `Fries can be great, but can also be mediocre - it's a lottery`,
        );
    });

  });

  describe('Mobile', () => {

    const criteria = {
      nameBeginsWith: 'Burger King',
      category: 'BURGER',
      ratingAtLeast: '3',
    };

    beforeEach(() => {
      const {height, width} = DEVICES.Mobile;
      cy.viewport(width, height);

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
          ['Burger King City Centre', '5'],
        ]);
    });

    it('view restaurants details in modal after click', () => {
      const notTriedBeforeRestaurant = 'Super Tasty Burger';
      const triedBeforeRestaurant = 'Burger King City Centre';

      cy.mount(<RestaurantSearchPage/>)
        .clickSubmitButton();

      cy.clickButton(notTriedBeforeRestaurant)
        .getModalTitle()
        .should('have.text', notTriedBeforeRestaurant)
        .getNameModalField()
        .should('have.value', notTriedBeforeRestaurant)
        .get('.ant-modal-body .ant-select-selection-item-content')
        .should('contain.text', 'Burger', 'Sandwich')
        .getTriedBeforeModalSwitch()
        .should('have.text', 'Not tried before')
        .getRatingModalField()
        .should('be.disabled')
        .parent()
        .should('not.have.text')
        .getReviewModalField()
        .should('not.have.value')
        .should('be.disabled')
        .should('have.attr', 'placeholder', 'Review')
        .getNotesModalField()
        .should('not.have.value')
        .should('not.be.disabled')
        .should('have.attr', 'placeholder', 'Notes (separated with newlines)')
        .clickButton('Cancel');

      cy.clickButton(triedBeforeRestaurant)
        .getModalTitle()
        .should('have.text', triedBeforeRestaurant)
        .getNameModalField()
        .should('have.value', triedBeforeRestaurant)
        .get('.ant-modal-body .ant-select-selection-item-content')
        .should('contain.text', 'Burger', 'Fast Food')
        .getTriedBeforeModalSwitch()
        .should('have.text', 'Tried before')
        .getRatingModalField()
        .should('not.be.disabled')
        .parents('.ant-select-selector')
        .should('have.text', '5')
        .getReviewModalField()
        .should('have.value', `Not the greatest burger, but it's all right for a fast food I guess`)
        .should('not.be.disabled')
        .getNotesModalField()
        .should('contain.text',
          `Whooper has plenty of white onion\n`
          + `Double cheeseburger is OK for the price\n`
          + `Fries can be great, but can also be mediocre - it's a lottery`)
        .should('not.be.disabled')
        .clickButton('Cancel');
    });

  });

});