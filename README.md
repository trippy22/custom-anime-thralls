# Custom Anime Thralls

A cosmetic RuneLite plugin that replaces Arceuus thralls with custom anime characters.
Choose a character and **flat pixel art**, **3D extrusion**, or **OSRS-style model** for ghost,
skeleton, and zombie thralls, across all summon tiers. Adjust their height and toggle
idle, walking, and attack motion. Changes apply to visible thralls in your client only.
OSRS-style models are the default for all three thrall types.

Flat models face the camera. Extrusions add thickness to the artwork. OSRS-style models
use rounded, faceted geometry and adjusted humanoid proportions for every character;
depth and rear colors are inferred from the front artwork. These are procedural models
with bob/hop motion, without articulated animation. **Original game thrall** disables replacement.

Bundled mesh JSON uses `geometryEncoding: "delta-v1"`: each vertex coordinate and
triangle index is stored as a difference from the preceding row in the same column,
starting at zero. The resource reader restores the exact geometry off the client thread.
This reduces archive size without changing model detail or requiring extra libraries.

Built for the RuneLite Plugin Hub; availability requires upstream approval.
Use Java 11+ and `./gradlew run` to launch the development client. `CustomThrallsPluginTest`
is its manual launcher, not an automated test. `./gradlew test` runs any locally present
verification tests; those tests are not committed. Jagex accounts should follow RuneLite's
[development login guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

In IntelliJ, import the Gradle project and run `CustomThrallsPluginTest.main()` with
`-ea` in VM options. The default launch loads **Custom Anime Thralls** without developer mode.
For the optional preview, add `--preview` to program arguments, or run
`./gradlew run --args="--preview"`. This enables developer mode and registers both plugins.
Enable **Custom Thralls Preview** in that client to inspect any character
beside your player without summoning thralls. It supports all three styles, size, and simulated
idle/walk/attack motion. Actual NPC replacement requires a visible thrall in-game;
the preview alone does not validate that behavior.
The preview is development-only and is excluded from the regular plugin JAR.

Completed models are cached by resource and height (at most three in the live plugin).
Character/style/height changes and shutdown clear the cache; position and motion remain
independent per thrall.

Licensed under [BSD 2-Clause](LICENSE). Reused code is credited in [NOTICE](NOTICE).
