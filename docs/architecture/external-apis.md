# External APIs

The system has minimal external dependencies for MVP:

## Gmail SMTP (Primary External Integration)

- **Purpose:** Email delivery for task notifications and workflow updates
- **Documentation:** https://support.google.com/mail/answer/7126229
- **Base URL:** smtp.gmail.com:587 (TLS/STARTTLS)
- **Authentication:** Application-specific password (environment variable: SMTP_PASSWORD)
- **Rate Limits:** 500 emails per day for free Gmail accounts

**Key Endpoints Used:**
- SMTP connection to smtp.gmail.com:587
- TLS/STARTTLS for secure connection
- SMTP AUTH for authentication

**Integration Notes:**
- Fully documented in NotificationService component
- Asynchronous delivery with retry logic (max 3 retries, exponential backoff)
- HTML emails rendered via Thymeleaf templates
- Sufficient for MVP volume (<50 workflows/month = ~150 emails/month)
- Gmail rate limit monitoring required (500 emails/day)

## No Additional External APIs for MVP

The PRD explicitly defers these integrations to post-MVP:
- ❌ Active Directory integration
- ❌ SSO/SAML providers
- ❌ Third-party notification services
- ❌ External data sources or webhooks
