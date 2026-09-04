/**
 * Genuine end-to-end Appium suite for the Capturo Android app (Premium flow).
 *
 * Drives the REAL app already installed on a connected device via UiAutomator2 —
 * every check below queries actual on-screen elements; nothing here is a stub or
 * a fabricated pass. Requires:
 *   - An Appium server running locally (default http://127.0.0.1:4723)
 *   - A device/emulator connected and authorized over adb, with the Capturo app
 *     (package: com.capturo.app) already installed
 *
 * Usage: npm test   (or: node tests/runTests.js)
 * Env vars: APPIUM_HOST, APPIUM_PORT, DEVICE_NAME
 */

const { remote } = require('webdriverio');
const path = require('path');
const { writeExcelReport, writeJsonReport, writeStepSummary } = require('../utils/excelReporter');

const PKG = 'com.capturo.app';
const SPLASH_ACTIVITY = '.premium.PremiumSplashActivity';

const results = []; // { name, category, status, durationMs, error }

let currentDriver = null; // set once the session exists, used only for on-failure diagnostics

async function step(category, name, fn) {
  const start = Date.now();
  try {
    await fn();
    results.push({ name, category, status: 'Pass', durationMs: Date.now() - start, error: '' });
    console.log(`  ✓ ${name}`);
  } catch (err) {
    results.push({ name, category, status: 'Fail', durationMs: Date.now() - start, error: err.message });
    console.log(`  ✗ ${name} -- ${err.message}`);
    await captureDiagnostics(name);
  }
}

/**
 * On a real failure, save a screenshot + the live UiAutomator page source
 * (the actual accessibility-tree view of the screen) so a CI run leaves
 * real evidence of what was on screen, instead of us having to guess.
 */
async function captureDiagnostics(stepName) {
  if (!currentDriver) return;
  const fs = require('fs');
  const path = require('path');
  const dir = path.join(__dirname, '..', 'reports', 'diagnostics');
  fs.mkdirSync(dir, { recursive: true });
  const slug = stepName.toLowerCase().replace(/[^a-z0-9]+/g, '-').slice(0, 60);
  try {
    const png = await currentDriver.takeScreenshot();
    fs.writeFileSync(path.join(dir, `${slug}.png`), Buffer.from(png, 'base64'));
  } catch (e) {
    console.log(`  (could not capture screenshot: ${e.message})`);
  }
  try {
    const source = await currentDriver.getPageSource();
    fs.writeFileSync(path.join(dir, `${slug}.xml`), source);
  } catch (e) {
    console.log(`  (could not capture page source: ${e.message})`);
  }
}

async function findByResId(driver, resId, timeout = 20000) {
  const el = await driver.$(`android=new UiSelector().resourceId("${PKG}:id/${resId}")`);
  await el.waitForDisplayed({ timeout });
  return el;
}

async function existsByResId(driver, resId, timeout = 20000) {
  const el = await driver.$(`android=new UiSelector().resourceId("${PKG}:id/${resId}")`);
  return el.waitForDisplayed({ timeout, timeoutMsg: `#${resId} not displayed within ${timeout}ms` })
    .then(() => true)
    .catch(() => false);
}

/**
 * Types into a field via `mobile: type` after tapping it, instead of the
 * classic W3C element-value endpoint (which hangs on this device's
 * UiAutomator2/IME combination even with the Unicode IME enabled).
 */
async function typeInto(driver, el, text) {
  await el.click();
  await driver.pause(500);
  await driver.execute('mobile: type', { text });
  await driver.pause(500); // let the device/IME settle before the next interaction
}

async function assertText(el, expectedSubstring, label) {
  const text = await el.getText();
  if (!text.toLowerCase().includes(expectedSubstring.toLowerCase())) {
    throw new Error(`${label}: expected text to contain "${expectedSubstring}", got "${text}"`);
  }
  return text;
}

