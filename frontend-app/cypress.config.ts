import {defineConfig} from 'cypress';

export default defineConfig({
  component: {
    specPattern: "cypress/component/*",
    devServer: {
      framework: "create-react-app",
      bundler: "webpack",
    },
  },

  e2e: {
    specPattern: "cypress/e2e/*",
    baseUrl: "http://localhost:3000",
  },
});
