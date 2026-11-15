# Builder's Handbag

A Minecraft 1.12.2 building tool for easily switching between up to 36 decoration configurations, in a similar way to a dank/null. Each configuration has an internal inventory, drawing from the player's inventory (including any bag) or a linked AE2 network when it runs out of materials.

The Handbag can act as a supplier for Building Gadgets or Better Builders Wands, providing materials from its internal inventory. For that, it needs a mixin loader, like [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) / Cleanroom 0.6+.

With the handbag in your main hand, middle-click (configurable keybind) a supported block to add its configuration.

The handbag will automatically re-absorb matching configured materials and their underlying block from dropped items, as well as periodically scan the player's inventory to reabsorb configured materials into its storage (but not their underlying block). The re-absorption process will skip configurations that are full or invalid, but the periodic scan may absorb them later, when space becomes available.

## Optional integrations

- **Chisel** for carved block variants.
- **ArchitectureCraft** for shape variants.
- **Blockcraftery** for editable blocks.
- **Future-MC** for stonecutter recipes (including custom ones).
- **AE2** for wireless material refilling.

Each can be enabled or disabled in the server config.

## Other configs
- **Clean-up BlockCraftery Frames**: When picking up a matching BlockCraftery frame, automatically void it (to avoid cluttering the inventory). The original material is dropped separately and will be re-absorbed by the handbag if there is space. The frames may be cleaned during the periodic inventory scan as well, if they somehow bypassed the initial pick-up absorption.
- **Old Open Behavior**: Restore the old right-click behavior for opening the handbag GUI, instead of requiring shift-click. This behavior can open the GUI at inopportune times, but may be preferred by some players.
- **Pick-up Absorption**: Automatically absorb configured materials and exact configured blocks when the player picks them up.
- **Inventory Cleanup**: Periodically scan the player's inventory to reabsorb configured materials into the handbag's storage.
- **Mixin Options**: Enable or disable mixins for Building Gadgets and Better Builders Wands integrations. Requires a restart to take effect.
- **Automatic Ratio Migration**: Enable automatic migration of changed Future-MC recipe ratios. If it is not enabled, changed configuration must be manually removed and reconfigured. The migration preserves whole materials and rounds the fractional remainder down to the new denominator.

## Mixin use
The mod uses mixins to allow Better Builders Wands and Building Gadgets to source materials from the handbag's internal inventory and apply them correctly during block placement. Neither of these mods support the necessary NBT matching, which is necessary for ArchitectureCraft/Blockcraftery integrations.

You can disable these mixins in the server config if you do not want the handbag to interact with Building Gadgets or Better Builders Wands, or the mixins do not work with your version of the mods.

## Installation
Add the mod to both client and server.

## License

This project is licensed under the [MIT License](LICENSE).

## Credits
- Chinese translation: @ZHAY10086
