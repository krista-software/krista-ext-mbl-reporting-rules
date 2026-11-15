# Configuration

The extension is configured through the **Setup** tab in Krista. Configuration is stored per-invoker
— each invoker keeps its own independent settings.

None of these fields is a credential, and none is secured: this extension contacts no external
system.

## Setup tab fields

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| Reporting Mailbox Address | No | -- | The shared mailbox everyone sends reports to. Mail **from** this address is skipped: [Match Sender](pages/MatchSender.md) flags it and [Decide Filing](pages/DecideFiling.md) sets it aside. |
| Week Starts On | Yes | -- | `Monday` or `Sunday`. Every week boundary in the extension follows this. |
| Time Zone | Yes | -- | IANA time-zone id, for example `America/New_York`. Week boundaries and due moments are evaluated in this zone, never in the appliance's own zone. |
| Due Day and Time | Yes | -- | When a week's report is due, as `<Day> HH:mm` in 24-hour time — for example `Friday 17:00`. The day may be a full English name or its three-letter abbreviation (`Mon`–`Sun`), in any case. |

## Validation on save

The extension validates these fields before the configuration is saved, and rejects it with a
specific message if any of them is unusable:

1. **Time Zone** — must be a recognised IANA zone id. `America/Nowhere` is rejected naming the value.
2. **Week Starts On** — must be exactly `Monday` or `Sunday`.
3. **Due Day and Time** — must parse as `<Day> HH:mm`. `Friday 5pm` is rejected, with the expected
   format shown in the message.

Run [Test Connection](pages/TestConnection.md) after saving to see the parsed settings echoed back.

## Notes

- **The due moment is per week.** `Friday 17:00` means the Friday *inside* the week being asked
  about, at 17:00 in the configured zone — not "next Friday". For a week starting Monday 2025-11-03,
  the due moment is 2025-11-07T17:00 in your zone.
- **Changing Week Starts On changes past answers.** Week starts, week labels and the week comparison
  in [Decide Filing](pages/DecideFiling.md) are all derived from it, so a mid-stream change will make
  previously computed weeks disagree with newly computed ones.
- **Dates cross the boundary as ISO-8601 text**, not as Krista date pickers: dates are `YYYY-MM-DD`
  and timestamps are full ISO-8601 (for example `2025-11-07T16:30:00-05:00`). This is deliberate —
  it keeps a calendar date from shifting by a day when it is converted between zones.
- **The Due Day and Time value itself is provisional** and should be confirmed with MBL; no default
  is built in.
