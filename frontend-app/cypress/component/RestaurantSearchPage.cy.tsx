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

                cy.get('input#nameBeginsWith')
                  .should('exist')
                  .get('input#category')
                  .should('exist')
                  .get('input#ratingAtLeast')
                  .should('exist')
                  .get('button#triedBefore')
                  .should('have.text', 'Not tried before')
                  .get('button[type="submit"]')
                  .should('have.text', 'Search')
                  .get('table')
                  .should('not.exist');
              });

              it('create new restaurant', () => {
                const name = 'Trattoria Napoli';
                const triedBefore = true;
                const rating = 9;
                const review = 'Great pizza!';
                const notes = ['I have to try all of their pizzas', 'Great value for money'];
                const categories = ['PIZZA'];
                cy.intercept('POST', '/restaurants', req => {
                  req.reply({ name, triedBefore, rating, review, notes, categories })
                }).as('restaurantCreation')

                cy.mount(<RestaurantSearchPage/>)
                  .get('button')
                  .contains('Add')
                  .click();

                cy.get('.ant-modal-body input#name')
                  .type(name);
                cy.get('.ant-modal-body input#categories')
                  .type('piz{enter}', {force: true})
                  .blur();
                cy.get('.ant-modal-body .ant-switch-inner')
                  .click();
                cy.get('.ant-modal-body input#rating')
                  .parents('.ant-select-selector')
                  .click()
                  .get('.ant-select-item')
                  .contains('' + rating)
                  .click();
                cy.get('.ant-modal-body textarea#review')
                  .type(review)
                  .get('.ant-modal-body textarea#notes')
                  .type(notes.join('\n'));
                cy.get('.ant-modal-footer')
                  .contains('Create')
                  .click();

                cy.wait('@restaurantCreation')
                  .then(interception => {
                    assert.deepEqual(interception.request.body, {
                      name, triedBefore, rating, review, notes, categories
                    });
                  });
              });

              it('restaurant not visited before edited after trying out', () => {
                const name = 'Super Tasty Burger';
                const triedBefore = true;
                const rating = 7;
                const review = 'This is a test review';
                const notes = ['This is the first note', 'This is the second note', 'And this is the third note'];
                const categories = ['BURGER', 'SANDWICH'];
                cy.intercept('PATCH', '/restaurants/' + encodeURIComponent(name), req => {
                  req.reply({ name, triedBefore, rating, review, notes, categories })
                }).as('restaurantUpdate')

                cy.mount(<RestaurantSearchPage/>)
                  .get('button[type="submit"]')
                  .click();

                cy.get('td > button')
                  .contains(name)
                  .click();
                cy.get('.ant-modal-body .ant-switch-inner')
                  .click();
                cy.get('.ant-modal-body input#rating')
                  .parents('.ant-select-selector')
                  .click()
                  .get('.ant-select-item')
                  .contains('' + rating)
                  .click();
                cy.get('.ant-modal-body textarea#review')
                  .type(review)
                  .get('.ant-modal-body textarea#notes')
                  .type(notes.join('\n'));
                cy.get('.ant-modal-footer')
                  .contains('Update')
                  .click();

                cy.wait('@restaurantUpdate')
                  .then(interception => {
                    assert.deepEqual(interception.request.body, {
                      name, triedBefore, rating, review, notes, categories
                    });
                  });
              });

              // TODO extract cypress commands
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
        .get('button[type="submit"]')
        .click();

      const expectedHeaderCells = [
        'Name', 'Categories', 'Tried before', 'Rating', 'Review', 'Notes',
      ];
      const expectedCells = [
        'Super Tasty Burger', 'Burger, sandwich', '', '', '', '',
        'Burger King City Centre', 'Burger, fast food', '', '5', 'Show', 'Show',
      ];
      const expectedNotes = [
        `Whooper has plenty of white onion`,
        `Double cheeseburger is OK for the price`,
        `Fries can be great, but can also be mediocre - it's a lottery`,
      ];
      cy.get('thead > tr > th')
        .should('have.length', expectedHeaderCells.length)
        .each((cell, index) => expect(cell.text()).to.be.equal(expectedHeaderCells[index]));
      cy.get('tbody > tr > td')
        .should('have.length', expectedCells.length)
        .each((cell, index) => expect(cell.text()).to.be.equal(expectedCells[index]))
        .contains('td:nth-of-type(5)', 'Show')
        .trigger('mouseover')
        .get('.ant-popover')
        .should('have.text', `Not the greatest burger, but it's all right for a fast food I guess`)
        .get('tbody')
        .contains('td:nth-of-type(6)', 'Show')
        .trigger('mouseover')
        .get('.ant-popover li')
        .each((item, index) => expect(item.text()).to.be.equal(expectedNotes[index]));
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
        .get('input#nameBeginsWith')
        .type(nameBeginsWith)
        .get('input#category')
        .click()
        .get('.ant-select-item')
        .contains(category, {matchCase: false})
        .click()
        .get('input#ratingAtLeast')
        .click()
        .get('.ant-select-item')
        .contains(ratingAtLeast)
        .click()
        .get('button#triedBefore')
        .click()
        .should('have.text', 'Tried before')
        .get('button[type="submit"]')
        .click();

      const expectedHeaderCells = [
        'Name', 'Rating',
      ];
      cy.get('thead > tr > th')
        .should('have.length', expectedHeaderCells.length)
        .each((cell, index) => expect(cell.text()).to.be.equal(expectedHeaderCells[index]));
      const expectedCells = [
        'Burger King City Centre', '5',
      ];
      cy.get('tbody > tr > td')
        .should('have.length', expectedCells.length)
        .each((cell, index) => expect(cell.text()).to.be.equal(expectedCells[index]));
    });

    it('full restaurants data is available in modal after click', () => {
      cy.mount(<RestaurantSearchPage/>)
        .get('button[type="submit"]')
        .click();

      cy.get('td > button')
        .first()
        .click()
        .get('.ant-modal-header > .ant-modal-title')
        .should('have.text', 'Super Tasty Burger')
        .get('.ant-modal-body input#name')
        .should('have.value', 'Super Tasty Burger')
        .get('.ant-modal-body .ant-select-selection-item-content')
        .should('contain.text', 'Burger', 'Sandwich')
        .get('.ant-modal-body .ant-switch-inner')
        .should('have.text', 'Not tried before')
        .get('.ant-modal-body input#rating')
        .should('not.have.value')
        .should('be.disabled')
        .get('.ant-modal-body textarea#review')
        .should('not.have.value')
        .should('be.disabled')
        .should('have.attr', 'placeholder', 'Review')
        .get('.ant-modal-body textarea#notes')
        .should('not.have.value')
        .should('not.be.disabled')
        .should('have.attr', 'placeholder', 'Notes (separated with newlines)')
        .get('.ant-modal-footer')
        .contains('Cancel')
        .click();
    });

  });

});