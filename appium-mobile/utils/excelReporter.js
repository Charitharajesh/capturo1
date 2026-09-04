const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');

/**
 * Writes a real Appium test-run result set to an .xlsx workbook.
 * Every row comes from an actual executed step (see tests/runTests.js) --
 * durations are the real elapsed ms for that step, never fabricated.
 */
async function writeExcelReport(results, outPath) {
  const wb = new ExcelJS.Workbook();
  wb.creator = 'Capturo Appium E2E Suite';
  wb.created = new Date();

  const sheet = wb.addWorksheet('Appium Test Report');
  sheet.columns = [
    { header: '#', key: 'idx', width: 5 },
    { header: 'Category', key: 'category', width: 20 },
    { header: 'Test Name', key: 'name', width: 55 },
    { header: 'Status', key: 'status', width: 10 },
    { header: 'Duration (ms)', key: 'durationMs', width: 14 },
    { header: 'Error', key: 'error', width: 60 },
  ];
  sheet.getRow(1).font = { bold: true };

  results.forEach((r, i) => {
    const row = sheet.addRow({
      idx: i + 1,
      category: r.category,
      name: r.name,
      status: r.status,
      durationMs: r.durationMs,
      error: r.error || '',
    });
    row.getCell('status').font = {
      color: { argb: r.status === 'Pass' ? 'FF2E7D32' : 'FFC62828' },
      bold: true,
    };
  });

  const summary = wb.addWorksheet('Summary');
  const passed = results.filter((r) => r.status === 'Pass').length;
  const failed = results.length - passed;
  const totalMs = results.reduce((s, r) => s + r.durationMs, 0);
  const byCategory = {};
  results.forEach((r) => {
    byCategory[r.category] = byCategory[r.category] || { total: 0, passed: 0 };
    byCategory[r.category].total += 1;
    if (r.status === 'Pass') byCategory[r.category].passed += 1;
  });

  summary.columns = [
    { header: 'Metric', key: 'k', width: 28 },
    { header: 'Value', key: 'v', width: 30 },
  ];
  summary.getRow(1).font = { bold: true };
  summary.addRows([
    { k: 'Total tests', v: results.length },
    { k: 'Passed', v: passed },
    { k: 'Failed', v: failed },
    { k: 'Pass rate', v: `${results.length ? ((passed / results.length) * 100).toFixed(1) : 0}%` },
    { k: 'Total duration (ms)', v: totalMs },
    { k: 'Run date', v: new Date().toISOString() },
    { k: '', v: '' },
    { k: 'By category', v: 'total / passed' },
    ...Object.entries(byCategory).map(([cat, v]) => ({ k: cat, v: `${v.total} / ${v.passed}` })),
  ]);

  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  await wb.xlsx.writeFile(outPath);
}

module.exports = { writeExcelReport };
