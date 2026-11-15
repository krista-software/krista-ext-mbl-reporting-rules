# Entities

The data structures these requests return. Every date is ISO-8601 text: calendar dates as
`YYYY-MM-DD`, timestamps in full ISO-8601 with an offset.

## Sender Match
The result of resolving an email sender against the reporter directory. Primary key: **From Address**.
Returned by [Match Sender](pages/MatchSender.md).

| Field | Type | Description |
|---|---|---|
| From Address | Text | The sender address that was resolved, lower-cased. |
| Match Type | Text | `Exact`, `Alias`, `None` or `Ambiguous`. |
| Reporter Id | Text | The matched reporter; empty unless Exact or Alias. |
| Reporter Name | Text | The matched reporter's display name. |
| Company Id | Text | The matched reporter's company; empty when they have none. |
| Candidate Count | Number | How many directory rows the address could refer to. |
| Is Reporting Mailbox | Boolean | `true` when the sender is the shared reporting mailbox itself. |
| Candidates | List of objects | Every candidate: `Reporter Id`, `Name`, `Company Id`, `Matched On` (`Email`, `Alias` or `Inactive`). |

## Stated Period
The week a message states for itself, normalised onto the configured week start. Primary key:
**Week Start**. Returned by [Detect Stated Period](pages/DetectStatedPeriod.md).

| Field | Type | Description |
|---|---|---|
| Week Start | Text | First day of the stated week, `YYYY-MM-DD`. Empty when none was found. |
| Period Found | Boolean | `true` when the message states a week or date range. |
| Week End | Text | Last day of the stated week, `YYYY-MM-DD`. |
| Evidence Text | Paragraph | The exact wording the period was read from, prefixed `Subject: ` or `Body: `. |
| Confidence | Text | `High` for an explicit date or range, `Low` for a relative phrase or week number. |

## Short Reply Assessment
Whether a message is a throwaway acknowledgement and whether it replies to our own mail. Primary key:
**Subject**. Returned by [Is Short Reply](pages/IsShortReply.md).

| Field | Type | Description |
|---|---|---|
| Subject | Text | The subject of the message that was assessed. |
| Is Short Reply | Boolean | `true` when the sender's own words are an acknowledgement or below the word threshold. |
| Is Reply To Our Mail | Boolean | `true` when In Reply To or References names one of our message ids. |
| Own Word Count | Number | Words left after quoted text, signature and disclaimer are stripped. |
| Reason | Paragraph | Which rule fired, in plain language. |

## Filing Decision
What to do with one message. Primary key: **Reporter Id**. Returned by
[Decide Filing](pages/DecideFiling.md).

| Field | Type | Description |
|---|---|---|
| Reporter Id | Text | The reporter the message was attributed to; empty when none was resolved. |
| Decision | Text | `File as Report`, `File under Company`, `Hold for Review` or `Set Aside`. |
| Company Id | Text | The company it would be filed under, when relevant. |
| Week Start | Text | First day of the reporting week, `YYYY-MM-DD`. |
| Reason | Paragraph | Why this decision was reached. Always populated — for a week disagreement it names both dates. |

## Week Window
One reporting week. Primary key: **Week Start**. Returned by [Week Of](pages/WeekOf.md).

| Field | Type | Description |
|---|---|---|
| Week Start | Text | First day of the week, `YYYY-MM-DD`. |
| Week End | Text | Last day of the week, `YYYY-MM-DD` — always six days later. |
| Week Label | Text | Display label, for example `Week of Nov 3, 2025`. |

## Report Status
A reporter's standing for one week. Primary key: **Reporter Id**. Returned by
[Get Report Status](pages/GetReportStatus.md).

| Field | Type | Description |
|---|---|---|
| Reporter Id | Text | The reporter this status is for. |
| Week Start | Text | First day of the week in question, `YYYY-MM-DD`. |
| Status | Text | `Received`, `Late`, `Missing`, `Excused` or `Pending`. |
| Received At | Text | When the report was filed, full ISO-8601; empty when nothing was filed. |
| Due Moment | Text | The instant the report was due, full ISO-8601 in the configured time zone. |

## A note on storage

None of these entities is stored. They are computed from the inputs you pass and returned; the
extension keeps no database and no cache, so the same inputs always produce the same entity.
