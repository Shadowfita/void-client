# Releasing

How to ship a new build of Void Client to end users.

## Overview

Source lives in the **private** repo `2011Scape/void-client`. Releases
(obfuscated jar + SHA-256 checksum) are published to the **public** repo
`2011Scape/void-client-releases`. End users download from the public repo.

Releases are cut by pushing a semver tag (`vX.Y.Z`). The
[`release` workflow](.github/workflows/release.yml) then:

1. Builds the shadow jar (`./gradlew :client:shadowJar`).
2. Runs ProGuard with [`client/proguard-rules.pro`](client/proguard-rules.pro)
   to obfuscate default-package project classes. Libraries are kept intact.
3. Computes `sha256sum` of the resulting jar.
4. Creates a release on `void-client-releases` via `gh release create`,
   attaching the jar and the `.sha256` file.

## Cutting a release

1. Bump `version = "X.Y.Z"` in `client/build.gradle.kts`.
2. Commit and push to `main` (normal PR flow).
3. Tag the commit and push the tag:

   ```bash
   git tag -a vX.Y.Z -m "vX.Y.Z"
   git push origin vX.Y.Z
   ```

4. Watch the run at
   https://github.com/2011Scape/void-client/actions. The workflow fails fast
   if the tag does not match the gradle version.

5. Once green, the release appears at
   https://github.com/2011Scape/void-client-releases/releases.

## Verifying a download

End users can verify a release jar against its checksum:

```bash
sha256sum --check void-client-X.Y.Z.jar.sha256
```

## Secrets and permissions

The workflow needs a fine-grained personal access token stored as the
`RELEASES_TOKEN` secret on the `void-client` repo.

Required token scope:

- **Resource owner:** `2011Scape`
- **Repository access:** only `void-client-releases`
- **Permissions:** `Contents: Read and write`

Rotate the token by regenerating it at
https://github.com/settings/personal-access-tokens and updating the
`RELEASES_TOKEN` secret under Settings → Secrets and variables → Actions.
