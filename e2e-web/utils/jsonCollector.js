/**
 * Custom Mocha reporter.
 *
 * Extends the built-in "spec" reporter (so `npm test` still prints the
 * normal console tree), and additionally records the REAL result of every
 * test — name, category (the describe block title), status and the
 * measured `test.duration` that Mocha itself computes — to
 * reports/results.json. That file is read by excelReporter.js (run via the
 * "posttest" npm script) to build the .xlsx report. No timings or results
 * here are fabricated; everything comes straight off Mocha's runner events.
 */
const path = require('path');
const fs = require('fs');
const Mocha = require('mocha');
const Spec = Mocha.reporters.Spec;

class JsonCollector extends Spec {
  constructor(runner, options) {
    super(runner, options);
    this._results = [];

    const record = (test, status, err) => {
      this._results.push({
        name: test.title,
        fullName: test.fullTitle(),
        category: (test.parent && test.parent.title) || 'Uncategorized',
        status,
        duration: typeof test.duration === 'number' ? test.duration : 0,
        error: err ? String(err.message || err) : null
      });
    };

    runner.on('pass', (test) => record(test, 'Pass'));
    runner.on('fail', (test, err) => record(test, 'Fail', err));

    runner.on('end', () => {
      const stats = runner.stats || {};
      const outDir = path.join(__dirname, '..', 'reports');
      if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

      const payload = {
        generatedAt: new Date().toISOString(),
        stats: {
          suites: stats.suites || 0,
          tests: stats.tests || 0,
          passes: stats.passes || 0,
          failures: stats.failures || 0,
          pending: stats.pending || 0,
          start: stats.start,
          end: stats.end,
          duration: stats.duration || 0
        },
        results: this._results
      };

      // Synchronous write so the file is guaranteed complete before the
      // mocha process exits and the "posttest" script runs.
      fs.writeFileSync(path.join(outDir, 'results.json'), JSON.stringify(payload, null, 2));
    });
  }
}

module.exports = JsonCollector;
