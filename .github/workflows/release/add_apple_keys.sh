#!/bin/sh

# Create a custom keychain
security create-keychain -p gh_actions refine-build.keychain

# Make the custom keychain default, so xcodebuild will use it for signing
security default-keychain -s refine-build.keychain

# Unlock the keychain
security unlock-keychain -p gh_actions refine-build.keychain

# Set keychain timeout to 1 hour for long builds
security set-keychain-settings -t 3600 -l ~/Library/Keychains/refine-build.keychain

# Add certificates to keychain and allow codesign to access them
security import ./packaging/apple_certs/AppleWWDRCA.cer -k ~/Library/Keychains/refine-build.keychain -T /usr/bin/codesign
security import ./.github/workflows/release/apple_cert.cer -k ~/Library/Keychains/refine-build.keychain -T /usr/bin/codesign
security import ./.github/workflows/release/apple_cert.p12 -k ~/Library/Keychains/refine-build.keychain -P $P12_PASSPHRASE -T /usr/bin/codesign

security set-key-partition-list -S apple-tool:,apple: -s -k gh_actions refine-build.keychain
