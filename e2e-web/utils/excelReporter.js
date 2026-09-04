/**
 * Reads reports/results.json (written by utils/jsonCollector.js during the
 * real `npm test` run) and produces reports/selenium-report.xlsx with:
 *   - "Test Results": Test Name, Category, Status, Duration (ms)
 *   - "Summary": total tests, passed, failed, total duration, pass rate %
 *
 * Every value comes directly from the Mocha run that just happened — no
 * random/fabricated durations or counts.
 */
const path = require('path');
const fs = require('fs');
const ExcelJS = require('exceljs');

async function main() {
  const reportsDir = path.join(__dirname, '..', 'reports');
  const resultsPath = path.join(reportsDir, 'results.json');

  if (!fs.existsSync(resultsPath)) {
    throw new Error(`No results found at ${resultsPath}. Run "npm test" first.`);
  }

  const payload = JSON.parse(fs.readFileSync(resultsPath, 'utf8'));
  const results = payload.results || [];
  const stats = payload.stats || {};

  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'capturo-web-e2e';
  workbook.created = new Date();

  /* -------- Sheet 1: Test Results -------- */
  const sheet = workbook.addWorksheet('Test Results');
  sheet.columns = [
    { header: 'Test Name', key: 'name', width: 60 },
    { header: 'Category', key: 'category', width: 32 },
    { header: 'Status', key: 'status', width: 12 },
    { header: 'Duration (ms)', key: 'duration', width: 16 }
  ];
  sheet.getRow(1).font = { bold: true };

  results.forEach((r) => {
    const row = sheet.addRow({
      name: r.name,
      category: r.category,
      status: r.status,
      duration: r.duration
    });
    const statusCell = row.getCell('status');
    statusCell.font = { color: { argb: r.status === 'Pass' ? 'FF2E7D32' : 'FFC62828' }, bold: true };
  });
  sheet.autoFilter = { from: 'A1', to: 'D1' };

  /* -------- Sheet 2: Summary -------- */
  const summarySheet = workbook.addWorksheet('Summary');
  const total = results.length;
  const passed = results.filter((r) => r.status === 'Pass').length;
  const failed = results.filter((r) => r.status === 'Fail').length;
  const totalDurationFromTests = results.reduce((sum, r) => sum + (r.duration || 0), 0);
  const passRate = total > 0 ? ((passed / total) * 100) : 0;

  summarySheet.columns = [
    { header: 'Metric', key: 'metric', width: 34 },
    { header: 'Value', key: 'value', width: 20 }
  ];
  summarySheet.getRow(1).font = { bold: true };

  const summaryRows = [
    ['Total Tests', total],
    ['Passed', passed],
    ['Failed', failed],
    ['Pending/Skipped', stats.pending || 0],
    ['Sum of Individual Test Durations (ms)', totalDurationFromTests],
    ['Total Suite Duration (ms) [Mocha runner.stats]', stats.duration || 0],
    ['Pass Rate (%)', Math.round(passRate * 100) / 100],
    ['Run Started (UTC)', stats.start || ''],
    ['Run Ended (UTC)', stats.end || ''],
    ['Report Generated (UTC)', payload.generatedAt || '']
  ];
  summaryRows.forEach(([metric, value]) => summarySheet.addRow({ metric, value }));

  if (!fs.existsSync(reportsDir)) fs.mkdirSync(reportsDir, { recursive: true });
  const outPath = path.join(reportsDir, 'selenium-report.xlsx');
  await workbook.xlsx.writeFile(outPath);
  console.log(`Excel report written to ${outPath}`);
  console.log(`Total: ${total} | Passed: ${passed} | Failed: ${failed} | Pass rate: ${passRate.toFixed(2)}%`);
}

module.exports = { main };

if (require.main === module) {
  main().catch((err) => {
    console.error('Failed to generate Excel report:', err);
    process.exit(1);
  });
}
