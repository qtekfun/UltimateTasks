# iCalendar corpus

Real-world task files used by the round-trip tests: every file must be written back byte for byte, and changing one property must leave every other line untouched.

- `synctools/`: VTODO fixtures from [bitfireAT/synctools](https://github.com/bitfireAT/synctools) (DAVx5), GPL-3.0-only. Copied unchanged.
- `edge-cases/`: written for this project to cover parsing corner cases (CRLF, folding inside multi-byte characters, quoted parameters, VALARM, VTIMEZONE, malformed lines, missing END).
- `nextcloud/`: files captured from a Nextcloud server with `tools/caldav-probe.py`, from test lists only.
