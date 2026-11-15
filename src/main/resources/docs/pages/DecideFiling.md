# Decide Filing

## Overview
The decision point. It takes the three signals the other requests produced, plus the model's reading
of the message, and returns exactly one outcome — **File as Report**, **File under Company**,
**Hold for Review** or **Set Aside** — with a written reason every time.

## Request Details
- **Area**: Email Triage
- **Type**: QUERY_SYSTEM
- **External API**: none — local computation

## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Sender Match JSON | Object | Yes | The [Match Sender](pages/MatchSender.md) result. |
| Period JSON | Object | Yes | The [Detect Stated Period](pages/DetectStatedPeriod.md) result. |
| Short Reply JSON | Object | Yes | The [Is Short Reply](pages/IsShortReply.md) result. |
| Model Reading JSON | Object | Yes | The model's own reading of the message. |

The field names match the outputs of the requests they come from, so an upstream result can be
passed straight through.

**Sender Match JSON**: `Match Type` (Text), `Reporter Id` (Text), `Company Id` (Text),
`Candidate Count` (Number), `Is Reporting Mailbox` (Boolean).

**Period JSON**: `Period Found` (Boolean), `Week Start` (Text, `YYYY-MM-DD`), `Week End` (Text),
`Evidence Text` (Text), `Confidence` (Text).

**Short Reply JSON**: `Is Short Reply` (Boolean), `Is Reply To Our Mail` (Boolean), `Reason` (Text).

**Model Reading JSON**: `Kind` (Text — `own report`, `company update` or `not a report`), `Week`
(Text, any date inside the week), `Confidence` (Text — `High` or `Low`).

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Filing Decision | Entity(Filing Decision) | The full result — see [Entities](pages/entities.md). |
| Decision | Text | `File as Report`, `File under Company`, `Hold for Review` or `Set Aside`. |
| Reporter Id | Text | The reporter the message was attributed to; empty when none was resolved. |
| Company Id | Text | The company it would be filed under, when relevant. |
| Week Start | Text | The reporting week, `YYYY-MM-DD`. Empty when no week could be established. |
| Reason | Paragraph | Why this decision was reached. Always populated. |
| Success | Boolean | `false` only when an input could not be read. |
| Message | Text | The decision and its reason. |

## The decision order

The rules are checked in this exact order, and the first one that applies wins:

1. **Reply to our own mail** → `Set Aside`.
2. **Short reply** → `Set Aside`. (Checked before the sender rules, so a short reply from a stranger
   is set aside, not held.)
3. **Sent from the reporting mailbox** → `Set Aside`.
4. **Reading is `not a report`** → `Set Aside`.
5. **Sender is `Ambiguous`** → `Hold for Review`, with the candidate count in the reason.
6. **Sender is not `Exact` or `Alias`** → `Hold for Review`. An unknown sender is never adopted.
7. **Stated week ≠ the model's week** → `Hold for Review`, with **both dates** in the reason.
8. **No week anywhere** → `Hold for Review`.
9. **Stated period is Low confidence** → `Hold for Review`.
10. **No stated period at all** → `Hold for Review`. The reading alone is not enough.
11. **Reading is not High confidence** → `Hold for Review`.
12. **Kind is `company update`** → `File under Company` when the reporter has a Company Id;
    `Hold for Review` when they do not.
13. **Kind is `own report`** → `File as Report`.
14. **Any other kind** → `Hold for Review`, naming the unrecognised kind.

Weeks are compared **after** being normalised onto the configured week start, so a model that names
the Thursday of the same week does not count as a disagreement. Kind and confidence comparisons are
case-insensitive.

## Example
**Input**: Sender Match JSON = `{Match Type: Exact, Reporter Id: r-1, Company Id: c-1}`,
Period JSON = `{Period Found: true, Week Start: 2025-11-03, Confidence: High}`,
Short Reply JSON = `{Is Short Reply: false, Is Reply To Our Mail: false}`,
Model Reading JSON = `{Kind: own report, Week: 2025-11-03, Confidence: High}`

**Output**: Decision = `File as Report`, Reporter Id = `r-1`, Week Start = `2025-11-03`,
Reason = `Own report from reporter r-1 for the week of 2025-11-03; sender, kind and week all agree with high confidence.`

**Disagreement**: with Model Reading Week = `2025-10-27` the same inputs return
Decision = `Hold for Review`, Reason = `The week stated in the message (2025-11-03) and the week in the reading (2025-10-27) disagree.`

## Error Handling
- **A missing composite input** — `Success = false`, Message `Model Reading JSON is required. Please provide a value for Model Reading JSON.`
- **An unparseable date inside a composite** — `Success = false` naming which one, for example
  `Model Reading JSON Week must be an ISO-8601 date such as 2025-11-03, but was 'last Tuesday'.`
- **Invalid configuration** — `Success = false` naming the setting to fix.

## Related Requests
- [Match Sender](pages/MatchSender.md), [Detect Stated Period](pages/DetectStatedPeriod.md),
  [Is Short Reply](pages/IsShortReply.md) — the three signals this request consumes.
