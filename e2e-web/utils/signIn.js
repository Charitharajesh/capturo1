/**
 * Shared sign-up helper used by every spec that needs to be inside the
 * signed-in app shell. Drives the real auth overlay fields found in
 * capturo-web/index.html (#auth-name, #auth-confirm, and the unlabeled
 * email/password fields identified by their placeholder text).
 */
const { By } = require('selenium-webdriver');
const { waitForClassCondition } = require('./driver');

async function openGetStarted(driver) {
  const getStartedLink = await driver.findElement(By.css('header .nav-cta a.btn-primary'));
  await getStartedLink.click();
  await waitForClassCondition(driver, By.id('auth-overlay'), 'show', true, 5000);
}

async function signUp(driver, { name, email, password }) {
  await openGetStarted(driver);

  const nameField = await driver.findElement(By.id('auth-name'));
  const emailField = await driver.findElement(By.css('.auth-body input[placeholder="Email address"]'));
  const passwordField = await driver.findElement(By.css('.auth-body input[placeholder="Password"]'));
  const confirmField = await driver.findElement(By.id('auth-confirm'));

  await nameField.clear();
  await nameField.sendKeys(name);
  await emailField.clear();
  await emailField.sendKeys(email);
  await passwordField.clear();
  await passwordField.sendKeys(password);
  await confirmField.clear();
  await confirmField.sendKeys(password);

  const submit = await driver.findElement(By.id('auth-cta'));
  await submit.click();

  await waitForClassCondition(driver, By.id('site'), 'hidden', true, 8000);
  await waitForClassCondition(driver, By.id('app-shell'), 'show', true, 8000);
}

module.exports = { openGetStarted, signUp };
