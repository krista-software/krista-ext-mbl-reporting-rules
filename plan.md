# MBL Reporting Rules — implementation plan

## Summary

| | |
|---|---|
| **Extension title** | MBL Reporting Rules |
| **Extension name (jar base)** | `mbl-reporting-rules` |
| **Module folder** | `mbl_reporting_rules` |
| **Class prefix** | `MblReportingRules` (e.g. `MblReportingRulesExtension`, `MblReportingRulesAttributes`) |
| **Version** | 1.0.0 |
| **Java package** | `app.krista.extensions.essentials.accountabilityreporting.mblreportingrules` |
| **Gradle group** | `app.krista.extensions.Essentials.AccountabilityReporting` |
| **Ecosystem / Domain** | Essentials / Accountability Reporting |
| **krista-apis** | 1.0.126 (pinned equal to `extension-impl-anno-processors`) |
| **Java** | 21 |
| **External system** | None. No HTTP client, no network calls, no secrets. Pure computation. |

This is a rules engine, not an integration: six read-only catalog requests that decide what to do
with weekly accountability reports emailed to a shared reporting mailbox. Every request is
`QUERY_SYSTEM`, returns a typed entity plus `Success` (Boolean) and `Message` (Text), and never
returns an empty reply.

> **Terminology note.** The requirement says every request is `READ_SYSTEM`. The Krista framework's
> `CatalogRequest.Type` enum is `QUERY_SYSTEM | CHANGE_SYSTEM | WAIT_FOR_EVENT`
> (`annotations-cheatsheet.md`) — there is no `READ_SYSTEM` member. All six requests are read-only,
> which is `QUERY_SYSTEM`. No new enum value is invented.

## Authentication — NONE

Per the wizard answer (`authentication: none`) and the requirement, there is **no authentication at
all**: no secured fields, no token, no authenticator, no auth tab. This is genuine, not an override —
the extension contacts nothing.

The Setup tab still carries four **unsecured configuration** attributes:

| Attribute | Type | Purpose |
|---|---|---|
| Reporting Mailbox Address | Text | Mail from this address is skipped |
| Week Starts On | Pick One: `Monday` / `Sunday` | Week boundary for all week arithmetic |
| Time Zone | Text (IANA id, e.g. `America/New_York`) | Zone for week boundaries and due moments |
| Due Day and Time | Text, `<Day> HH:mm` (e.g. `Friday 17:00`) | When a week's report is due |

`VALIDATE_ATTRIBUTES` and `TEST_CONNECTION` validate these **locally** — the time zone against
`ZoneId.getAvailableZoneIds()`, the week start against the two allowed values, and the due day/time
against the `<Day> HH:mm` grammar (full English day name or 3-letter abbreviation, case-insensitive,
24-hour time). No remote call is made by either hook.

## Areas and requests

### Set Up

| Request | Inputs | Output | Endpoint | Reused or new |
|---|---|---|---|---|
| Test Connection | — | Success, Message | none — local validation | new |
| Health Check | — | Success, Message | none — local validation | new |

### Sender Matching

| Request | Inputs | Output | Endpoint | Reused or new |
|---|---|---|---|---|
| Match Sender | From Address\*, From Name, Reporter Directory\* `[ { Id, Name, Emails, Aliases, Company Id, Active } ]` | `Entity(Sender Match)` + Success + Message | none — local computation | new |

Rules: email comparison is case-insensitive and trimmed; **inactive reporters never match**; an
address matching two or more active reporters is `Ambiguous` and lists every candidate; aliases are
alternate email addresses (not display names); a From Address equal to the configured Reporting
Mailbox Address sets `Is Reporting Mailbox` and returns `None`.

### Reporting Period

| Request | Inputs | Output | Endpoint | Reused or new |
|---|---|---|---|---|
| Detect Stated Period | Subject\*, Body Text\*, Received At\* (ISO-8601) | `Entity(Stated Period)` + Success + Message | none — local computation | new |
| Week Of | Date\* (YYYY-MM-DD), Offset Weeks | `Entity(Week Window)` + Success + Message | none — local computation | new |

