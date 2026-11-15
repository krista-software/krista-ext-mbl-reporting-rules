# Test Connection

## Overview
Confirms the extension is configured correctly. There is no external system to reach, so this
validates the saved Setup-tab settings — the IANA time zone, the week start, and the Due Day and
Time format — and echoes back what it parsed.

## Request Details
- **Area**: Setup
- **Type**: QUERY_SYSTEM
- **External API**: none — local validation only

## Input Parameters
This request takes no inputs.

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Success | Boolean | `true` when every setting parses. |
| Message | Text | The parsed settings in plain language, or what is wrong with them. |

## Example
**Input**: *(none)*

**Output**:
- Success = `true`
- Message = `Configuration is valid: week starts on Monday, time zone America/New_York, reports due Friday 17:00, reporting mailbox reports@mbl.example. This extension performs no network calls, so there is nothing further to test.`

## Error Handling
A configuration problem returns `Success = false` with the specific fix in **Message**:

- **Unknown time zone** — `Time Zone 'America/Nowhere' is not a recognized IANA time-zone id. Use a value such as 'America/New_York'.`
- **Bad week start** — `Week Starts On must be either 'Monday' or 'Sunday', but was 'Wednesday'.`
- **Unparseable due day/time** — `Due Day and Time 'Friday 5pm' could not be understood. Expected '<Day> HH:mm' in 24-hour time, for example 'Friday 17:00'.`

## Related Requests
- [Health Check](pages/HealthCheck.md) — the same validation plus JVM heap usage.
- [Configuration](pages/ExtensionConfiguration.md) — what each setting means.
