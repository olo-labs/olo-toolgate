# Install and Enrollment

## Purpose

Normal install uses `olo-toolgate-client install --server <URL>`, browser/device authentication, device key generation and registration.

In the console, open **Devices → Enroll device**. All unexpired requests in your
tenant appear under **Devices waiting for approval**, refreshed every five seconds.
Select **Review**, compare the code and fingerprint with the client, and choose
**Allow connection until** in your local time before selecting **Enroll device**.
You can also deny a request. Pending requests expire after ten minutes.

Connection approvals default to 24 hours. The server stores the chosen deadline
and rejects device access when it expires; certificate renewal cannot extend it.
The Clients view shows **Approval expired** after the deadline. Devices enrolled
before deadline support retain their existing approval behavior. API approval
callers that omit `connectionExpiresAtUnixMs` receive a 24-hour approval.
