#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 UltimateTasks contributors
# SPDX-License-Identifier: GPL-3.0-or-later
"""Probes a Nextcloud CalDAV server for the T02 prototype (PLAN.md).

Only touches a list it creates itself ("UltimateTasks probe") and, read-mostly, the Deck
board you name. Credentials come from ~/.config/ultimatetasks-probe (chmod 600):

    server=https://cloud.example.com
    user=alice
    password=<an app password>

The password is never printed. Results go to build/probe/ and captured tasks to
app/src/test/resources/ics-corpus/nextcloud/.

    tools/caldav-probe.py discover        # lists collections, creates the probe list
    tools/caldav-probe.py capture         # saves the tasks of the probe list
    tools/caldav-probe.py deck <board>    # saves the board's tasks, tests writing one back
    tools/caldav-probe.py protocol        # sync-collection, PROPPATCH, PUT/DELETE on the probe list
"""

import base64
import os
import re
import stat
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "build", "probe")
CORPUS = os.path.join(ROOT, "app", "src", "test", "resources", "ics-corpus", "nextcloud")
PROBE_NAME = "UltimateTasks probe"
NS = {"d": "DAV:", "c": "urn:ietf:params:xml:ns:caldav", "cs": "http://calendarserver.org/ns/",
      "a": "http://apple.com/ns/ical/", "oc": "http://owncloud.org/ns", "nc": "http://nextcloud.com/ns"}


def config():
    path = os.path.expanduser("~/.config/ultimatetasks-probe")
    if os.stat(path).st_mode & (stat.S_IRWXG | stat.S_IRWXO):
        sys.exit(f"{path} must be chmod 600")
    values = dict(line.strip().split("=", 1) for line in open(path) if "=" in line)
    if not values["server"].startswith("https://"):
        sys.exit("server must use https")
    return values


CFG = config()
AUTH = "Basic " + base64.b64encode(f"{CFG['user']}:{CFG['password']}".encode()).decode()
LOG = []


def request(method, url, body=None, headers=None):
    url = urllib.parse.urljoin(CFG["server"], url)
    data = body.encode() if isinstance(body, str) else body
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", AUTH)
    req.add_header("User-Agent", "UltimateTasks-probe")
    for key, value in (headers or {}).items():
        req.add_header(key, value)
    try:
        with urllib.request.urlopen(req) as res:
            status, res_headers, text = res.status, dict(res.headers), res.read().decode()
    except urllib.error.HTTPError as err:
        status, res_headers, text = err.code, dict(err.headers), err.read().decode(errors="replace")
    LOG.append(f"{method} {urllib.parse.urlparse(url).path} -> {status}")
    return status, res_headers, text


def propfind(url, props, depth="0"):
    body = f'<d:propfind {" ".join(f"xmlns:{k}=\"{v}\"" for k, v in NS.items())}><d:prop>{props}</d:prop></d:propfind>'
    status, _, text = request("PROPFIND", url, body, {"Depth": depth, "Content-Type": "application/xml"})
    return status, ET.fromstring(text) if text.strip().startswith("<") else None


def home():
    _, tree = propfind("/remote.php/dav/", "<d:current-user-principal/>")
    principal = tree.find(".//d:current-user-principal/d:href", NS).text
    _, tree = propfind(principal, "<c:calendar-home-set/>")
    return tree.find(".//c:calendar-home-set/d:href", NS).text


