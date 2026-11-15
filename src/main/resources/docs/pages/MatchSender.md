# Match Sender

## Overview
Resolves an email sender against your reporter directory and says how confident the match is:
`Exact`, `Alias`, `None` or `Ambiguous`. It deliberately refuses to guess — an address that two
active reporters both claim comes back as `Ambiguous` with both candidates listed, so a human can
decide.

## Request Details
- **Area**: Sender Matching
- **Type**: QUERY_SYSTEM
- **External API**: none — local computation

## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| From Address | Text | Yes | The sender address. A full header form such as `Jane Doe <jane@mbl.example>` is accepted and unwrapped. |
| From Name | Text | No | The sender's display name. Recorded for context only — **never** used to match. |
| Reporter Directory | List of objects | Yes | Your reporters. Each row has `Id`, `Name`, `Emails`, `Aliases`, `Company Id`, `Active`. |

Each row of **Reporter Directory** contains:

| Field | Type | Description |
|---|---|---|
| Id | Text | The reporter's id, returned as **Reporter Id** on a match. |
| Name | Text | Display name. |
| Emails | Text | One or more primary addresses, separated by commas or semicolons. |
| Aliases | Text | Alternate addresses, separated by commas or semicolons. |
| Company Id | Text | The reporter's company, if any. |
| Active | Boolean | `false` means this reporter can never match. A row with no value is treated as active. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Sender Match | Entity(Sender Match) | The full result — see [Entities](pages/entities.md). |
| Match Type | Text | `Exact`, `Alias`, `None` or `Ambiguous`. |
| Reporter Id | Text | The matched reporter; empty unless `Exact` or `Alias`. |
| Company Id | Text | The matched reporter's company; empty when they have none. |
| Candidates | List of objects | Every directory row the address could refer to. |
| Success | Boolean | `false` only when the request could not be evaluated. |
| Message | Text | Why this answer was reached. |

Each item in **Candidates** contains:

| Field | Type | Description |
|---|---|---|
| Reporter Id | Text | The candidate's id. |
| Name | Text | The candidate's display name. |
| Company Id | Text | The candidate's company. |
| Matched On | Text | `Email`, `Alias`, or `Inactive` (matched the address but is not active). |

## Example
**Input**: From Address = `Jane.Doe@MBL.example`, Reporter Directory = one row with
Id `r-1`, Emails `jane.doe@mbl.example`, Company Id `c-1`, Active `true`

**Output**: Match Type = `Exact`, Reporter Id = `r-1`, Company Id = `c-1`, Candidate Count = `1`,
Message = `'jane.doe@mbl.example' is a primary address of Jane Doe (r-1).`

## Business Rules
1. **Matching is by address only.** The display name never influences the result.
2. **Comparison is case-insensitive** and trims whitespace; `Jane.Doe@MBL.example` matches
   `jane.doe@mbl.example`.
3. **Inactive reporters never match.** If the address belongs only to inactive reporters, the result
   is `None` — and they appear in **Candidates** marked `Inactive`, so you can see why.
4. **Two or more active reporters on one address is `Ambiguous`**, with an empty Reporter Id. One
   active plus one inactive is not ambiguous: the active one wins.
5. **Primary addresses beat aliases.** An `Exact` match is preferred when both are possible.
6. **The reporting mailbox is special.** If From Address equals the configured Reporting Mailbox
   Address, the result is `None` with `Is Reporting Mailbox = true`, and
   [Decide Filing](pages/DecideFiling.md) sets such a message aside.

## Error Handling
- **Blank From Address** — `Success = false`, Message `From Address is required. Please provide a value for From Address.`
- **Empty directory** — not an error: the result is `None` with no candidates.

## Related Requests
- [Decide Filing](pages/DecideFiling.md) — consumes this result.
