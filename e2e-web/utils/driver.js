/**
 * Shared Selenium WebDriver setup for all specs.
 * Runs headless Chrome via Selenium 4's built-in Selenium Manager, which
 * detects whatever Chrome is installed and fetches a matching driver —
 * portable across machines/CI runners without pinning a chromedriver
 * version that could drift out of sync with the local Chrome build.
 */
const { Builder, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

async function buildDriver() {
  const options = new chrome.Options();
  options.addArguments(
    '--headless=new',
    '--disable-gpu',
    '--window-size=1440,1000',
    '--no-sandbox',
    '--disable-dev-shm-usage'
  );
  const driver = await new Builder()
    .forBrowser('chrome')
    .setChromeOptions(options)
    .build();
  return driver;
}

/**
 * Waits until the element located by `locator` does/doesn't have `className`
 * in its class list. Polls the live class attribute rather than relying on
 * a single snapshot, since the app toggles classes asynchronously.
 */
async function waitForClassCondition(driver, locator, className, shouldHave, timeout = 8000) {
  const el = await driver.wait(until.elementLocated(locator), timeout);
  await driver.wait(async () => {
    const cls = (await el.getAttribute('class')) || '';
    const has = cls.split(/\s+/).includes(className);
    return shouldHave ? has : !has;
  }, timeout, `Timed out waiting for element matching ${locator} to ${shouldHave ? 'have' : 'not have'} class "${className}"`);
  return el;
}

/**
 * Clicks an element reliably on long pages. The site's sticky header can
 * intercept a plain WebDriver click when the browser's default
 * scrollIntoView() lands the element's top edge right under it, so this
 * scrolls the element to the vertical center of the viewport first and
 * falls back to a JS click if a real click is still intercepted.
 */
async function safeClick(driver, element) {
  await driver.executeScript(
    "arguments[0].scrollIntoView({block: 'center', inline: 'center'});",
    element
  );
  try {
    await element.click();
  } catch (err) {
    const message = String((err && err.message) || err);
    if (/click intercepted/i.test(message) || /not clickable/i.test(message)) {
      await driver.executeScript('arguments[0].click();', element);
    } else {
      throw err;
    }
  }
}

module.exports = { buildDriver, BASE_URL, waitForClassCondition, safeClick };
