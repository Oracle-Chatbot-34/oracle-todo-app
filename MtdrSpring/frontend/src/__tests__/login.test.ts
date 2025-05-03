// login.test.ts

import { Builder, By, WebDriver } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome';
import 'chromedriver';
jest.setTimeout(30000); // Increase timeout for Selenium operations

describe('Login functionality test', () => {
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

  test('User can login and see correct username displayed', async () => {
    // Test data
    const loginUrl = 'http://localhost:5173/login';
    // Find a way to pull these from a config file or environment variables
    const testUsername = '';
    const testPassword = '';
    const expectedDisplayName = 'Roberto Morales';

    // Step 1: Navigate to the login page
    await driver.get(loginUrl);

    // Step 2: Verify login page loaded correctly
    const loginForm = await driver.findElement(By.css('form'));
    expect(await loginForm.isDisplayed()).toBe(true);

    // Step 3: Enter credentials
    const usernameField = await driver.findElement(By.id('username'));
    const passwordField = await driver.findElement(By.id('password'));

    await usernameField.sendKeys(testUsername);
    await passwordField.sendKeys(testPassword);

    // Step 4: Submit the form
    const submitButton = await driver.findElement(
      By.css('button[type="submit"]')
    );
    await submitButton.click();

    // Step 5: Wait for dashboard page to load
    // This assumes there's an element with id "dashboard" on the page after successful login
    try {
      // Wait for either navbar (success) or error message (failure)
      await driver.wait(async () => {
      try {
        // Check for success indicator (navbar)
        const navbarExists = await driver.findElements(By.id('navbar')).then(elements => elements.length > 0);
        
        // Check for error indicator
        const errorExists = await driver.findElements(By.id('error')).then(elements => elements.length > 0);
        
        // Return true if either element is found
        return navbarExists || errorExists;
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      } catch (_) {
        return false;
      }
      }, 10000, 'Timed out waiting for either navbar or error element');
      
      // Verify success by checking if error is present
      const errors = await driver.findElements(By.id('error'));
      if (errors.length > 0) {
      throw new Error('Login failed - error element detected');
      }
    } catch (e) {
      console.error('Login process failed:', e);
      throw e;
    }

    // Step 6: Verify the displayed username is correct
    // This assumes there's an element that contains the username - adjust the selector as needed
    const displayedUsername = await driver.findElement(
      By.id('user-display-name')
    );
    const actualUsername = await displayedUsername.getText();

    expect(actualUsername).toContain(expectedDisplayName);
  });
});
