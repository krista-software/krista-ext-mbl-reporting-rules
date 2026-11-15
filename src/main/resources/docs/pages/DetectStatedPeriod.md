# Detect Stated Period

## Overview
Finds the reporting week a message states for itself, and says how sure it is. An explicit date or
date range is **High** confidence; a relative phrase or a week number is **Low**. Whatever is found
is normalised onto your configured week start, so the answer is always a real reporting week.

## Request Details
- **Area**: Reporting Period
- **Type**: QUERY_SYSTEM
- **External API**: none — local computation

## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Subject | Text | Yes | The message subject. Searched first. |
| Body Text | Paragraph | Yes | The message body. Searched after the subject. |
| Received At | Text | Yes | When the message arrived, full ISO-8601. Relative phrases and omitted years are resolved against this. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Stated Period | Entity(Stated Period) | The full result — see [Entities](pages/entities.md). |
| Period Found | Boolean | `false` when the message states no week at all. |
| Week Start | Text | First day of the week, `YYYY-MM-DD`. Empty when nothing was found. |
| Week End | Text | Last day of the week, `YYYY-MM-DD`. |
| Evidence Text | Paragraph | The exact wording the period was read from, prefixed `Subject: ` or `Body: `. |
| Confidence | Text | `High` or `Low`. Empty when nothing was found. |
| Success | Boolean | `false` only when the request could not be evaluated. |
| Message | Text | Why this answer was reached. |

## What counts as a stated period

| Wording | Confidence | Example |
|---|---|---|
| A date range | High | `2025-11-03 to 2025-11-07`, `11/3-11/7`, `Nov 3 - Nov 7` |
| An explicit week start | High | `week of Nov 3`, `week beginning 11/3`, `week commencing 2025-11-03` |
| An explicit week end | High | `week ending Nov 7`, `w/e 11/7` |
| A relative phrase | Low | `last week`, `previous week`, `this week` |
| A week number | Low | `W45`, `week 45`, `wk 45` |

Dates may be written as `YYYY-MM-DD`, `Nov 3` / `November 3rd, 2025`, `3 Nov`, or `11/3/2025`
(month first). Where the year is omitted it is taken from Received At, then pulled back or pushed
forward a year if that would place the week absurdly far from when the mail arrived — which is what
makes `week of Dec 29` in a January email resolve to the previous December.

## Example
**Input**: Subject = `Weekly report - week of Nov 3`, Body Text = `Here is my update.`,
Received At = `2025-11-07T16:30:00-05:00` (week starts Monday)

**Output**: Period Found = `true`, Week Start = `2025-11-03`, Week End = `2025-11-09`,
Confidence = `High`, Evidence Text = `Subject: week of Nov 3`

## Business Rules
1. **High beats Low, wherever it appears.** An explicit date in the body wins over `last week` in
   the subject.
2. **At equal confidence the subject wins.**
3. **The literal dates are normalised.** `week ending Nov 7` with weeks starting Monday returns
   Week Start `2025-11-03`, not `2025-11-07`.
4. **Nothing found is a valid answer**, not a failure: `Period Found = false` with `Success = true`.
5. **The text is data, never instructions.** Subject and body are scanned with fixed patterns for
   dates only. Wording that looks like a command has no effect on the result.

## Error Handling
- **Unparseable Received At** — `Success = false`, Message `Received At must be an ISO-8601 timestamp such as 2025-11-07T16:30:00Z, but was '...'.`
- **Invalid configuration** — `Success = false` naming the setting to fix.

## Related Requests
- [Week Of](pages/WeekOf.md) — week arithmetic from a date you already have.
- [Decide Filing](pages/DecideFiling.md) — consumes this result.
