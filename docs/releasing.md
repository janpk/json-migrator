# Releasing to Maven Central

json-migrator publishes through the **Sonatype Central Portal** (`central.sonatype.com`). Versioning
is **tag-driven**: [jgitver](https://github.com/jgitver/jgitver) derives the Maven version from git,
so a build on tag `1.2.3` is version `1.2.3` (between tags it is the next patch `-SNAPSHOT`). You
never edit versions in the pom — the tag is the source of truth. Publishing is wired behind the
`release` Maven profile so day-to-day builds stay fast.

## What gets published

| Artifact | Published? |
| --- | --- |
| `com.mosedotten.json.migrator:json-migrator` (parent pom) | yes |
| `com.mosedotten.json.migrator:json-migrator-engine` | yes |
| `com.mosedotten.json.migrator:json-migrator-engine-java` | yes |
| `demo-kotlin`, `demo-java`, `engine-test`, `report` | no (`maven.deploy.skip=true`) |

## One-time setup

### 1. Verify the namespace (`com.mosedotten`)

1. Sign in to `central.sonatype.com`.
2. Add the namespace `com.mosedotten`; the Portal shows a TXT record value.
3. Add that value as a **DNS TXT record on `mosedotten.com`** and click verify.

### 2. GPG signing key

Central requires every artifact to be PGP-signed.

```bash
gpg --gen-key                                                # remember the passphrase
gpg --list-keys                                              # note the key id
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>    # publish the public key
gpg --armor --export-secret-keys <KEY_ID> > private-key.asc  # for the CI secret (keep safe, delete after)
```

### 3. GitHub repository secrets (for the Release workflow)

| Secret | Value |
| --- | --- |
| `CENTRAL_TOKEN_USERNAME` | Portal → Generate User Token → username |
| `CENTRAL_TOKEN_PASSWORD` | …the token's password |
| `GPG_PRIVATE_KEY` | contents of `private-key.asc` |
| `GPG_PASSPHRASE` | the key's passphrase |

## Cutting a release

```bash
make release VERSION=1.2.3
```

`make release` checks the working tree is clean, you're on an up-to-date `main`, `VERSION` is
SemVer, and the tag doesn't already exist — then creates the annotated tag `1.2.3` and pushes it.
The push triggers `.github/workflows/release.yml`, where jgitver sets the version from the tag and
`./mvnw -Prelease deploy` signs and uploads the bundle. The workflow then creates a **GitHub Release**
whose notes are generated from the commit history since the previous tag — grouped by each commit's
leading verb (Added / Fixed / Removed / Changed / Documentation), configured in `cliff.toml`.

The `release` profile uses `autoPublish=true`, so a deployment that validates cleanly is published
to Maven Central automatically — no manual Portal step. The artifacts are resolvable in minutes and
searchable in ~30. Because a published version is **immutable**, make sure `make release` cuts
exactly the version you intend; there is no drop-and-retry once it goes live. To review before
publishing instead, set `autoPublish=false` and the deployment will wait in
**central.sonatype.com → Deployments** for a manual **Publish** click (a validated-but-unpublished
deployment can be dropped and re-uploaded at the same version).

### Manual/local release (fallback)

If you need to release without CI, put the Portal token under a `central` server in
`~/.m2/settings.xml`, check out the tag, and run (pinning the version to the tag with
`jgitver.use-version` so it is never a `-SNAPSHOT`, which Central rejects):

```bash
./mvnw -Prelease -Dkover.skip=true -Djgitver.use-version=1.2.3 -Dgpg.passphrase=<PASSPHRASE> clean deploy
```

## Notes and follow-ups

- **Do not use `versions:set` or edit pom versions** — jgitver computes the version from the tag. The
  literal versions in the poms are placeholders; the deployed pom carries the resolved version.
- Every CI checkout uses `fetch-depth: 0` because jgitver needs the git history and tags.
- **The javadoc jar is a valid but empty placeholder** (satisfies Central's requirement). To ship
  rendered Kotlin API docs, replace the `maven-jar-plugin` `attach-javadoc` execution in the
  `release` profile with the Dokka plugin, using a version that supports the Kotlin in use.
- The release plumbing could not be dry-run in the development sandbox (no network access to the
  Maven repository for jgitver and the publishing plugins); the first tagged release is where it is
  validated end to end.
