import 'antd/dist/antd.dark.css';
import {mount} from 'cypress/react18';
import './commands';

declare global {
  namespace Cypress {
    interface Chainable {
      mount: typeof mount
    }
  }
}

Cypress.Commands.add('mount', mount);
