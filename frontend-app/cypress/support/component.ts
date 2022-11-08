import 'antd/dist/antd.dark.css';
import {mount} from 'cypress/react18';
import {
  clickButton, clickSubmitButton, clickTriedBeforeModalSwitch, clickTriedBeforeSearchButton, getCategoriesModalField, getCategorySearchField, getModalTitle,
  getNameModalField, getNameSearchField, getNotesModalField, getRatingModalField, getRatingSearchField, getReviewModalField, getSubmitButton, getTable,
  getTriedBeforeModalSwitch, getTriedBeforeSearchButton, selectRatingInModal, selectSearchCategory, selectSearchRating, typeInCategoriesModalField,
  typeInNameModalField, typeInNameSearchField, typeInNotesModalField, typeInReviewModalField, verifyNotesTooltip, verifyReviewTooltip, verifyTableHeaderCells,
  verifyTableRows,
} from './commands';

declare global {
  namespace Cypress {
    interface Chainable {
      mount: typeof mount,
      getNameSearchField: typeof getNameSearchField,
      getCategorySearchField: typeof getCategorySearchField,
      getRatingSearchField: typeof getRatingSearchField,
      getTriedBeforeSearchButton: typeof getTriedBeforeSearchButton,
      getSubmitButton: typeof getSubmitButton,
      getTable: typeof getTable,
      typeInNameSearchField: typeof typeInNameSearchField,
      selectSearchCategory: typeof selectSearchCategory,
      selectSearchRating: typeof selectSearchRating,
      clickTriedBeforeSearchButton: typeof clickTriedBeforeSearchButton,
      clickSubmitButton: typeof clickSubmitButton,
      verifyTableHeaderCells: typeof verifyTableHeaderCells,
      verifyTableRows: typeof verifyTableRows,
      verifyReviewTooltip: typeof verifyReviewTooltip,
      verifyNotesTooltip: typeof verifyNotesTooltip,
      getModalTitle: typeof getModalTitle,
      getNameModalField: typeof getNameModalField,
      getCategoriesModalField: typeof getCategoriesModalField,
      getTriedBeforeModalSwitch: typeof getTriedBeforeModalSwitch,
      getRatingModalField: typeof getRatingModalField,
      getReviewModalField: typeof getReviewModalField,
      getNotesModalField: typeof getNotesModalField,
      typeInNameModalField: typeof typeInNameModalField,
      typeInCategoriesModalField: typeof typeInCategoriesModalField,
      selectRatingInModal: typeof selectRatingInModal,
      typeInReviewModalField: typeof typeInReviewModalField,
      typeInNotesModalField: typeof typeInNotesModalField,
      clickTriedBeforeModalSwitch: typeof clickTriedBeforeModalSwitch,
      clickButton: typeof clickButton,
    }
  }
}

Cypress.Commands.addAll({
  mount,
  getNameSearchField,
  getCategorySearchField,
  getRatingSearchField,
  getTriedBeforeSearchButton,
  getSubmitButton,
  getTable,
  typeInNameSearchField,
  selectSearchCategory,
  selectSearchRating,
  clickTriedBeforeSearchButton,
  clickSubmitButton,
  verifyTableHeaderCells,
  verifyTableRows,
  verifyReviewTooltip,
  verifyNotesTooltip,
  getModalTitle,
  getNameModalField,
  getCategoriesModalField,
  getTriedBeforeModalSwitch,
  getRatingModalField,
  getReviewModalField,
  getNotesModalField,
  typeInNameModalField,
  typeInCategoriesModalField,
  selectRatingInModal,
  typeInReviewModalField,
  typeInNotesModalField,
  clickTriedBeforeModalSwitch,
  clickButton,
});