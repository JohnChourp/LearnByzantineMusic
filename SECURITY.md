## Security Policy
The app's privacy policy is in [PRIVACY.md](PRIVACY.md): the app collects, transmits and shares no personal data.

## Supported Versions

| Version | Supported |
| ------- | --------- |
| [Latest GitHub Release](https://github.com/JohnChourp/LearnByzantineMusic/releases/latest) | :white_check_mark: |
| Older releases | :x: |

## Reporting a Vulnerability

If you discover a vulnerability in our software, please report it privately and do not open a public issue:

Preferred: Use GitHub private vulnerability reporting via the "Report a vulnerability" button on the repository's Security tab (https://github.com/JohnChourp/LearnByzantineMusic/security/advisories/new).
Email: Alternatively, send a detailed description of the vulnerability to jonhiedarn@gmail.com.
Subject Line: Use the subject line "Vulnerability Report - LearnByzantineMusic".
Information to Include: Provide as much detail as possible, including:
A description of the issue
Steps to reproduce the vulnerability
Potential impact of the vulnerability
Any supporting materials (e.g., screenshots, code samples, logs)

## Response Time
We aim to respond to vulnerability reports within 5 business days. You will receive an update on the status of your report at least every 14 days until the issue is resolved.

## Handling Accepted and Declined Reports
Accepted Vulnerabilities: If the vulnerability is confirmed, we will work with you to resolve the issue. You may be asked for further details or assistance as we address the vulnerability.
Declined Vulnerabilities: If the report is declined, we will provide a reason for the decision. This may occur if the issue is not reproducible or does not meet the threshold of a security vulnerability.
We appreciate your effort in helping to keep our systems and users secure!

## Project-Specific Repository Security Rules (LearnByzantineMusic)

This repository is public. The following controls are mandatory for this project:

1. Never commit secrets or key material in source control.
2. Keep sensitive values only in GitHub Actions Secrets / runtime environment variables.
3. Run `scripts/check-no-secrets.sh` before every release.
4. Initialize Android release signing through `scripts/setup-release-signing.sh` so credentials stay outside source control.
5. Enforce CI security checks on every push and pull request via `.github/workflows/security-guard.yml`.
6. Block Android release publishing if signing secrets are missing or if only unsigned APK output exists.

Required GitHub Secrets for Android release signing:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
