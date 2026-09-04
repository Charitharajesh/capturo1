const assert = require('assert');
const { By } = require('selenium-webdriver');
const { buildDriver, BASE_URL } = require('../utils/driver');

describe('Security smoke checks', function () {
  let driver;
  before(async function () { driver = await buildDriver(); await driver.get(BASE_URL); });
  after(async function () { if (driver) await driver.quit(); });

  it('does not expose common secret markers in the page source', async function () {
    const source = (await driver.getPageSource()).toLowerCase();
    ['service_role', 'supabase_key', 'private_key'].forEach((marker) => {
      assert.ok(!source.includes(marker), `secret marker exposed: ${marker}`);
    });
  });

  it('uses password controls and no insecure external resources', async function () {
    assert.ok((await driver.findElements(By.css('input[type="password"]'))).length > 0);
    const insecure = await driver.executeScript(`return [...document.querySelectorAll('script[src],link[href],img[src],form[action]')]
      .map((element) => element.src || element.href || element.action).filter((url) => url.startsWith('http://'));`);
    assert.deepStrictEqual(insecure, []);
  });
});