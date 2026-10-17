# Dupe Patches

Closes item duplication glitches that still work in vanilla Minecraft, each with its own switch. For NeoForge 1.21.1, 1.21.11, 26.2 and 26.3.

NeoForge port of [Panda Anti-Dupe](https://github.com/TreeOfSelf/PandaAntiDupe) by TreeOfSelf, which is Fabric only. Only dupes that a game test can still reproduce on vanilla are ported; the rest are either fixed in vanilla already or could not be reproduced, and are left out.

| Patch | Versions | Default | What it stops |
|---|---|---|---|
| `trident` | 1.21.1 | on | Swapping a trident you are charging into the crafting grid or a container, then throwing it: the thrown trident is a copy and the original stays behind. |
| `book` | 1.21.1 | on | Signing a book with a title over 32 characters (needs a modified client). The book can't be saved, so the player can't be saved and rolls back on relog while chests keep what they were given. |
| `gravityBlock` | all | off | Sand, gravel, concrete powder and other falling blocks that go through an end portal also land where they left. Vanilla keeps this on purpose and sand dupers rely on it. |

Config: `config/dupepatches.properties`.

## Tests

Every patch has a game test, and a control that runs the same setup with the patch off for that one player or block and has to reproduce the dupe. On versions where vanilla already prevents a dupe, the control checks that instead. CI runs them on all four versions, plus a client start-up test.

## Licence

CC0-1.0, like the original.
