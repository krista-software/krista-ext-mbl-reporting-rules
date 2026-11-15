# Authentication

**MBL Reporting Rules has no authentication, because it has nothing to authenticate to.**

The extension is a pure calculator. It contacts no external system, makes no HTTP calls, and holds no
credentials — there is no token, no API key, no OAuth app, no sign-in, and no Authentication tab.

## What this means for you

- **There is nothing to obtain.** You do not need to register an app, create a personal access token,
  or ask anyone for a secret before using this extension.
- **No field on the Setup tab is secured**, because none of them is a credential. The four settings
  are local policy values — see [Configuration](pages/ExtensionConfiguration.md).
- **No outbound network access is required** from the appliance for this extension to work.

## Required scopes / permissions

| Scope / Permission | Why it's needed |
|---|---|
| None | The extension never calls another system, so it requests no scopes and needs no permissions granted anywhere. |

## Where the data comes from, then

Everything the rules work on is passed in by your workflow as request inputs — the sender address,
the subject and body, the reporter directory, the reports already on file. Nothing is fetched and
nothing is retained after the request returns.

The mail itself is read by whatever mail integration you already use; this extension only decides
what the rules say about it.

## Verifying

Run [Test Connection](pages/TestConnection.md) in the Setup area. Because there is no connection, it
validates the saved configuration instead — the time zone, the week start and the due day/time — and
echoes the parsed settings back to you. A failure means a setting needs fixing, not that a system is
unreachable.
