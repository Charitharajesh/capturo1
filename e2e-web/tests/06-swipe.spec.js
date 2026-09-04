const assert = require('assert');
const { By, until } = require('selenium-webdriver');
const { buildDriver, BASE_URL } = require('../utils/driver');
const { signUp } = require('../utils/signIn');

describe('Try a Photographer — swipe favouriting', function () {
  let driver;

  before(async function () {
    driver = await buildDriver();
    await driver.get(BASE_URL);
    await signUp(driver, { name: 'Swipe Tester', email: 'swipe.tester@example.com', password: 'Passw0rd!23' });
    await driver.findElement(By.css('.applink[data-tab="try"]')).click();
    await driver.wait(until.elementLocated(By.css('#tab-try.active')), 5000);
  });

  after(async function () {
    if (driver) await driver.quit();
  });

  it('liking a photographer shows a "Saved to favourites" toast and advances the card', async function () {
    const nameEl = await driver.findElement(By.css('#tab-try .swipe-card .txt h3'));
    const likedName = await nameEl.getText();

    const likeBtn = await driver.findElement(By.css('#tab-try .swipe-actions .yes'));
    await likeBtn.click();

    const toast = await driver.findElement(By.id('toast'));
    await driver.wait(async () => (await toast.getText()).includes('Saved to favourites'), 4000);
    assert.ok((await toast.getText()).includes('Saved to favourites'), 'expected favourites toast');

    await driver.wait(async () => {
      const t = await driver.findElement(By.css('#tab-try .swipe-card .txt h3')).getText();
      return t !== likedName;
    }, 4000);
    const nextName = await driver.findElement(By.css('#tab-try .swipe-card .txt h3')).getText();
    assert.notStrictEqual(nextName, likedName, 'expected the swipe card to move on to the next photographer');
  });
});
