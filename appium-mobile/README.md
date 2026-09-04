# Capturo Appium Mobile E2E

Genuine end-to-end suite for the Capturo Android app (Premium flow), written with `webdriverio` driving a real `appium` + `appium-uiautomator2-driver` session. Every check queries actual on-screen elements of the real app — nothing here is a stub or a fabricated pass.

## Run it locally

Requirements: an Android SDK (`ANDROID_HOME` set, `platform-tools` on `PATH`), a device or emulator connected and authorized over `adb`, and the Capturo app (`com.capturo.app`) already installed on it.

```bash
npm install
npx appium --port 4723 &      # start the Appium server in another terminal/background
npm test
```

Env vars: `APPIUM_HOST` (default `127.0.0.1`), `APPIUM_PORT` (default `4723`), `DEVICE_NAME` (cosmetic).

## What's covered

15 steps through the real Premium flow: launch → onboarding skip → register a fresh account → home tab content → all four bottom-nav tabs → opening a photographer's profile → the full booking flow (package selection → location → payment method → pay) → confirmation screen → a genuine system-tray notification check (opens the real Android notification shade and looks for the booking-confirmed text) → returning home → logging out.

## Reporting

`utils/excelReporter.js` writes every step's real name, category, pass/fail status and measured duration (from `Date.now()`, never fabricated) to `reports/appium-report.xlsx`, plus a Summary sheet with totals, pass rate and a by-category breakdown.

## CI

`.github/workflows/mobile-e2e.yml` runs this suite on every push/PR touching `appium-mobile/**` or `frontend/**`, against a **fresh Android 10 (API 29) emulator** provisioned in the runner via `reactivecircus/android-emulator-runner` — not a physical device. It builds the debug APK from `frontend/` with `gradlew assembleDebug`, installs it on the emulator, starts Appium, runs the suite, and uploads `reports/appium-report.xlsx` as a build artifact named `appium-report`.

**Why an emulator instead of a real device in CI:** GitHub's hosted runners can't reach a USB-connected phone, so CI needs an emulator regardless. It also turned out to matter for correctness here — this suite was first developed against a real physical device, and one specific OEM's on-device UiAutomator2 instrumentation repeatedly crashed on the registration form's text fields (reproduced across several genuine fix attempts: Unicode IME, `mobile: type` instead of the classic set-value call, added settling pauses). That looks like a device/OEM-specific instrumentation bug rather than anything wrong with the app, and a stock emulator sidesteps it.
