# Get Report Status

## Overview
Says where one reporter stands for one week — `Excused`, `Received`, `Late`, `Missing` or `Pending`
— by comparing what is already on file against that week's due moment.

## Request Details
- **Area**: Report Status
- **Type**: QUERY_SYSTEM
- **External API**: none — local computation

## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Reporter Id | Text | Yes | The reporter to look up. |
| Week Start | Text | Yes | First day of the week in question, `YYYY-MM-DD`. |
| Filed Reports | List of objects | No | Reports already on file. Omit or leave empty when nothing is filed. |
| As Of | Text | No | The moment to evaluate against, full ISO-8601. Defaults to now — set it to make a result reproducible. |

Each row of **Filed Reports** contains:

| Field | Type | Description |
|---|---|---|
| Reporter Id | Text | Required. Rows for other reporters are ignored. |
| Week Start | Text | Required, `YYYY-MM-DD`. Rows for other weeks are ignored. |
| Received At | Text | Full ISO-8601 timestamp. Leave empty for a week that is on file but not yet filed. |
| Excused | Boolean | `true` marks the week excused. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Report Status | Entity(Report Status) | The full result, including the Due Moment used — see [Entities](pages/entities.md). |
| Status | Text | `Received`, `Late`, `Missing`, `Excused` or `Pending`. |
| Received At | Text | When it was filed, full ISO-8601; empty when nothing was filed. |
| Success | Boolean | `false` only when an input could not be read. |
| Message | Text | Why this status was reached, naming the due moment. |

## How the status is decided

The matching entry (same Reporter Id **and** same week) is evaluated in this order:

1. **Excused** — the entry is marked excused. Checked before everything else.
2. **Received** — filed **at or before** the due moment. The boundary is inclusive.
3. **Late** — filed after the due moment.
4. **Missing** — nothing filed and As Of is past the due moment.
5. **Pending** — nothing filed and the week is not due yet.

The due moment comes from the **Due Day and Time** setting, located inside the week being asked
about, in the configured time zone. For a week starting Monday `2025-11-03` with `Friday 17:00` and
`America/New_York`, that is `2025-11-07T17:00-05:00`.

## Example
**Input**: Reporter Id = `r-1`, Week Start = `2025-11-03`, As Of = `2025-11-08T09:00:00-05:00`,
Filed Reports = one row for `r-1` / `2025-11-03` received at `2025-11-07T17:01:00-05:00`

**Output**: Status = `Late`, Received At = `2025-11-07T17:01-05:00`,
Message = `Filed 2025-11-07T17:01-05:00, which is after the due moment 2025-11-07T17:00-05:00.`

## Business Rules
1. **`Pending` and `Missing` are different answers.** Nothing filed before the deadline is `Pending`;
   nothing filed after it is `Missing`. Chase only the second.
2. **The due moment is evaluated in the configured zone**, not the appliance's. A report filed at
   `2025-11-07T22:30Z` is 17:30 in New York — late against a 17:00 deadline, even though it is still
   the same UTC day.
3. **Rows for other reporters or other weeks are ignored**, so you can pass a whole roster.
4. **An entry with no Received At** behaves as nothing filed: `Missing` or `Pending` by As Of.
5. **Set As Of explicitly** whenever you need a reproducible answer — a report run, or a test.

## Error Handling
- **Blank Reporter Id** — `Success = false`, Message `Reporter Id is required. Please provide a value for Reporter Id.`
- **Unparseable Week Start or As Of** — `Success = false` naming the field and the expected shape.
- **A Filed Reports row missing its keys** — `Success = false`, Message
  `Filed Reports entry 2 needs both a Reporter Id and a Week Start.` The row number is 1-based.
- **Invalid configuration** — `Success = false` naming the setting to fix; the due moment cannot be
  computed without a valid Due Day and Time and Time Zone.

## Related Requests
- [Week Of](pages/WeekOf.md) — produces the Week Start this request expects.
