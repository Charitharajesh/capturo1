const assert = require('assert');
const { By, until } = require('selenium-webdriver');
const { buildDriver, BASE_URL, safeClick } = require('../utils/driver');
const { signUp } = require('../utils/signIn');

describe('Photographer detail and booking flow', function () {
  let driver;

  before(async function () {
    driver = await buildDriver();
    await driver.get(BASE_URL);
    await signUp(driver, { name: 'Booking Tester', email: 'booking.tester@example.com', password: 'Passw0rd!23' });
    await driver.findElement(By.css('.applink[data-tab="home"]')).click();
    await driver.wait(until.elementLocated(By.css('#tab-home.active')), 5000);
  });

  after(async function () {
    if (driver) await driver.quit();
  });

  it('opens a photographer detail page with name, portfolio grid and packages', async function () {
    const firstCard = await driver.findElement(By.css('#tab-home .app-photog-card'));
    const cardName = (await firstCard.findElement(By.css('.name')).getText()).trim();
    await safeClick(driver, firstCard);

    await driver.wait(until.elementLocated(By.css('#tab-detail.active')), 5000);

    const heading = await driver.findElement(By.css('.detail-hero-web h1'));
    const headingText = (await heading.getText()).trim();
    assert.ok(headingText.length > 0, 'expected a photographer name in the detail heading');
    assert.ok(
      headingText.startsWith(cardName.replace('✓', '').trim()) || cardName.startsWith(headingText.replace('✓', '').trim()),
      `expected detail heading "${headingText}" to match the clicked card "${cardName}"`
    );

    const portfolioItems = await driver.findElements(By.css('.portfolio-grid .p'));
    assert.ok(portfolioItems.length >= 6, `expected a populated portfolio grid, got ${portfolioItems.length} items`);

    const packageCards = await driver.findElements(By.css('#pkg-list .pkg-card'));
    assert.strictEqual(packageCards.length, 3, `expected 3 package options, got ${packageCards.length}`);
  });

  it('selects a package, books it, and confirms via toast + notification badge', async function () {
    const badge = await driver.findElement(By.id('notif-badge'));
    const badgeBefore = await badge.getText();

    const packageCards = await driver.findElements(By.css('#pkg-list .pkg-card'));
    await safeClick(driver, packageCards[1]);
    await driver.wait(async () => (await packageCards[1].getAttribute('class')).includes('selected'), 3000);
    assert.ok((await packageCards[1].getAttribute('class')).includes('selected'), 'expected the 2nd package to be selected');
    assert.ok(!(await packageCards[0].getAttribute('class')).includes('selected'), 'expected the 1st package to be deselected');

    const bookBtn = await driver.findElement(By.css('.booking-box button.btn-primary'));
    await safeClick(driver, bookBtn);

    // Booking navigates back to the Bookings tab
    await driver.wait(until.elementLocated(By.css('#tab-bookings.active')), 5000);

    // Immediate "Booking request sent" toast confirms the request
    const toast = await driver.findElement(By.id('toast'));
    await driver.wait(async () => (await toast.getText()).includes('Booking request sent'), 4000);
    assert.ok((await toast.getText()).includes('Booking request sent'), 'expected booking-request toast');

    // Unread notification badge should have increased from its prior value
    await driver.wait(async () => (await badge.getText()) !== badgeBefore, 4000);
    const badgeAfter = await badge.getText();
    assert.notStrictEqual(badgeAfter, badgeBefore, `expected unread badge to change from "${badgeBefore}"`);

    // The app simulates a confirmation ~5s later via setTimeout — wait for it for real.
    // #notif-list lives inside the closed .notif-dropdown (display:none), so read its
    // raw textContent (DOM state) rather than getText() (which only sees rendered text).
    const notifList = await driver.findElement(By.id('notif-list'));
    await driver.wait(async () => (await notifList.getAttribute('textContent')).includes('Booking confirmed!'), 7000);
    const listText = await notifList.getAttribute('textContent');
    assert.ok(listText.includes('Booking confirmed!'), 'expected a "Booking confirmed!" notification to arrive');
  });
});
