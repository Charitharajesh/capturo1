const assert = require('assert');
const { By, until } = require('selenium-webdriver');
const { buildDriver, BASE_URL, waitForClassCondition } = require('../utils/driver');
const { signUp } = require('../utils/signIn');

describe('Notifications bell', function () {
  let driver;

  before(async function () {
    driver = await buildDriver();
    await driver.get(BASE_URL);
    await signUp(driver, { name: 'Notif Tester', email: 'notif.tester@example.com', password: 'Passw0rd!23' });
  });

  after(async function () {
    if (driver) await driver.quit();
  });

  it('shows an unread badge with a welcome notification, which clears on open', async function () {
    const badge = await driver.findElement(By.id('notif-badge'));
    await driver.wait(async () => !(await badge.getAttribute('class')).includes('hide'), 5000);
    const badgeText = await badge.getText();
    assert.ok(Number(badgeText) >= 1, `expected unread badge count >= 1, got "${badgeText}"`);

    const bell = await driver.findElement(By.css('.notif-bell'));
    await bell.click();
    await waitForClassCondition(driver, By.id('notif-dropdown'), 'show', true, 5000);

    const items = await driver.findElements(By.css('#notif-list .notif-item'));
    assert.ok(items.length >= 1, 'expected at least one notification item in the dropdown');
    const firstItemText = await items[0].getText();
    assert.ok(firstItemText.toLowerCase().includes('welcome'), `expected first notification to be the welcome message, got "${firstItemText}"`);

    // Opening the dropdown marks notifications read in this app -> badge clears
    await driver.wait(async () => (await badge.getAttribute('class')).includes('hide'), 5000);
    assert.ok((await badge.getAttribute('class')).includes('hide'), 'expected unread badge to hide after opening the dropdown');
  });

  it('a new notification re-shows the badge, and Mark all read clears it again', async function () {
    await driver.findElement(By.css('.applink[data-tab="try"]')).click();
    await driver.wait(until.elementLocated(By.css('#tab-try.active')), 5000);
    await driver.findElement(By.css('#tab-try .swipe-actions .yes')).click();

    const badge = await driver.findElement(By.id('notif-badge'));
    await driver.wait(async () => !(await badge.getAttribute('class')).includes('hide'), 5000);
    const unreadCount = Number(await badge.getText());
    assert.ok(unreadCount >= 1, `expected a new unread notification after liking, got badge "${await badge.getText()}"`);

    // Re-open the bell (a click outside .notif-wrap auto-closes the dropdown)
    await driver.findElement(By.css('.notif-bell')).click();
    await waitForClassCondition(driver, By.id('notif-dropdown'), 'show', true, 5000);

    const markAllRead = await driver.findElement(By.css('.notif-dropdown .head span'));
    await markAllRead.click();

    await driver.wait(async () => (await badge.getAttribute('class')).includes('hide'), 5000);
    assert.ok((await badge.getAttribute('class')).includes('hide'), 'expected badge to hide after Mark all read');

    const items = await driver.findElements(By.css('#notif-list .notif-item'));
    const texts = await Promise.all(items.map((i) => i.getText()));
    assert.ok(texts.some((t) => t.toLowerCase().includes('favourite')), 'expected a "Saved to favourites" notification in the list');
  });
});
