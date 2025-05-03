import { WebDriver, By, until } from "selenium-webdriver";

export async function login(driver: WebDriver) {
  await driver.get('http://localhost:5173/login');

  const usernameField = await driver.findElement(By.id('username'));
  const passwordField = await driver.findElement(By.id('password'));

  // Find a way to pull these from a config file or environment variables
  await usernameField.sendKeys('');
  await passwordField.sendKeys('');

  const submitButton = await driver.findElement(
    By.css('button[type="submit"]')
  );
  await submitButton.click();

  // Wait for dashboard to load
  await driver.wait(until.elementLocated(By.id('navbar')), 10000);
}