Confidence mapping, per your answer: explicit calendar dates/ranges → **High**; relative phrases
("last week", "this week") resolved against Received At → **Low**; week numbers ("W45", "week 45") →
**Low**. Week Label renders as `Week of Nov 3, 2025`.

### Email Triage

| Request | Inputs | Output | Endpoint | Reused or new |
|---|---|---|---|---|
| Is Short Reply | Subject\*, Body Text\*, In Reply To, References, Our Message Ids `[ Text ]` | `Entity(Short Reply Assessment)` + Success + Message | none — local computation | new |
| Decide Filing | Sender Match\*, Period\*, Short Reply\*, Model Reading\* (typed objects) | `Entity(Filing Decision)` + Success + Message | none — local computation | new |

Short-reply rule (your provisional default, kept as named constants in one class so it can be
retuned): after stripping quoted text, signature and disclaimer, **fewer than 25 words** OR an exact
match against the acknowledgement phrase list (`thanks`, `thank you`, `got it`, `noted`, `ok`,
`okay`, `will do`, `received`, `sounds good`). `Is Reply To Our Mail` is true when In Reply To or
References contains any of Our Message Ids.

Decide Filing evaluates in this fixed order:

1. Short reply **or** reply to our own mail **or** sender is the reporting mailbox → **Set Aside**.
2. Model kind `not a report` → **Set Aside**.
3. Sender `None` or `Ambiguous` → **Hold for Review** (never auto-adopted), reason names which.
4. Stated period and model week disagree → **Hold for Review**, with **both values in the reason**.
5. Any low confidence, or no agreeing week at all → **Hold for Review**.
6. Kind `company update` **and** the reporter has a company id → **File under Company**;
   `company update` with no company id → **Hold for Review**.
7. Kind `own report`, sender Exact/Alias, week agreed, confidence high → **File as Report**.

Email subject and body are read as **data only** — never interpreted as instructions.

### Report Status

| Request | Inputs | Output | Endpoint | Reused or new |
|---|---|---|---|---|
| Get Report Status | Reporter Id\*, Week Start\*, Filed Reports `[ { Reporter Id, Week Start, Received At, Excused } ]`, As Of | `Entity(Report Status)` + Success + Message | none — local computation | new |

Order of evaluation, per your answer: Excused → Received (at or before the due moment) → Late (after
it) → Missing (nothing filed and As Of is past due) → **Pending** (nothing filed, not yet due).

## Entities

All six are new, named vendor-agnostically so a later extension in this domain can reuse them.

| Entity | Fields |
|---|---|
| Sender Match | Match Type, Reporter Id, Reporter Name, Company Id, Candidate Count, Is Reporting Mailbox, Candidates `[ { Reporter Id, Name, Company Id, Matched On } ]` |
| Stated Period | Period Found, Week Start, Week End, Evidence Text, Confidence |
| Short Reply Assessment | Is Short Reply, Is Reply To Our Mail, Reason |
| Filing Decision | Decision, Reporter Id, Company Id, Week Start, Reason |
| Week Window | Week Start, Week End, Week Label |
| Report Status | Status, Received At |

## Capabilities

| Capability | Enabled | Why |
|---|---|---|
| Entity search | No | No backing system to query; entities are computed results, not stored records |
| Webhooks / WAIT_FOR_EVENT | No | Nothing pushes to us |
| MCP (`tool = true`) | No | Not requested; these rules are meant to be composed by a workflow, and the hard rule that email text is data-only argues against exposing them as free agent tools. Easy to switch on later |
| Solutions tab | No | Not requested |
| File handling | No | No binary inputs or outputs |

## Domain reuse

`domain-catalog.json` returned **`found: false`** for Essentials / Accountability Reporting — the
domain is brand new, so there was nothing published to reuse and no per-item review was needed. All
six requests and all six entities are **new**. Nothing was excluded from scope.

