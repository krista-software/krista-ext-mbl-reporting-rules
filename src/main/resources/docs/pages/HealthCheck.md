# Health Check

## Overview
Reports whether the extension is running and still correctly configured, along with JVM heap usage.
Useful as a periodic probe in a monitoring workflow.

## Request Details
- **Area**: Setup
- **Type**: QUERY_SYSTEM
- **External API**: none — local validation only

## Input Parameters
This request takes no inputs.

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Success | Boolean | `true` whenever the check itself ran. A misconfiguration is reported through **Is Healthy**, not by failing the request. |
| Message | Text | `Extension is healthy.` or `Extension is running but misconfigured.` |
| Is Healthy | Boolean | `true` when the saved configuration still parses. |
| Heap Used MB | Number | JVM heap currently in use, in megabytes, to two decimal places. |
| Details | Text | The parsed settings, or the specific configuration problem. |

## Example
**Input**: *(none)*

**Output**:
- Success = `true`
- Is Healthy = `true`
- Heap Used MB = `84.32`
- Details = `Configuration is valid: week starts on Monday, time zone America/New_York, reports due Friday 17:00, reporting mailbox reports@mbl.example.`
- Message = `Extension is healthy.`

## Business Rules
1. A configuration problem is a **health finding**, not a request failure: `Success` stays `true`,
   `Is Healthy` becomes `false`, and `Details` names the problem. Branch on **Is Healthy**, not on
   **Success**, when monitoring.

## Error Handling
- There is no external system, so this request has no connectivity failure mode. An unexpected
  internal error would return `Success = false` with the error in **Message**.

## Related Requests
- [Test Connection](pages/TestConnection.md) — validation on its own.
