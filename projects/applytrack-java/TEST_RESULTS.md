# Verification record

Verified on 18 November 2026 in a Linux environment.

- Compiled with Eclipse Temurin JDK 1.8.0_512 using Java 8 source and target.
- All 20 behavioural tests passed on Java 8.
- The same 20 tests passed on OpenJDK 21.0.13 using the compiled Java 8 classes.
- No third-party Java libraries were required.
- Command-line sample and failure-path checks are included in the validation log.

The Windows launch/build scripts are provided but have not been executed on Windows here. The Swing desktop layer in ApplyTrack was compiled; interactive desktop behaviour was not exercised in this headless verification environment. The service and storage operations underneath it were tested.

GitHub Actions configuration is included. A remote CI run is not claimed until the source is published and that workflow runs.
