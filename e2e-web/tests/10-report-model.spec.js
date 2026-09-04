const assert = require('assert');
const { summarize } = require('../utils/reportModel');

describe('Report model unit tests', function () {
  it('calculates totals, durations, and pass rate from test results', function () {
    assert.deepStrictEqual(summarize([
      { status: 'Pass', duration: 12 },
      { status: 'Fail', duration: 8 },
      { status: 'Pass', duration: 10 }
    ], { pending: 1 }), {
      total: 3, passed: 2, failed: 1, pending: 1, duration: 30, passRate: 66.67
    });
  });
});