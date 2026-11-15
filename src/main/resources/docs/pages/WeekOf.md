# Week Of

## Overview
Returns the reporting week a date falls in — its first day, last day and display label — using your
configured week start. An optional offset shifts the answer by whole weeks, so "the week before
this one" needs no date arithmetic in the workflow.

## Request Details
- **Area**: Reporting Period
- **Type**: QUERY_SYSTEM
- **External API**: none — local computation

## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Date | Text | Yes | The date to locate, ISO-8601 `YYYY-MM-DD`. A full timestamp is accepted and read in the configured time zone. |
| Offset Weeks | Number | No | Whole weeks to shift by: `-1` is the previous week, `1` the next. Defaults to `0`. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Week Window | Entity(Week Window) | The full result — see [Entities](pages/entities.md). |
| Week Start | Text | First day of the week, `YYYY-MM-DD`. |
| Week End | Text | Last day of the week, `YYYY-MM-DD` — always six days after the start. |
| Week Label | Text | Display label, for example `Week of Nov 3, 2025`. |
| Success | Boolean | `false` only when an input could not be read. |
| Message | Text | The label and window in plain language. |

## Example
**Input**: Date = `2025-11-05`, Offset Weeks = *(empty)*, Week Starts On = `Monday`

**Output**: Week Start = `2025-11-03`, Week End = `2025-11-09`, Week Label = `Week of Nov 3, 2025`

With Week Starts On = `Sunday`, the same input returns Week Start `2025-11-02` and Week End
`2025-11-08`.

## Business Rules
1. **The week start setting decides the boundary.** Nothing else does — not the ISO week, not the
   locale of the appliance.
2. **Year boundaries are handled.** `2025-12-31` in Monday mode returns `Week of Dec 29, 2025`,
   ending `2026-01-04`. The label always carries the **start** date's year.
3. **The label has no leading zero** on the day: `Week of Nov 3, 2025`, never `Nov 03`.
4. **A date that is already the week start is returned unchanged.**

## Error Handling
- **Unparseable Date** — `Success = false`, Message `Date must be an ISO-8601 date such as 2025-11-03, but was '...'.`
- **Fractional Offset Weeks** — `Success = false`, Message `Offset Weeks must be a whole number of weeks, but was 1.5.`
- **Offset beyond ±520 weeks** — rejected as a likely mistake, naming the limit.

## Related Requests
- [Detect Stated Period](pages/DetectStatedPeriod.md) — the week a message states for itself.
- [Get Report Status](pages/GetReportStatus.md) — takes a Week Start like the one returned here.
