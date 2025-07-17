# Security Policy

## Supported Versions

We currently support security updates for the following versions of the HTLM5 Android App:

| Version | Supported          |
|---------|--------------------|
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take security issues seriously and appreciate your efforts to responsibly disclose your findings. We'll do our best to address any security issues as quickly as possible.

### How to Report a Security Vulnerability

Please report security vulnerabilities by emailing our security team at [security@yourdomain.com]. Please include the following details in your report:

- Description of the vulnerability
- Steps to reproduce the issue
- Any potential impact
- Any mitigations if known
- Your name and affiliation (if any) for credit

We will acknowledge receipt of your report within 48 hours and provide a more detailed response within 72 hours indicating the next steps in handling your report.

### Our Security Process

1. Upon receiving a security report, we will:
   - Acknowledge the report and assign it to a lead handler
   - Confirm the issue and determine the affected versions
   - Audit code to find any potential similar problems
   - Prepare fixes for all releases under active maintenance

2. Once a fix is ready, we will:
   - Release new versions with the security fix
   - Publicly announce the vulnerability and the fix in our release notes
   - Credit the reporter (unless they wish to remain anonymous)

### Security Best Practices

To help keep your app secure, we recommend:

1. Always keep your app updated to the latest version
2. Use secure communication (HTTPS) for all network requests
3. Store sensitive data securely using Android's Keystore system
4. Follow the principle of least privilege when requesting permissions
5. Keep your development environment and dependencies up to date

### Security Updates

We regularly update our dependencies to include the latest security patches. These updates are included in our regular release cycle.

### Bug Bounty

We currently do not have a formal bug bounty program, but we are happy to recognize and thank security researchers who help us keep our users safe.
