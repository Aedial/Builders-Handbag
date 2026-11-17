# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project adheres to Semantic Versioning.

- Keep a Changelog: https://keepachangelog.com/en/1.1.0/
- Semantic Versioning: https://semver.org/spec/v2.0.0.html


## [1.3.0] - 2026-12-??
### Added
- Add LittleTiles integration.
- Add Chisels & Bits integration.


## [1.2.0] - 2026-11-22
### Added
- Retain ArchitectureCraft-Renovated texture orientation when picking and placing blocks.
- Add a config option for unlimited configured blocks in creative mode.
- Show a placement ghost with rotation and connections while holding the Handbag in Normal or Sequential mode.
- Show an error log on opening the GUI when no configurations are available.
- Allow the Handbag to refill from sub-inventories, like a bag or /dank/null, with Baubles support.
- Allow the Handbag to be used as a supplier for Building Gadgets or Better Builders Wands. These specific integrations rely on mixins and can be disable in config.
- Add in-world configuration picking on keybind (default: middle-click).
- Add material re-absorption from dropped items. The handbag will match picked items + do a periodic inventory scan every 30s to reabsorb configured materials into its storage.
- Add "Random" mode, which chooses a random configuration from the handbag upon placement. The "Normal" mode will continue to use the currently selected configuration.
- Add "Sequential" mode, which cycles to the next configuration in the handbag after each successful placement.
- Add a crafting recipe to copy configurations into an unconfigured handbag.
- Add the FutureMC integration, which allows the handbag to store and place blocks corresponding to the FutureMC stonecutter.
- Add a config to automatically migrate FutureMC recipes, when their ratio are changed. If enabled, the remainder will be rounded down to the nearest multiple of the new ratio. If not, the configuration will need to be manually removed and re-added to the handbag.

### Fixed
- Allow removed integrations to still persist their configurations, so that material is not lost if the user temporarily removes a mod or wants to recover the blocks. The material will not be selectable and will render as "invalid" in the GUI, but will be restored once the integration is added back.
- Fix GUI crashing when Chisel is not present.

### Changed
- Change the right-click behavior to open the GUI only on shift-click, instead of "whenever no block is placed". The old behavior can be restored in config.
- ArchitectureCraft will now consume the true fractional amount of materials required for its blocks, rather than 1 whole block per placement.


## [1.1.1] - 2026-09-16
### Fixed
- Optimize the rendering of the handbag, to reduce the unnecessary load on the client.
- Fix ArchitectureCraft blocks messing with smooth lighting, when currently selected.


## [1.1.0] - 2026-08-21
### Added
- Add contents overlay rendering above the experience bar, to show the contents of the currently selected configuration and adjacent configurations.

### Fixed
- Fix handbags with no stored configurations being placed instead of showing an error message when used.


## [1.0.1] - 2026-08-20
### Fixed
- Fix the "Linked"/"Unlinked" tooltip text for AE2 integration not being there.
- Fix ArchitectureCraft sometimes not rendering correctly after placement (rendering as a default wooden slope) until a neighbor update occurs.


## [1.0.0] - 2026-08-19
### Added
- Initial release of Builder's Handbag :
    - A portable builder's tool for saving, selecting, and placing up to 36 different decorative block configurations.
    - Per-configuration material storage, with automatic refilling from the player's inventory / wireless AE2 if linked.
    - Sneak-scroll configuration switching and in-screen configuration management.
    - Optional Chisel, ArchitectureCraft, and Blockcraftery decoration support.
    - Texture provided by a TESR model, with the selected configuration rendered in the model.
