const assert = require('assert');
const { By } = require('selenium-webdriver');
const { buildDriver, BASE_URL, waitForClassCondition } = require('../utils/driver');

describe('Sign up flow', function () {
  let driver;
  const firstName = 'Priya';
  const fullName = `${firstName} Sharma`;

  before(async function () {
    driver = await buildDriver();
    await driver.get(BASE_URL);
  });

  after(async function () {
    if (driver) await driver.quit();
  });

  it('opens the auth overlay when Get Started is clicked', async function () {
    const getStarted = await driver.findElement(By.css('header .nav-cta a.btn-primary'));
    await getStarted.click();

    const overlay = await waitForClassCondition(driver, By.id('auth-overlay'), 'show', true, 5000);
    assert.strictEqual(await overlay.isDisplayed(), true, 'expected auth overlay to be visible');

    const title = await driver.findElement(By.id('auth-title'));
    assert.strictEqual(await title.getText(), 'Create your account');
  });

  it('submits name/email/password and lands in the signed-in app shell', async function () {
    const nameField = await driver.findElement(By.id('auth-name'));
    const emailField = await driver.findElement(By.css('.auth-body input[placeholder="Email address"]'));
    const passwordField = await driver.findElement(By.css('.auth-body input[placeholder="Password"]'));
    const confirmField = await driver.findElement(By.id('auth-confirm'));

    await nameField.sendKeys(fullName);
    await emailField.sendKeys('priya.sharma@example.com');
    await passwordField.sendKeys('SuperSecret123!');
    await confirmField.sendKeys('SuperSecret123!');

    const submit = await driver.findElement(By.id('auth-cta'));
    await submit.click();

    await waitForClassCondition(driver, By.id('site'), 'hidden', true, 8000);
    await waitForClassCondition(driver, By.id('app-shell'), 'show', true, 8000);

    const site = await driver.findElement(By.id('site'));
    const appShell = await driver.findElement(By.id('app-shell'));
    assert.strictEqual(await site.isDisplayed(), false, 'expected marketing site (#site) to be hidden after sign-up');
    assert.strictEqual(await appShell.isDisplayed(), true, 'expected app shell (#app-shell) to be visible after sign-up');

    const greeting = await driver.findElement(By.css('#tab-home h1'));
    const greetingText = await greeting.getText();
    assert.ok(greetingText.includes(firstName), `expected Home greeting to contain "${firstName}", got "${greetingText}"`);
  });
});
