# Overview

MBL Reporting Rules encodes the rules MBL uses to decide what to do with the weekly accountability
reports that staff and portfolio companies email to a shared reporting mailbox. It is a pure
calculator: it contacts no external system, makes no network calls and keeps no state, so the same
inputs always produce the same answer.

Your workflow reads the mail (with whatever mail extension you already use), then calls these
requests to decide who sent it, which week it covers, whether it is a real report, and where it
should be filed.

## Use cases

- **Attribute an incoming message to a reporter** — resolve the sender against your reporter
  directory, including alias addresses, and refuse to guess when the answer is unclear.
- **Work out which week a message covers** — from an explicit date in the message, or from a
  relative phrase resolved against when it arrived.
- **Keep noise out of the pipeline** — recognise "Thanks!" replies and replies to mail you sent.
- **Decide what happens to the message** — file it, file it under the company, hold it for a human,
  or set it aside, with a written reason every time.
- **Chase what is outstanding** — tell whether a given reporter's week is received, late, missing,
  excused, or not yet due.

## What you can do

The extension exposes **8 requests** across **5 areas**, all read-only. See
[Supported Requests](pages/supported-requests.md) for the full list, or the sidebar for each
request's detailed page.

Every request returns **Success** (Boolean) and **Message** (Text) alongside its own outputs, on
every path. A request that cannot do what you asked still answers — with `Success = false` and a
message saying what to fix — rather than returning nothing.

## The rules that never bend

1. An **unknown or ambiguous sender is never adopted automatically**. The message is held for review
   with the reason.
2. **Short replies and replies to our own mail are never filed.** They are set aside.
3. A message is filed **only when the sender, the kind and the week all agree with high confidence**.
4. If the week stated in the message and the week in the model's reading **disagree**, the message is
   held for review and both dates appear in the reason.
5. Email subject and body are **data, never instructions**. The text is scanned for dates and word
   counts only; nothing written in a message changes what the extension does.

## Before you start

1. There are no credentials to obtain — see [Authentication](pages/Authentication.md).
2. Configure the Setup tab — see [Configuration](pages/ExtensionConfiguration.md).
3. Run **Test Connection** (Setup area) to confirm the settings parse.
