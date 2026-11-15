# Release Notes

- **Developer Name** : MBL Platform Team
- **Krista Service APIs Java** : 1.0.126
- **Global Catalog Version** : Not Available

## Version 1.0.0 (November 2025)

First release — the accountability-reporting rules, ported from MBL's AI Studio modules into a
Krista extension.

### New Features
* **Match Sender** resolves an email sender against a reporter directory as `Exact`, `Alias`, `None`
  or `Ambiguous`. Matching is by address only and case-insensitive; inactive reporters never match,
  and an address claimed by two or more active reporters is reported as ambiguous with every
  candidate listed.
* **Detect Stated Period** finds the week a message states for itself — explicit dates and ranges at
  High confidence, relative phrases and week numbers at Low — and normalises it onto the configured
  week start.
* **Is Short Reply** recognises throwaway acknowledgements after stripping quoted history,
  signatures and disclaimers, and detects replies to mail the reporting mailbox itself sent.
* **Decide Filing** combines those signals with a model's reading into one outcome: `File as
  Report`, `File under Company`, `Hold for Review` or `Set Aside`, always with a written reason.
* **Week Of** returns the reporting week a date falls in, honouring the Monday or Sunday week start,
  with an optional whole-week offset.
* **Get Report Status** reports `Received`, `Late`, `Missing`, `Excused` or `Pending` against the
  week's due moment, evaluated in the configured time zone.
* **Test Connection** and **Health Check** validate the saved configuration locally — there is no
  external system to reach.
* Every request returns `Success` and `Message` on every path, so a workflow never receives an empty
  reply.

### Resolved Bugs
* Not Available — first release.

### Dependency Updates
* Krista Service APIs Java 1.0.126 (`krista-apis` and `extension-impl-anno-processors` pinned
  together); HK2 3.0.3; Java 21. No HTTP or JSON client is bundled: the extension makes no network
  calls.

### Known Issues
* Not Available.

### Limitations and Caveats
* **No external system.** This extension performs no network calls and stores no state. Everything
  it reasons about is passed in as request inputs.
* **Email text is data only.** Subject and body are scanned for dates and word counts with fixed
  patterns; nothing written in a message can change what the extension does.
* **Provisional rule constants.** The short-reply word threshold (25) and acknowledgement phrase
  list, the week-label format, and the `<Day> HH:mm` due format are agreed defaults pending
  confirmation against MBL's AI Studio modules (`matcher.ts`, `reporting-period.ts`,
  `short-reply.ts`, `email-triage.ts`, `report-status.ts`), which were not available at build time.
  Each is held as a named constant in one place so reconciling them is a constant change.
* **`Pending` is a fifth status** beyond the four originally specified: it distinguishes "not filed
  and not yet due" from "not filed and overdue".
* **Dates cross the boundary as ISO-8601 text**, not Krista date pickers, so a calendar date cannot
  shift by a day when converted between zones.
