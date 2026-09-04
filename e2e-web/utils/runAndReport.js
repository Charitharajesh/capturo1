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

const mocha = spawnSync(
  process.execPath,
  [require.resolve('mocha/bin/_mocha'), 'tests/**/*.spec.js', '--timeout', '30000'],
  { stdio: 'inherit', shell: false }
);

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

report().then(() => {
  process.exit(mocha.status === null ? 1 : mocha.status);
});
