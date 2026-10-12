# Hitbox Visuals — Minecraft 1.21.4 (Fabric)

Client-side Fabric mod source project for Minecraft Java 1.21.4 and Java 21.

## Controls

- `H` by default — toggle hitboxes.
- `K` by default — open the mod settings.
- Change both in **Options → Controls → Key Binds**, under **Hitbox Visuals**.

## Visibility rules

- Players without invisibility: show a hitbox only when a block raycast can see part of the player; fully occluded players are skipped.
- Invisible players with at least one equipped armor piece: same visibility rule; no through-wall rendering.
- Invisible players with no equipped armor: never show a hitbox.
- There is no through-walls mode.

## Build locally

Requires JDK 21 and internet access. Install Gradle 8.10.2 or use the GitHub Actions workflow included in `.github/workflows/gradle.yml`.

Run from the project root:

```bash
gradle build
```

The JAR should appear in `build/libs/`.

## GitHub Actions

The included workflow installs Java 21 and Gradle 8.10.2 itself, so a `gradlew` wrapper file is not required. It uploads the built JAR as an Actions artifact when the build succeeds.

Note: the source project still needs to pass the actual CI compilation; a successful download/link alone does not mean it has been tested in Minecraft.
