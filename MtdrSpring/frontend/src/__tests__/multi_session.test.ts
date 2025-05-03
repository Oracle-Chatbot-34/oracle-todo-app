import { Builder, By, WebDriver } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome';
import 'chromedriver';
import { login } from './utils';
jest.setTimeout(60000); // Increase timeout for multi-tab operations


describe('Multi-Tab Session Test', () => {
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

  test('User can login in multiple tabs simultaneously', async () => {
    // Login in the first tab
    await login(driver);
    console.log('Successfully logged in on first tab');

    // Get current window handle to return to it later
    const firstTabHandle = await driver.getWindowHandle();

    // Create a new tab
    await driver.executeScript('window.open()');

    // Get all window handles
    const windowHandles = await driver.getAllWindowHandles();

    // Switch to the new tab (the second window handle)
    const secondTabHandle = windowHandles.find(
      (handle) => handle !== firstTabHandle
    );
    if (!secondTabHandle) {
      throw new Error('Could not create second tab');
    }

    await driver.switchTo().window(secondTabHandle);

    // Try to login in the second tab
    try {
      await login(driver);
      console.log('Successfully logged in on second tab');

      // Verify that we're actually logged in on the second tab
      // Get current URL to confirm we're on a post-login page
      const currentUrl = await driver.getCurrentUrl();
      expect(currentUrl).not.toBe('http://localhost:5173/login');

      // Check for presence of an element that would only appear when logged in
      const navbarElement = await driver.findElement(By.id('navbar'));
      expect(await navbarElement.isDisplayed()).toBe(true);

      // Switch back to first tab to verify session is still active there
      await driver.switchTo().window(firstTabHandle);

      // Verify we're still logged in on the first tab
      const firstTabUrl = await driver.getCurrentUrl();
      expect(firstTabUrl).not.toBe('http://localhost:5173/login');

      // Check for presence of navbar in first tab
      const firstTabNavbar = await driver.findElement(By.id('navbar'));
      expect(await firstTabNavbar.isDisplayed()).toBe(true);

      // If we get here, the test has passed
      console.log(
        'Test passed: User can be logged in on multiple tabs simultaneously'
      );
    } catch (error) {
      console.error('Failed to login on second tab:', error);
      throw new Error('Failed to maintain multi-tab session');
    }
  });
});
