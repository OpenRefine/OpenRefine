#!/usr/bin/env python3
"""Validate an App Store Connect API key against Apple's notary service.

The Linux equivalent of `xcrun notarytool history` - builds the same ES256 JWT
notarytool builds, and calls the same endpoint. Useful for separating credential
problems from build-pipeline problems without a Mac or a full CI run.

Usage:
    check_notarization_credentials.py <keyfile.p8> <key-id> <issuer-id>

Prints status codes, Apple's error text, and recent submission history.
Never prints the private key or the bearer token.
"""
import json
import sys
import time
import urllib.error
import urllib.request

ENDPOINT = "https://appstoreconnect.apple.com/notary/v2/submissions"


def main(argv):
    if len(argv) != 4:
        print(__doc__.strip())
        return 2

    key_path, key_id, issuer_id = argv[1], argv[2], argv[3]

    try:
        private_key = open(key_path).read()
    except OSError as e:
        print(f"cannot read key file: {e}")
        return 1

    # Surface the paste errors that cost us days: stray whitespace is invisible
    # in a web form but changes the JWT and yields a bare 401 from Apple.
    for label, value, expected in (("key id", key_id, 10), ("issuer id", issuer_id, 36)):
        if value != value.strip():
            print(f"WARNING: {label} has leading/trailing whitespace")
        if len(value.strip()) != expected:
            print(f"WARNING: {label} is {len(value.strip())} chars, expected {expected}")

    try:
        import jwt
    except ImportError:
        print("PyJWT is required:  pip install pyjwt cryptography")
        return 1

    now = int(time.time())
    token = jwt.encode(
        {"iss": issuer_id, "iat": now, "exp": now + 900, "aud": "appstoreconnect-v1"},
        private_key,
        algorithm="ES256",
        headers={"kid": key_id},
    )
    print(f"JWT built: alg=ES256 kid={key_id} iss={issuer_id}")

    req = urllib.request.Request(ENDPOINT, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            data = json.loads(r.read().decode()).get("data", [])
        print(f"HTTP {r.status} -- CREDENTIALS ACCEPTED ({len(data)} submissions visible)")
        print("\nMost recent submissions:")
        for s in data[:5]:
            a = s.get("attributes", {})
            print(f"  {a.get('createdDate','?'):26} {a.get('status','?'):10} {a.get('name','?')}")
        return 0
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"HTTP {e.code} -- REJECTED")
        if e.code == 401:
            print("  Unauthenticated: the JWT was rejected. The key, key id and")
            print("  issuer id do not correspond, or one carries stray whitespace.")
        elif e.code == 403:
            print("  Forbidden: credentials are valid but lack notarization access.")
        print(f"  Apple said: {body.strip()[:300]}")
        return 1
    except Exception as e:
        print(f"request failed: {type(e).__name__}: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
