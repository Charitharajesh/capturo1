const assert = require('assert');
const { By } = require('selenium-webdriver');
const { buildDriver, BASE_URL, waitForClassCondition } = require('../utils/driver');
const { signUp } = require('../utils/signIn');

describe('Sign out', function () {
  let driver;

  before(async function () {
    driver = await buildDriver();
    await driver.get(BASE_URL);
    await signUp(driver, { name: 'Logout Tester', email: 'logout.tester@example.com', password: 'Passw0rd!23' });
  });

  after(async function () {
    if (driver) await driver.quit();
  });

  it('opens the profile menu and logs out back to the marketing site', async function () {
    const avatar = await driver.findElement(By.css('.avatar'));
    await avatar.click();
    await waitForClassCondition(driver, By.id('profile-dropdown'), 'show', true, 5000);

    const logoutLink = await driver.findElement(By.css('#profile-dropdown a[onclick="signOut()"]'));
    assert.strictEqual(await logoutLink.isDisplayed(), true, 'expected Log out link to be visible');
    assert.strictEqual((await logoutLink.getText()).trim(), 'Log out');
    await logoutLink.click();

    await waitForClassCondition(driver, By.id('app-shell'), 'show', false, 5000);

    const site = await driver.findElement(By.id('site'));
    const appShell = await driver.findElement(By.id('app-shell'));
    assert.strictEqual(await site.isDisplayed(), true, 'expected marketing site (#site) visible after logout');
    assert.strictEqual(await appShell.isDisplayed(), false, 'expected app shell (#app-shell) hidden after logout');

    const toast = await driver.findElement(By.id('toast'));
    await driver.wait(async () => (await toast.getText()).includes('Signed out'), 4000);
    assert.ok((await toast.getText()).includes('Signed out'), 'expected "Signed out" toast');
  });
});
