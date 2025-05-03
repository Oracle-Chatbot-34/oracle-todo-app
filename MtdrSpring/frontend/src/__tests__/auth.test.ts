// login.test.ts

import { Builder, By, until, WebDriver } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome';
import 'chromedriver';
jest.setTimeout(30000); // Increase timeout for Selenium operations

describe('Check if an unautheticated user can navigate to a protected route', () => {
  let driver: WebDriver;

  beforeAll(async () => {
    // Set up Chrome options
    const options = new chrome.Options();
    // options.addArguments('--headless'); // Uncomment to run in headless mode

    // Initialize the WebDriver
    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(options)
      .build();
  });

  afterAll(async () => {
    // Clean up and close the browser
    await driver.quit();
  });

  test('User cannot access protected route', async () => {

    // Step 1: Navigate to protected route
    await driver.get('http://localhost:5173/tasks');

    // Step 2: Check if redirected to login page
    try {
      const loginElement = await driver.wait(
        until.elementLocated(By.id('login')),
        10000
      );
      
      // If we find the login element, it means redirection worked correctly
      expect(loginElement).toBeTruthy();
      
      // Optional: verify URL has changed to login page
      const currentUrl = await driver.getCurrentUrl();
      expect(currentUrl).toContain('login');
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (error) {
      // If element not found, the test should fail
      fail('User was not redirected to login page');
    }

  });
});
