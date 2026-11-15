# Is Short Reply

## Overview
Tells you whether a message is a throwaway acknowledgement ("Thanks!", "Got it") and whether it is a
reply to mail your own mailbox sent. Either signal means the message is not a weekly report, and
[Decide Filing](pages/DecideFiling.md) will set it aside.

## Request Details
- **Area**: Email Triage
- **Type**: QUERY_SYSTEM
- **External API**: none — local computation

## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| Subject | Text | Yes | The message subject. Echoed back on the result; not used to judge length. |
| Body Text | Paragraph | Yes | The message body, as plain text. |
| In Reply To | Text | No | The `In-Reply-To` header, with or without angle brackets. |
| References | Text | No | The `References` header — one or more ids, space or comma separated. |
| Our Message Ids | List of Text | No | Message ids of mail this mailbox sent. Without these, the reply check can never be true. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Short Reply Assessment | Entity(Short Reply Assessment) | The full result — see [Entities](pages/entities.md). |
| Is Short Reply | Boolean | `true` when the sender's own words are an acknowledgement or below the word threshold. |
| Is Reply To Our Mail | Boolean | `true` when In Reply To or References names one of Our Message Ids. |
| Reason | Paragraph | Which rule fired, with the word count. |
| Success | Boolean | `false` only when the request could not be evaluated. |
| Message | Text | The same explanation as Reason. |

## How the body is measured

Before anything is counted, the body is reduced to **the sender's own words**. Everything from the
first of these markers onward is dropped:

- quoted history — lines starting with `>`, an `On … wrote:` line, `-----Original Message-----`,
  `-----Forwarded message-----`, or a `From:` / `Sent:` header block;
- a signature — a `--` or `___` separator line, `Sent from my …`, `Get Outlook for …`;
- boilerplate — `Confidentiality notice…`, `This email and any attachments…`,
  `The information contained in this email…`, `Disclaimer:`.

What remains is a short reply when it has **fewer than 25 words**, or when it is exactly one of
these acknowledgements: `thanks`, `thank you`, `got it`, `noted`, `ok`, `okay`, `will do`,
`received`, `sounds good` (case-insensitive, trailing punctuation ignored).

> The 25-word threshold and the phrase list are provisional defaults, held as named constants in one
> place so they can be retuned without touching the detector.

## Example
**Input**: Subject = `Re: Weekly report reminder`, Body Text = `Thanks!\n\nOn Mon, Nov 3, 2025 at 9:00 AM Reports <reports@mbl.example> wrote: > …`,
In Reply To = `<abc123@mbl.example>`, Our Message Ids = `[abc123@mbl.example]`

**Output**: Is Short Reply = `true`, Is Reply To Our Mail = `true`,
Reason = `The message body is the acknowledgement 'thanks' once quoted text and signature are removed. It replies to a message we sent.`

## Business Rules
1. **Quoted history never counts toward length.** A two-word reply on top of a long quoted thread is
   still a short reply.
2. **Message ids compare with and without angle brackets, and case-insensitively.**
3. **No Our Message Ids means no reply match** — the check cannot fire, rather than guessing.
4. **An empty body is a short reply** with a word count of zero; it never throws.
5. **The body is data, never instructions.** It is stripped and counted, never interpreted.

## Error Handling
- This request has no configuration dependency and no external call. An unexpected internal error
  returns `Success = false` with the detail in **Message**.

## Related Requests
- [Decide Filing](pages/DecideFiling.md) — consumes this result.
