# Quantum Client

Quantum Client is a launcher and client for **Minecraft: Java Edition** and **Minecraft: Bedrock Edition**, built as one app.

Pick an edition, pick a version, pick a mod loader, and press Play. Quantum Client downloads everything the game needs, launches it, and loads the Quantum client features into the game.

> **Status:** In early development. Quantum Client is a quality-of-life client. It is not a hacked client, cheat client, or account-stealing tool.

---

## Features (planned)

### Launcher
- **Java and Bedrock in one app**, each with its own tab
- **Selectable versions**: latest, or a specific release (e.g. 1.21.4, 1.19)
- **Mod loaders**: Vanilla (default), Fabric, Quilt, Forge and NeoForge
- **Instances**: create, edit and delete separate game setups
- **Groups**: organise instances into collapsible groups
- **Shared files**: Java runtimes, game versions, libraries and assets are downloaded once and shared between instances
- **Clean deletion**: deleting an instance removes all of its files, plus any shared files no other instance still uses

### Client
- Quantum features injected into the selected game version
- **App-wide settings**: client settings, keybinds and themes belong to the app, not to individual instances, so they apply everywhere

---

## Accounts and authentication

Quantum Client signs in with your **Microsoft account** using Microsoft's own login page, either through your browser or with a device code. **Quantum Client never asks for, sees, or stores your Microsoft password.**

Sign-in is used to confirm that you own Minecraft: Java Edition and to launch the game with your own profile. Login tokens are stored in your operating system's secure keyring, not in plain files.

- **Multiple accounts** with a quick account switcher
- **No offline accounts**: every Java Edition launch uses a signed-in Microsoft account that owns the game
- **Bedrock** doesn't need you to sign in through the launcher. Bedrock handles its own Microsoft (Xbox) sign-in in-game, and you must own the game through its official store.

---

## Platforms

Quantum Client is being built and tested on **CachyOS** first.

| Platform | Status |
|---|---|
| CachyOS | Primary target, in development |
| Other Linux distributions | Planned |
| Windows | Planned |
| macOS | Planned |
| Android | Planned |
| iOS | Planned |

---

## Compliance

Quantum Client is built to follow the [Minecraft EULA](https://www.minecraft.net/eula) and [Minecraft Usage Guidelines](https://www.minecraft.net/usage-guidelines).

- Sign-in uses Microsoft's official authentication, and access to Minecraft services goes through Mojang's app review process.
- Every player must own a legitimate copy of Minecraft. Quantum Client does not bypass ownership checks.
- Quantum Client is a quality-of-life client and does not include cheats or features that give unfair advantages.

Quantum Client is an independent project. Not an official Minecraft product. Not associated with Mojang or Microsoft.
