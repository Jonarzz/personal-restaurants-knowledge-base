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

            });
          });
  });

  describe('Desktop', () => {

    beforeEach(() => {
      const {height, width} = DEVICES.Desktop;
      cy.viewport(width, height);
    });

    it('restaurant elements are rendered in table on search', () => {
      cy.fixture('restaurants').then(json => {
        cy.intercept({
          method: 'GET',
          pathname: '/restaurants',
          query: {
            triedBefore: 'false',
          },
        }, json);
      });

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
        `Fries can be great, but can also be mediocre - it's a lottery`
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

    it('restaurant elements are rendered in table on search', () => {
      const nameBeginsWith = 'Burger King';
      const category = 'BURGER';
      const ratingAtLeast = '3';
      cy.fixture('restaurants').then(json => {
        cy.intercept({
          method: 'GET',
          pathname: '/restaurants',
          query: {
            nameBeginsWith, category, ratingAtLeast,
            triedBefore: 'true',
          },
        }, [json[1]]);
      });

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
        'Name', 'Categories',
      ];
      cy.get('thead > tr > th')
        .should('have.length', expectedHeaderCells.length)
        .each((cell, index) => expect(cell.text()).to.be.equal(expectedHeaderCells[index]));
      const expectedCells = [
        'Burger King City Centre', 'Burger, fast food',
      ];
      cy.get('tbody > tr > td')
        .should('have.length', expectedCells.length)
        .each((cell, index) => expect(cell.text()).to.be.equal(expectedCells[index]));
    });

    // TODO test restaurant editing and details on mobile (same modal)

  });

});