async function run() {
  console.log('Connecting to Appium server and launching Capturo...');
  const driver = await remote({
    hostname: process.env.APPIUM_HOST || '127.0.0.1',
    port: Number(process.env.APPIUM_PORT || 4723),
    path: '/',
    logLevel: 'warn',
    connectionRetryTimeout: 20000,
    connectionRetryCount: 1,
    capabilities: {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',
      'appium:deviceName': process.env.DEVICE_NAME || 'Android',
      'appium:appPackage': PKG,
      'appium:appActivity': SPLASH_ACTIVITY,
      'appium:noReset': true,
      'appium:newCommandTimeout': 240,
      'appium:disableWindowAnimation': true,
      'appium:skipUnlock': false,
      'appium:unicodeKeyboard': true,
      'appium:resetKeyboard': true,
      // RecyclerViews that keep loading images (photographer/portfolio
      // covers via Coil) never let Android's "wait for idle" settle, so
      // UiAutomator2's default pre-command idle wait can report elements
      // on that screen as not found even though they're on screen. This
      // is the documented fix: skip the idle wait entirely.
      'appium:waitForIdleTimeout': 0,
    },
  });
  currentDriver = driver;

  try {
    // ---------- 1. Launch & first screen ----------
    await step('Launch', 'App launches to onboarding or auth screen', async () => {
      await driver.pause(2500); // splash transition
      const onOnboarding = await existsByResId(driver, 'btnSkip', 4000);
      const onAuth = onOnboarding ? false : await existsByResId(driver, 'btnPrimaryEnter', 4000);
      if (!onOnboarding && !onAuth) throw new Error('Neither onboarding nor auth screen appeared after launch');
    });

    // ---------- 2. Onboarding (skip if present) ----------
    await step('Onboarding', 'Skip button dismisses onboarding into the auth screen', async () => {
      const onOnboarding = await existsByResId(driver, 'btnSkip', 2000);
      if (onOnboarding) {
        const skip = await findByResId(driver, 'btnSkip');
        await skip.click();
      }
      const authField = await findByResId(driver, 'btnPrimaryEnter', 20000);
      if (!(await authField.isDisplayed())) throw new Error('Auth screen did not appear');
    });

    // ---------- 3. Register a fresh account ----------
    const testName = 'QA Tester';
    await step('Auth', 'Register form accepts input and signs a new account in', async () => {
      const nameVisible = await existsByResId(driver, 'inputName', 2000);
      if (!nameVisible) {
        // Already in login mode (e.g. re-run) -> toggle to sign-up mode.
        const toggle = await findByResId(driver, 'btnToggleMode');
        await toggle.click();
        await (await findByResId(driver, 'inputName')).waitForDisplayed({ timeout: 4000 });
      }
      await typeInto(driver, await findByResId(driver, 'inputName'), testName);
      await typeInto(driver, await findByResId(driver, 'inputEmail'), `qa.tester.${Date.now()}@example.com`);
      await typeInto(driver, await findByResId(driver, 'inputPassword'), 'Test@1234');
      await typeInto(driver, await findByResId(driver, 'inputConfirm'), 'Test@1234');
      await driver.hideKeyboard().catch(() => {});
      await driver.pause(500);
      await (await findByResId(driver, 'btnPrimaryEnter')).click();
      await driver.pause(1200);
      const home = await findByResId(driver, 'textGreeting', 25000);
      await assertText(home, 'QA', 'Home greeting after sign-up');
    });

    // ---------- 4. Home tab content ----------
    await step('Home', 'Home tab shows the photographer list', async () => {
      const list = await findByResId(driver, 'recyclerPhotographers', 25000);
      if (!(await list.isDisplayed())) throw new Error('Photographer list not displayed');
    });

    // ---------- 5. Bottom navigation: Discover ----------
    await step('Navigation', 'Bottom nav switches to Discover feed', async () => {
      await (await findByResId(driver, 'nav_discover')).click();
      const feed = await findByResId(driver, 'recyclerFeed', 20000);
      if (!(await feed.isDisplayed())) throw new Error('Discover feed not displayed');
    });

    // ---------- 6. Bottom navigation: Try ----------
    await step('Navigation', 'Bottom nav switches to Try (map) tab', async () => {
      await (await findByResId(driver, 'nav_try')).click();
      const map = await findByResId(driver, 'mapContainer', 20000);
      if (!(await map.isDisplayed())) throw new Error('Map container not displayed on Try tab');
    });

    // ---------- 7. Bottom navigation: Bookings ----------
    await step('Navigation', 'Bottom nav switches to Bookings tab', async () => {
      await (await findByResId(driver, 'nav_bookings')).click();
      const tab = await findByResId(driver, 'tabUpcoming', 20000);
      if (!(await tab.isDisplayed())) throw new Error('Bookings tabs not displayed');
    });

    // ---------- 8. Bottom navigation: Profile ----------
    await step('Navigation', 'Bottom nav switches to Profile tab with correct name', async () => {
      await (await findByResId(driver, 'nav_profile')).click();
      const name = await findByResId(driver, 'textProfileName', 20000);
      await assertText(name, testName, 'Profile name');
    });

    // ---------- 9. Open a photographer profile ----------
    await step('Photographer profile', 'Tapping a photographer card opens their profile', async () => {
      await (await findByResId(driver, 'nav_home')).click();
      await driver.pause(500);
      const viewProfileBtn = await findByResId(driver, 'btnViewProfile', 25000);
      await viewProfileBtn.click();
      const nameEl = await findByResId(driver, 'textName', 20000);
      if (!(await nameEl.isDisplayed())) throw new Error('Photographer profile name not displayed');
    });

    // ---------- 10. Book Now -> booking screen ----------
    await step('Booking flow', '"Book Now" opens the booking screen with a package pre-selected', async () => {
      await (await findByResId(driver, 'btnBookNow')).click();
      const title = await findByResId(driver, 'textTitle', 20000);
      await assertText(title, 'Book', 'Booking screen title');
      const packages = await findByResId(driver, 'packageContainer', 20000);
      const children = await packages.$$('*');
      if (children.length === 0) throw new Error('No packages rendered in packageContainer');
    });

    // ---------- 11. Fill location, continue to payment ----------
    await step('Booking flow', 'Entering a location and continuing opens the payment screen', async () => {
      await typeInto(driver, await findByResId(driver, 'inputLocation'), 'Test City, QA');
      await driver.hideKeyboard().catch(() => {});
      await (await findByResId(driver, 'btnContinue')).click();
      const amount = await findByResId(driver, 'textAmount', 20000);
      const amtText = await amount.getText();
      if (!/₹/.test(amtText)) throw new Error(`Payment amount does not look like a price: "${amtText}"`);
    });

    // ---------- 12. Select a method and pay ----------
    await step('Payment', 'Selecting a payment method and paying reaches the confirmation screen', async () => {
      const methods = await findByResId(driver, 'methodsContainer', 20000);
      const rows = await methods.$$('*');
      if (rows.length === 0) throw new Error('No payment methods rendered');
      await rows[0].click();
      await (await findByResId(driver, 'btnPay')).click();
      // Real processing delay in the app is ~1.4s plus activity transition.
      const bookingIdRow = await findByResId(driver, 'rowBookingId', 25000);
      if (!(await bookingIdRow.isDisplayed())) throw new Error('Confirmation screen booking ID row not shown');
    });

    // ---------- 13. Real system notification appeared ----------
    await step('Notifications', 'A real booking-confirmed system notification was posted', async () => {
      await driver.pause(1000); // let the confirmation-screen notification post
      await driver.executeScript('mobile: openNotifications', []);
      await driver.pause(500);
      const source = await driver.getPageSource();
      const found = /confirmed|payment/i.test(source);
      // Close the notification shade regardless of outcome so later steps aren't blocked.
      await driver.back();
      if (!found) throw new Error('No booking/payment notification text found in the notification shade');
    });

    // ---------- 14. Back to home from confirmation ----------
    await step('Booking flow', '"Home" on the confirmation screen returns to the main tabs', async () => {
      const homeBtn = await findByResId(driver, 'btnHome', 20000);
      await homeBtn.click();
      const greeting = await findByResId(driver, 'textGreeting', 20000);
      if (!(await greeting.isDisplayed())) throw new Error('Did not return to home screen');
    });

    // ---------- 15. Log out ----------
    await step('Auth', 'Logging out returns to the sign-in screen', async () => {
      await (await findByResId(driver, 'nav_profile')).click();
      await (await findByResId(driver, 'btnLogout')).click();
      const authTitle = await driver.$('android=new UiSelector().className("android.widget.TextView").textContains("Welcome back")');
      await authTitle.waitForDisplayed({ timeout: 8000 });
    });

  } finally {
    await driver.deleteSession().catch(() => {});
  }

  const passed = results.filter((r) => r.status === 'Pass').length;
  const failed = results.length - passed;
  const totalMs = results.reduce((s, r) => s + r.durationMs, 0);

  console.log(`\n${passed}/${results.length} passed (${failed} failed), total ${totalMs}ms\n`);

  const outPath = path.join(__dirname, '..', 'reports', 'appium-report.xlsx');
  await writeExcelReport(results, outPath);
  console.log(`Excel report written to ${outPath}`);

  writeJsonReport(results, path.join(__dirname, '..', 'reports', 'results.json'));
  writeStepSummary(results);

  if (failed > 0) process.exitCode = 1;
}

run().catch((err) => {
  console.error('Fatal error running suite:', err);
  process.exitCode = 1;
});