To keep future reuse possible, nothing is prefixed with the vendor/organisation name: the entities
are `Sender Match`, `Stated Period`, `Week Window`, `Report Status` and so on — not
`MBL Sender Match`. Only the extension itself carries the MBL name.

## Testing (JUnit 5, pure Java)

Per `krista-extension-testing`, the framework classes (`@Extension`, Area classes) stay thin
delegators and are **not** unit-tested; all logic lives in plain classes that are. Planned test
classes: `SenderMatcherTest`, `StatedPeriodDetectorTest`, `ShortReplyDetectorTest`,
`FilingDecisionEngineTest`, `WeekCalculatorTest`, `ReportStatusCalculatorTest`, plus
`ConnectionConfigValidatorTest`. The required scenarios are all covered, including unknown sender →
Hold for Review, ambiguous sender → Hold for Review, short reply → Set Aside, period disagreement →
Hold for Review, and week arithmetic across a year boundary in **both** Monday and Sunday modes. The
full per-request list is in `implementation-plan.json`.

---

## ⚠ Placeholder IDs — must be replaced before deploy

| Placeholder | Planned value | Action needed |
|---|---|---|
| `@Domain(id = ...)` | `catEntryDomain_<minted-uuid>` for "Accountability Reporting" | **MUST REPLACE** with the real registered domain id once the domain is registered in Krista. It compiles but will not bind. |
| `@Domain(ecosystemId = ...)` | `catEntryEcosystem_d5309676-d39f-5941-b881-61f8c5dee3cf` | Copied from the bundled `servicetrade` reference, which is in the same **Essentials** ecosystem — believed real and registered, but worth confirming against a current sibling. |
| `@Domain(ecosystemVersion = ...)` | minted stable UUID per file | Loose value; a stable UUID is acceptable. |
| Request ids (`localDomainRequest_…`) / entity ids (`localDomainEntity_…`) | `uuid5` of `"mbl-reporting-rules.<area>.<method>"` | Local to the extension — correct as generated, no action. |

## ⚠ Decisions and assumptions to review

1. **Nested lists inside composite fields are not used.** The skills only attest flat inline objects
   (`[ { Key: Text, ... } ]`); a nested `Emails: [ Text ]` inside an object is nowhere in the
   framework reference, so it is **not** invented here. Instead, `Reporter Directory`'s `Emails` and
   `Aliases` are **Text holding a comma- or semicolon-separated list**, split and trimmed by the
   parser. Say the word if you would rather pass one directory row per email address.
2. **A fifth status, `Pending`,** is added to Get Report Status per your answer — the original
   requirement listed four (Received / Late / Missing / Excused).
3. **`As Of`** is added as an optional input to Get Report Status per your answer, so lateness is
   deterministic and testable.
4. **Mail from the reporting mailbox is `Set Aside`, not held.** The requirement says such mail is
   "skipped"; Hold for Review would queue our own mail for a human, so Set Aside is the reading used.
   Match Sender surfaces it as `Is Reporting Mailbox` so the behaviour is visible, not buried.
5. **Dates cross the boundary as ISO-8601 text** (your answer), not Krista `@Field.Date` epoch
   millis — so week starts never shift by a day near midnight. The trade-off is that Krista's date
   picker is not offered on these inputs.
6. **Candidates are not passed into Decide Filing** — only `Candidate Count`, because a nested list
   inside a composite input is not an attested type (see 1). The count is enough to explain an
   ambiguous hold in the reason text.
7. **The TypeScript modules were not supplied**, so the matching, period-parsing, short-reply and
   status rules are built from the written requirement and your provisional answers rather than
   ported. The thresholds, phrase list, week-label format and due-time format are isolated as named
   constants in one place each, so reconciling them with `matcher.ts` / `reporting-period.ts` /
   `short-reply.ts` / `email-triage.ts` / `report-status.ts` later is a constant change, not a
   rewrite.
