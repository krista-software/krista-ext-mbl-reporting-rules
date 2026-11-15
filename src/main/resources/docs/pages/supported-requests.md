# Supported Requests

MBL Reporting Rules exposes **8 catalog requests** across **5 areas**. Every one is read-only
(`QUERY_SYSTEM`), performs no network call, and returns **Success** and **Message** alongside its own
outputs. Each links to its detailed page.

## Setup
| Request | Type | Description |
|---|---|---|
| [Test Connection](pages/TestConnection.md) | QUERY_SYSTEM | Validates the saved configuration — time zone, week start, due day and time. |
| [Health Check](pages/HealthCheck.md) | QUERY_SYSTEM | Reports configuration health and JVM heap usage. |

## Sender Matching
| Request | Type | Description |
|---|---|---|
| [Match Sender](pages/MatchSender.md) | QUERY_SYSTEM | Resolves an email sender against the reporter directory: Exact, Alias, None or Ambiguous. |

## Reporting Period
| Request | Type | Description |
|---|---|---|
| [Detect Stated Period](pages/DetectStatedPeriod.md) | QUERY_SYSTEM | Finds the week a message states for itself, at High or Low confidence. |
| [Week Of](pages/WeekOf.md) | QUERY_SYSTEM | Returns the reporting week a date falls in, optionally offset by whole weeks. |

## Email Triage
| Request | Type | Description |
|---|---|---|
| [Is Short Reply](pages/IsShortReply.md) | QUERY_SYSTEM | Detects throwaway acknowledgements and replies to our own mail. |
| [Decide Filing](pages/DecideFiling.md) | QUERY_SYSTEM | Combines the signals into one outcome: File as Report, File under Company, Hold for Review or Set Aside. |

## Report Status
| Request | Type | Description |
|---|---|---|
| [Get Report Status](pages/GetReportStatus.md) | QUERY_SYSTEM | Reports a reporter's standing for a week: Received, Late, Missing, Excused or Pending. |

## A typical sequence

1. [Match Sender](pages/MatchSender.md) — who sent it?
2. [Detect Stated Period](pages/DetectStatedPeriod.md) — which week does it claim?
3. [Is Short Reply](pages/IsShortReply.md) — is it even a report?
4. Ask your model to read the message (kind, week, confidence).
5. [Decide Filing](pages/DecideFiling.md) — what happens to it?
6. [Get Report Status](pages/GetReportStatus.md) — afterwards, who is still outstanding?
