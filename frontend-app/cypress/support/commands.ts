export const getNameSearchField = () => cy.get('form input#nameBeginsWith');
export const getCategorySearchField = () => cy.get('form input#category');
export const getRatingSearchField = () => cy.get('form input#ratingAtLeast');
export const getTriedBeforeSearchButton = () => cy.get('form button#triedBefore');
export const getSubmitButton = () => cy.get('form button[type="submit"]');
export const getTable = () => cy.get('table');

export const typeInNameSearchField = (text: string) => getNameSearchField().type(text);
export const selectSearchCategory = (category: string) =>
  getCategorySearchField().click()
                          .get('.ant-select-item')
                          .contains(category, {matchCase: false})
                          .click();
export const selectSearchRating = (rating: string) =>
  getRatingSearchField()
    .click()
    .get('.ant-select-item')
    .contains(rating)
    .click();
export const clickTriedBeforeSearchButton = () => getTriedBeforeSearchButton().click();
export const clickSubmitButton = () => getSubmitButton().click();

export const verifySearchFormEmpty = () => cy.getNameSearchField()
                                             .should('not.have.value')
                                             .getCategorySearchField()
                                             .should('not.have.value')
                                             .getRatingSearchField()
                                             .should('not.have.value')
                                             .getTriedBeforeSearchButton()
                                             .should('have.text', 'Not tried before');

export const verifyNotificationHeader = (expectedText: string) =>
  cy.get('.ant-notification-notice-message')
    .should('have.text', expectedText);
export const verifyNotificationContent = (expectedText: string) =>
  cy.get('.ant-notification-notice-description')
    .should('have.text', expectedText);
export const closeNotification = () => cy.get('.ant-notification-notice-close')
                                         .click();

export const verifyTableHeaderCells = (...expectedCells: string[]) =>
  cy.get('thead > tr > th')
    .should('have.length', expectedCells.length)
    .each((cell, index) => expect(cell.text()).to.be.equal(expectedCells[index]));
export const verifyTableRows = (expectedRows: string[][]) => {
  const expectedCells = expectedRows.flat();
  return cy.get('tbody > tr')
           .should('have.length', expectedRows.length)
           .children('td')
           .each((cell, index) => expect(cell.text()).to.be.equal(expectedCells[index]));
};

export const verifyReviewTooltip = (expectedReview: string) => verifyRowReviewTooltip(1, expectedReview);
export const verifyRowReviewTooltip = (rowNumber: number, expectedReview: string) =>
  cy.get(`tbody tr:nth-of-type(${rowNumber})`)
    .contains('td:nth-of-type(5)', 'Show review')
    .trigger('mouseover')
    .get('.ant-popover')
    .should('have.text', expectedReview);

export const verifyNotesTooltip = (...expectedNotes: string[]) => verifyRowNotesTooltip(1, ...expectedNotes);
export const verifyRowNotesTooltip = (rowNumber: number, ...expectedNotes: string[]) =>
  cy.get(`tbody tr:nth-of-type(${rowNumber})`)
    .contains('td:nth-of-type(6)', 'Show notes')
    .trigger('mouseover')
    .get('.ant-popover li')
    .should('have.length', expectedNotes.length)
    .each((item, index) => expect(item.text()).to.be.equal(expectedNotes[index]));

export const getModalTitle = () => cy.get('.ant-modal-header > .ant-modal-title');
export const getNameModalField = () => cy.get('.ant-modal-body input#name');
export const getCategoriesModalField = () => cy.get('.ant-modal-body input#categories');
export const getTriedBeforeModalSwitch = () => cy.get('.ant-modal-body .ant-switch-inner');
export const getRatingModalField = () => cy.get('.ant-modal-body input#rating');
export const getReviewModalField = () => cy.get('.ant-modal-body textarea#review');
export const getNotesModalField = () => cy.get('.ant-modal-body textarea#notes');

export const typeInNameModalField = (text: string) => getNameModalField().type(text);
export const typeInCategoriesModalField = (text: string) =>
  getCategoriesModalField()
    .type(text + '{enter}', {force: true})
    .blur();
export const clickTriedBeforeModalSwitch = () => getTriedBeforeModalSwitch().click();
export const selectRatingInModal = (rating: number) =>
  getRatingModalField()
    .parents('.ant-select-selector')
    .click()
    .get('.ant-select-item')
    .contains('' + rating)
    .click();
export const typeInReviewModalField = (text: string) => getReviewModalField().type(text);
export const typeInNotesModalField = (notes: string[]) => getNotesModalField().type(notes.join('\n'));


export const clickButton = (buttonText: string) => cy.get('button')
                                                     .contains(buttonText)
                                                     .click();

export const verifyPrimaryModalButtonDisabled = (expectDisabled = true) =>
  cy.get('.ant-modal-footer > .ant-btn-primary')
    .should((expectDisabled ? '' : 'not.') + 'be.disabled');