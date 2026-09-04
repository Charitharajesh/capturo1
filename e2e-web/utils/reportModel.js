function summarize(results, stats = {}) {
  const passed = results.filter((result) => result.status === 'Pass').length;
  const failed = results.filter((result) => result.status === 'Fail').length;
  const total = results.length;
  return {
    total,
    passed,
    failed,
    pending: stats.pending || 0,
    duration: results.reduce((sum, result) => sum + (result.duration || 0), 0),
    passRate: total ? Math.round((passed / total) * 10000) / 100 : 0
  };
}

module.exports = { summarize };