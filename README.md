# Custom Anime Thralls

A cosmetic RuneLite plugin that replaces the appearance of Arceuus thralls with anime
characters. Supports ghost, skeleton, and zombie thralls at every summon tier.
Changes are visible only to you and do not affect gameplay.

In **Custom Anime Thralls** settings:

- Choose a character separately for **Ghost**, **Skeleton**, and **Zombie**.
- Choose each thrall's style: **OSRS-style model** (default), **3D extrusion**, or
  **Flat pixel art** that faces the camera.
- Adjust **Model height** and toggle **Animate** for gentle bobbing and attack hops.
- Select **Original game thrall** to keep a thrall type's normal appearance.

Settings apply to visible thralls, including other players' summons. The plugin changes
existing thralls; it does not summon them or unlock the spells. Motion uses bobbing and
hopping rather than moving arms and legs.

On first use, the plugin downloads all character models from this project's GitHub
Releases and caches them locally. Cached models work offline; a new model release needs another download.
If the initial download fails, original thralls remain visible. Check your connection,
wait a minute, and toggle the plugin to retry.

## Development

With Java 11+, run `./gradlew run` to launch RuneLite with the plugin.
Use `./gradlew run --args="--preview"` to preview characters without summoning thralls.
In IntelliJ, run `CustomThrallsPluginTest.main()` with `-ea` in VM options.

Licensed under [BSD 2-Clause](LICENSE). Reused code is credited in [NOTICE](NOTICE).