def collections():
    props = ("<d:displayname/><d:resourcetype/><c:supported-calendar-component-set/><a:calendar-color/>"
             "<a:calendar-order/><cs:getctag/><d:sync-token/><d:current-user-privilege-set/>"
             "<oc:owner-principal/><nc:deleted-at/><cs:source/>")
    _, tree = propfind(home(), props, depth="1")
    result = []
    for response in tree.findall("d:response", NS):
        prop = response.find(".//d:prop", NS)
        components = [c.get("name") for c in prop.findall(".//c:comp", NS)]
        types = [child.tag.split("}")[1] for child in prop.find("d:resourcetype", NS) or []]
        privileges = sorted({p.tag.split("}")[1] for p in prop.findall(".//d:privilege/*", NS)})
        text = lambda path: (prop.find(path, NS).text if prop.find(path, NS) is not None else None)
        result.append({"href": response.find("d:href", NS).text, "name": text("d:displayname"),
                       "types": types, "components": components, "color": text("a:calendar-color"),
                       "order": text("a:calendar-order"), "ctag": text("cs:getctag") is not None,
                       "sync-token": text("d:sync-token") is not None, "privileges": privileges,
                       "owner": text("oc:owner-principal")})
    return result


def probe_list(create=False):
    found = [c for c in collections() if c["name"] == PROBE_NAME]
    if found:
        return found[0]["href"]
    if not create:
        sys.exit("Run 'discover' first")
    href = home() + "ultimatetasks-probe/"
    body = (f'<c:mkcalendar xmlns:d="DAV:" xmlns:c="{NS["c"]}" xmlns:a="{NS["a"]}"><d:set><d:prop>'
            f"<d:displayname>{PROBE_NAME}</d:displayname><a:calendar-color>#FF9500</a:calendar-color>"
            '<c:supported-calendar-component-set><c:comp name="VTODO"/></c:supported-calendar-component-set>'
            "</d:prop></d:set></c:mkcalendar>")
    status, _, text = request("MKCALENDAR", href, body, {"Content-Type": "application/xml"})
    LOG.append(f"MKCALENDAR body: {text[:300]}")
    return href


def tasks(href):
    body = (f'<c:calendar-query xmlns:d="DAV:" xmlns:c="{NS["c"]}"><d:prop><d:getetag/><c:calendar-data/>'
            '</d:prop><c:filter><c:comp-filter name="VCALENDAR"><c:comp-filter name="VTODO"/>'
            "</c:comp-filter></c:filter></c:calendar-query>")
    _, _, text = request("REPORT", href, body, {"Depth": "1", "Content-Type": "application/xml"})
    tree = ET.fromstring(text)
    return [(r.find("d:href", NS).text, r.find(".//d:getetag", NS).text, r.find(".//c:calendar-data", NS).text)
            for r in tree.findall("d:response", NS) if r.find(".//c:calendar-data", NS) is not None]


def save_corpus(prefix, items):
    os.makedirs(CORPUS, exist_ok=True)
    for href, _, data in items:
        name = re.sub(r"[^A-Za-z0-9._-]", "_", href.rstrip("/").split("/")[-1])
        # ElementTree turns the CRLF of calendar-data into LF; servers send CRLF.
        data = data.replace("\r\n", "\n").replace("\n", "\r\n")
        with open(os.path.join(CORPUS, f"{prefix}-{name}"), "w", newline="") as file:
            file.write(data)
    print(f"saved {len(items)} tasks to {CORPUS}")


def discover():
    probe_list(create=True)
    report = collections()
    for c in report:
        print(c)
    write("discover.txt", "\n".join(map(str, report)))


def capture():
    save_corpus("tasks", tasks(probe_list()))


