# Publishing Setup Guide

This guide explains how to set up publishing to Maven Central with automatic versioning.

## Overview

- **Publishing target**: Maven Central via Sonatype Central Portal
- **Versioning**: Automatic from git tags using axion-release-plugin
- **Signing**: GPG signature required for all artifacts

## One-Time Setup

### 1. Register with Sonatype Central Portal

1. Go to https://central.sonatype.com/
2. Sign up or log in
3. Verify namespace ownership for `io.github.matthewjones372`
   - For GitHub namespaces: Link your GitHub account to automatically verify `io.github.YOUR_USERNAME`

### 2. Generate GPG Key

```bash
# Generate key (use RSA, 4096 bits)
gpg --full-generate-key

# List your keys
gpg --list-secret-keys --keyid-format=long

# Export private key (ASCII-armored)
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key.asc

# Get the passphrase you set during key generation
```

### 3. Configure Local Credentials

Create or edit `~/.gradle/gradle.properties`:

```properties
mavenCentralUsername=your-sonatype-username
mavenCentralPassword=your-sonatype-password
signingInMemoryKey=<paste-entire-ascii-armored-key-including-headers>
signingInMemoryKeyPassword=your-gpg-passphrase
```

**Important**: The `signingInMemoryKey` should include the full ASCII-armored key:
```
-----BEGIN PGP PRIVATE KEY BLOCK-----
...
-----END PGP PRIVATE KEY BLOCK-----
```

### 4. Configure GitHub Secrets

Add these secrets to your GitHub repository (Settings > Secrets and variables > Actions):

- `MAVEN_CENTRAL_USERNAME`: Your Sonatype username
- `MAVEN_CENTRAL_PASSWORD`: Your Sonatype password (or token)
- `GPG_PRIVATE_KEY`: Your ASCII-armored GPG private key
- `GPG_PASSPHRASE`: Your GPG key passphrase

## How Automatic Versioning Works

The project uses [axion-release-plugin](https://github.com/allegro/axion-release-plugin) for automatic versioning:

- **Release version**: When current commit has a tag (e.g., tag `v1.0.2` → version `1.0.2`)
- **SNAPSHOT version**: When there are commits after the latest tag (e.g., `1.0.3-SNAPSHOT`)

### Check Current Version

```bash
./gradlew currentVersion
```

## Publishing Workflow

### Option 1: Automatic Publishing (Recommended)

Simply push a git tag:

```bash
git tag v1.0.3
git push origin v1.0.3
```

GitHub Actions will:
1. Detect version `1.0.3` from tag
2. Build all modules
3. Sign artifacts with GPG
4. Publish to Maven Central

### Option 2: Manual Local Publishing

```bash
# Build and publish
./gradlew publishAllPublicationsToMavenCentralRepository
```

## Published Artifacts

Each submodule is published separately:

- `io.github.matthewjones372:golden-core:VERSION`
- `io.github.matthewjones372:golden-jackson:VERSION`
- `io.github.matthewjones372:golden-kotlinx-json:VERSION`

All artifacts include:
- Main JAR
- Sources JAR
- Javadoc JAR (auto-generated)
- POM with metadata
- GPG signatures (.asc files)

## Consuming from Maven Central

Once published, users can consume without authentication:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.github.matthewjones372:golden-jackson:1.0.2")
}
```

## Troubleshooting

### Version showing as 0.1.0

This means no git tags are detected. Check:
```bash
git tag -l
git fetch --tags
```

### Publishing fails with "Unauthorized"

Check your credentials in `~/.gradle/gradle.properties` or GitHub Secrets.

### Signing fails

Ensure your GPG key is properly formatted in the properties file, including the header and footer lines.

### Maven Central sync delay

After successful publishing, artifacts may take 15-30 minutes to appear on Maven Central search. They are immediately available for download once published.

## References

- [Sonatype Central Portal](https://central.sonatype.com/)
- [Vanniktech Maven Publish Plugin](https://github.com/vanniktech/gradle-maven-publish-plugin)
- [Axion Release Plugin](https://github.com/allegro/axion-release-plugin)
- [GPG Documentation](https://gnupg.org/documentation/)
