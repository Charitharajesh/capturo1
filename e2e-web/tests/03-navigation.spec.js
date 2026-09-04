const assert = require('assert');
const { By, until } = require('selenium-webdriver');
const { buildDriver, BASE_URL } = require('../utils/driver');
const { signUp } = require('../utils/signIn');

describe('App navigation tabs', function () {
  let driver;

  before(async function () {
    driver = await buildDriver();
    await driver.get(BASE_URL);
    await signUp(driver, { name: 'Nav Tester', email: 'nav.tester@example.com', password: 'Passw0rd!23' });
  });

  after(async function () {
    if (driver) await driver.quit();
  });

  it('Home tab renders the photographer grid and hero banner', async function () {
    await driver.findElement(By.css('.applink[data-tab="home"]')).click();
    await driver.wait(until.elementLocated(By.css('#tab-home.active')), 5000);

    const cards = await driver.findElements(By.css('#tab-home .app-photog-card'));
    assert.ok(cards.length >= 8, `expected at least 8 photographer cards on Home, got ${cards.length}`);

    const hero = await driver.findElement(By.css('#tab-home .home-hero'));
    assert.strictEqual(await hero.isDisplayed(), true, 'expected Home hero banner to be visible');
  });

  it('Discover tab renders post cards', async function () {
    await driver.findElement(By.css('.applink[data-tab="discover"]')).click();
    await driver.wait(until.elementLocated(By.css('#tab-discover.active')), 5000);

    const posts = await driver.findElements(By.css('#tab-discover .discover-card'));
    assert.ok(posts.length >= 6, `expected at least 6 discover posts, got ${posts.length}`);
  });

  it('Try a Photographer tab renders a swipe card', async function () {
    await driver.findElement(By.css('.applink[data-tab="try"]')).click();
    await driver.wait(until.elementLocated(By.css('#tab-try.active')), 5000);

    const swipeCard = await driver.findElement(By.css('#tab-try .swipe-card'));
    assert.strictEqual(await swipeCard.isDisplayed(), true, 'expected a swipe card to be visible');

    const nameEl = await driver.findElement(By.css('#tab-try .swipe-card .txt h3'));
    const nameText = await nameEl.getText();
    assert.ok(nameText.length > 0, 'expected swipe card to show a photographer name');
  });

  it('Bookings tab renders status tabs and booking cards', async function () {
    await driver.findElement(By.css('.applink[data-tab="bookings"]')).click();
    await driver.wait(until.elementLocated(By.css('#tab-bookings.active')), 5000);

    const statusTabs = await driver.findElements(By.css('#tab-bookings .bk-tab'));
    assert.strictEqual(statusTabs.length, 3, `expected 3 booking status tabs, got ${statusTabs.length}`);

    const cards = await driver.findElements(By.css('#tab-bookings .bk-card'));
    assert.ok(cards.length >= 2, `expected at least 2 upcoming booking cards, got ${cards.length}`);
  });
});