def deck(board):
    matches = [c for c in collections() if c["name"] and board.lower() in c["name"].lower()]
    if len(matches) != 1:
        sys.exit(f"Expected one collection matching {board!r}, found {[c['name'] for c in matches]}")
    collection = matches[0]
    print(collection)
    items = tasks(collection["href"])
    save_corpus("deck", items)
    lines = [str(collection)]
    if items:
        href, etag, data = items[0]
        data = data.replace("\r\n", "\n").replace("\n", "\r\n")
        same = request("PUT", href, data, {"If-Match": etag, "Content-Type": "text/calendar; charset=utf-8"})
        lines.append(f"PUT unchanged with If-Match: {same[0]} etag={same[1].get('ETag')}")
        changed = re.sub(r"(?m)^SUMMARY:(.*)$", r"SUMMARY:\1 (probe)", data, count=1)
        status, headers, _ = request("PUT", href, changed, {"Content-Type": "text/calendar; charset=utf-8"})
        lines.append(f"PUT with changed SUMMARY: {status} etag={headers.get('ETag')}")
        after = tasks(collection["href"])
        lines.append("summary after: " + next((re.search(r"(?m)^SUMMARY:(.*)$", d).group(1).strip()
                                                for h, _, d in after if h == href), "?"))
        back = request("PUT", href, data, {"Content-Type": "text/calendar; charset=utf-8"})
        lines.append(f"PUT original back: {back[0]}")
        stale = request("PUT", href, data, {"If-Match": '"stale"', "Content-Type": "text/calendar; charset=utf-8"})
        lines.append(f"PUT with a stale If-Match: {stale[0]} (412 expected)")
    write("deck.txt", "\n".join(lines))


def protocol():
    href = probe_list()
    lines = []
    sync = (f'<d:sync-collection xmlns:d="DAV:"><d:sync-token/><d:sync-level>1</d:sync-level>'
            "<d:prop><d:getetag/></d:prop></d:sync-collection>")
    status, _, text = request("REPORT", href, sync, {"Content-Type": "application/xml"})
    token = re.search(r"<d:sync-token>([^<]+)</d:sync-token>", text)
    lines.append(f"sync-collection initial: {status}, token={bool(token)}, entries={text.count('<d:response>')}")
    uid = f"probe-{uuid.uuid4()}"
    ics = ("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//UltimateTasks//probe//EN\r\nBEGIN:VTODO\r\n"
           f"UID:{uid}\r\nSUMMARY:Probe task\r\nDUE;VALUE=DATE:20261010\r\nPRIORITY:5\r\n"
           "CATEGORIES:probe\r\nX-APPLE-SORT-ORDER:1\r\nEND:VTODO\r\nEND:VCALENDAR\r\n")
    task = f"{href}{uid}.ics"
    created = request("PUT", task, ics, {"If-None-Match": "*", "Content-Type": "text/calendar; charset=utf-8"})
    lines.append(f"PUT new with If-None-Match: {created[0]} etag={created[1].get('ETag')}")
    again = request("PUT", task, ics, {"If-None-Match": "*", "Content-Type": "text/calendar; charset=utf-8"})
    lines.append(f"PUT same again with If-None-Match: {again[0]} (412 expected)")
    if token:
        body = sync.replace("<d:sync-token/>", f"<d:sync-token>{token.group(1)}</d:sync-token>")
        status, _, text = request("REPORT", href, body, {"Content-Type": "application/xml"})
        lines.append(f"sync-collection with token: {status}, entries={text.count('<d:response>')}")
    patch = (f'<d:propertyupdate xmlns:d="DAV:" xmlns:a="{NS["a"]}"><d:set><d:prop>'
             "<a:calendar-color>#34C759</a:calendar-color><a:calendar-order>7</a:calendar-order>"
             "<d:displayname>UltimateTasks probe</d:displayname></d:prop></d:set></d:propertyupdate>")
    lines.append(f"PROPPATCH color/order/name: {request('PROPPATCH', href, patch, {'Content-Type': 'application/xml'})[0]}")
    lines.append(f"DELETE task: {request('DELETE', task)[0]}")
    write("protocol.txt", "\n".join(lines))


def write(name, text):
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name), "w") as file:
        file.write(text + "\n\nRequests:\n" + "\n".join(LOG) + "\n")
    print(text)
    print(f"-> {os.path.join(OUT, name)}")


if __name__ == "__main__":
    commands = {"discover": discover, "capture": capture, "protocol": protocol}
    if len(sys.argv) == 3 and sys.argv[1] == "deck":
        deck(sys.argv[2])
    elif len(sys.argv) == 2 and sys.argv[1] in commands:
        commands[sys.argv[1]]()
    else:
        sys.exit(__doc__)
