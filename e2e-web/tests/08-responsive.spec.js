const assert = require('assert');
const { By } = require('selenium-webdriver');
const { buildDriver, BASE_URL } = require('../utils/driver');
const { signUp } = require('../utils/signIn');

// The app's own CSS defines the mobile breakpoint as max-width:860px
// (see .app-nav-links / .app-mobile-nav rules in capturo-web/index.html).
const BREAKPOINT_PX = 860;

describe('Responsive mobile nav', function () {
  let driver;

  before(async function () {
    driver = await buildDriver();
    await driver.get(BASE_URL);
    await signUp(driver, { name: 'Mobile Tester', email: 'mobile.tester@example.com', password: 'Passw0rd!23' });
  });

  after(async function () {
    if (driver) await driver.quit();
  });

  it('shows the mobile bottom nav and hides the desktop nav below the 860px breakpoint', async function () {
    // Above the breakpoint: desktop nav visible, mobile nav hidden
    await driver.manage().window().setRect({ width: BREAKPOINT_PX + 340, height: 900 });
    const desktopNav = await driver.findElement(By.css('.app-nav-links'));
    const mobileNav = await driver.findElement(By.css('.app-mobile-nav'));
    assert.strictEqual(await desktopNav.isDisplayed(), true, 'expected desktop nav links visible above the breakpoint');
    assert.strictEqual(await mobileNav.isDisplayed(), false, 'expected mobile bottom nav hidden above the breakpoint');

    // Below the breakpoint: narrow mobile width
    await driver.manage().window().setRect({ width: 375, height: 800 });
    await driver.wait(async () => await mobileNav.isDisplayed(), 5000);
    assert.strictEqual(await mobileNav.isDisplayed(), true, 'expected mobile bottom nav visible on a narrow viewport');
    assert.strictEqual(await desktopNav.isDisplayed(), false, 'expected desktop nav links hidden on a narrow viewport');
  });
});
