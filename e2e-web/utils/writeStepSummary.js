/**
 * Renders the real results from reports/results.json as a markdown table
 * and appends it to the GitHub Actions job summary, so the pass/fail status
 * of every test case is visible directly on the run page — not just inside
 * the downloadable Excel artifact.
 */
const fs = require('fs');
const path = require('path');

function main() {
  const resultsPath = path.join(__dirname, '..', 'reports', 'results.json');
  const summaryFile = process.env.GITHUB_STEP_SUMMARY;

  if (!fs.existsSync(resultsPath)) {
    console.log('No results.json found — skipping step summary.');
    return;
  }

  const data = JSON.parse(fs.readFileSync(resultsPath, 'utf8'));
  const { stats, results } = data;

  const lines = [];
  lines.push('### 🌐 Web E2E (Selenium) — test case results');
  lines.push('');
  lines.push(`**${stats.passes}/${stats.tests} passed** (${stats.failures} failed) in ${(stats.duration / 1000).toFixed(1)}s — ${stats.tests ? ((stats.passes / stats.tests) * 100).toFixed(1) : 0}% pass rate`);
  lines.push('');
  lines.push('| Status | Category | Test | Duration |');
  lines.push('|---|---|---|---|');
  results.forEach((r) => {
    const icon = r.status === 'Pass' ? '✅' : '❌';
    lines.push(`| ${icon} | ${r.category} | ${r.name} | ${r.duration}ms |`);
  });
  lines.push('');
  lines.push('Full Excel report: see the `selenium-report` build artifact on this run.');
  lines.push('');

  const md = lines.join('\n');

  if (summaryFile) {
    fs.appendFileSync(summaryFile, md + '\n');
    console.log('Wrote test case status table to GITHUB_STEP_SUMMARY.');
  } else {
    console.log(md);
  }
}

module.exports = { main };

if (require.main === module) {
  main();
}
