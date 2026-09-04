# Capturo Web E2E

Selenium (headless Chrome) end-to-end suite for the Capturo website, written with `selenium-webdriver` + `mocha`.

## Run it

```bash
npm install
npm test
```

- Expects the site to be reachable at `http://localhost:5173`. Locally that's your own dev copy of the site; in CI it's served straight from `site-under-test/` in this folder (a snapshot of the site kept here purely so the suite has something to run against in a clean checkout — the real source of truth for the site lives outside this repo). Override the target with the `BASE_URL` env var, e.g.:

  ```bash
  BASE_URL=http://localhost:8080 npm test
  ```

- Uses headless Chrome via Selenium 4's built-in Selenium Manager, which detects whatever Chrome/Chromium is installed and fetches a matching driver automatically — no pinned driver version to fall out of sync, but a real Chrome/Chromium install is required on the machine (present by default on GitHub's `ubuntu-latest` runners).

## CI

`.github/workflows/web-e2e.yml` runs this suite on every push and pull request. Set repository variables `WEB_APP_URL` to the public GitHub Pages URL and `BACKEND_URL` to the public Render API URL. The runner passes the API URL to the web app as its `?api=` override. The workflow uploads `reports/selenium-report.xlsx` as a build artifact named `selenium-report`.
- `npm test` runs Mocha over `tests/**/*.spec.js`, then automatically runs the `posttest` script (`npm run report`), which converts the real results captured during the run into an Excel report at `reports/selenium-report.xlsx`.

## What's covered

- `tests/01-landing.spec.js` — landing page title/hero/nav
- `tests/02-auth.spec.js` — sign-up flow into the app shell
- `tests/03-navigation.spec.js` — Home / Discover / Try / Bookings tabs
- `tests/04-booking.spec.js` — photographer detail page, package selection, booking + confirmation
- `tests/05-notifications.spec.js` — notification bell, unread badge, mark-all-read
- `tests/06-swipe.spec.js` — Try tab swipe/like favouriting toast
- `tests/07-signout.spec.js` — profile menu logout back to the marketing site
- `tests/08-responsive.spec.js` — mobile bottom nav vs desktop nav at the site's real 860px breakpoint

## Reporting

- `utils/jsonCollector.js` is a custom Mocha reporter (wired in via `.mocharc.json`) that prints the normal spec console output and also writes every test's real name, category (describe block), pass/fail status and Mocha-measured `duration` to `reports/results.json`.
- `utils/excelReporter.js` reads that JSON and writes `reports/selenium-report.xlsx` with:
  - **Test Results** sheet: Test Name, Category, Status, Duration (ms)
  - **Summary** sheet: total tests, passed, failed, total duration, pass rate %

Run just the report step again (without re-running the browser tests) via:

```bash
npm run report
```
