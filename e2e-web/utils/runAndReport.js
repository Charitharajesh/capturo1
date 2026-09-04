/**
 * Runs Mocha, then ALWAYS generates the Excel report and CI step summary
 * afterwards — regardless of whether any test failed — then exits with
 * Mocha's real exit code.
 *
 * (npm's `posttest` lifecycle hook only runs after a *successful* `test`
 * script, so chaining report generation that way would silently skip the
 * report whenever a test fails — exactly when you most want to see it.)
 */
const { spawnSync } = require('child_process');
const path = require('path');

/**
 * Selenium 4's Selenium Manager resolves (and, on a cold machine/CI runner,
 * downloads) a matching chromedriver binary the first time a driver is ever
 * built in this process/host. That one-time cost was intermittently blowing
 * past the very first spec's 30s mocha timeout, failing "Landing page"
 * while every later spec (which reuses the now-cached driver) passed in
 * well under a second. Paying that cost here, before the timed mocha run
 * starts, makes every spec's timeout budget reflect actual test time.
 */
async function warmUpDriver() {
  console.log('Warming up ChromeDriver (Selenium Manager resolution)...');
  try {
    const { buildDriver } = require('./driver');
    const driver = await buildDriver();
    await driver.quit();
  } catch (err) {
    console.error('ChromeDriver warm-up failed (continuing anyway):', err.message);
  }
}

async function report() {
  console.log('\nGenerating Excel report and CI step summary...');
  await require('./excelReporter').main().catch((err) => {
    console.error('Failed to generate Excel report:', err.message);
  });
  try {
    require('./writeStepSummary').main();
  } catch (err) {
    console.error('Failed to write step summary:', err.message);
  }
}

async function main() {
  await warmUpDriver();

  const mocha = spawnSync(
    process.execPath,
    [require.resolve('mocha/bin/_mocha'), 'tests/**/*.spec.js', '--timeout', '30000'],
    { stdio: 'inherit', shell: false }
  );

  await report();
  process.exit(mocha.status === null ? 1 : mocha.status);
}

main();
