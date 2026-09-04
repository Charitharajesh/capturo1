const assert = require('assert');
const { By } = require('selenium-webdriver');
const { buildDriver, BASE_URL } = require('../utils/driver');

describe('Landing page', function () {
  let driver;

  before(async function () {
    driver = await buildDriver();
    await driver.get(BASE_URL);
  });

  after(async function () {
    if (driver) await driver.quit();
  });

  it('loads with the Capturo title and hero heading', async function () {
    const title = await driver.getTitle();
    assert.ok(title.includes('Capturo'), `expected title to include "Capturo", got "${title}"`);

    const heroHeading = await driver.findElement(By.css('.hero-copy h1'));
    const heroText = await heroHeading.getText();
    assert.ok(heroText.includes('Capture Moments'), `expected hero heading to include "Capture Moments", got "${heroText}"`);
    assert.ok(heroText.includes('Book Memories'), `expected hero heading to include "Book Memories", got "${heroText}"`);
  });

  it('shows Sign In and Get Started in the nav bar', async function () {
    const navCta = await driver.findElement(By.css('header .nav-cta'));
    const navText = await navCta.getText();
    assert.ok(navText.includes('Sign In'), `expected nav to include "Sign In", got "${navText}"`);
    assert.ok(navText.includes('Get Started'), `expected nav to include "Get Started", got "${navText}"`);

    const signIn = await driver.findElement(By.css('header .nav-cta a.btn-outline'));
    const getStarted = await driver.findElement(By.css('header .nav-cta a.btn-primary'));
    assert.strictEqual(await signIn.isDisplayed(), true, 'expected Sign In button to be visible');
    assert.strictEqual(await getStarted.isDisplayed(), true, 'expected Get Started button to be visible');
  });
});
