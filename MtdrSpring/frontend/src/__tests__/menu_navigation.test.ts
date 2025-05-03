// login.test.ts

import { Builder, By, until, WebDriver } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome';
import 'chromedriver';
import { login } from './utils';
jest.setTimeout(30000); // Increase timeout for Selenium operations

describe('Response to user navigation using navbar links', () => {
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

  test('Navbar can be navigated correctly', async () => {
  
   // Step 1: Login to the application
    await login(driver);

    // Step 2: Get the navbar element and identfy the links
    const navbar = await driver.findElement(By.id('navbar'));
    const tasksLink = await navbar.findElement(By.linkText('Tasks'));

    // Step 3: Click on the Tasks link
    tasksLink.click();

    // Step 4: Wait for the Tasks page to load
    const tasksPageDetected = await driver.wait(
      until.elementLocated(By.id('tasks-page')),
      10000
    );


    // Step 5: Verify that the Tasks page is displayed
    expect(tasksPageDetected).toBeTruthy();

  });
});
