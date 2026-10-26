# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project adheres to Semantic Versioning.

- Keep a Changelog: https://keepachangelog.com/en/1.1.0/
- Semantic Versioning: https://semver.org/spec/v2.0.0.html


## [1.2.0] - 2026-10-30
### Added
- Show an error log on opening the GUI when no configurations are available.
- Allow the Handbag to refill from sub-inventories, like a bag or /dank/null, with Baubles support.
- Allow the Handbag to be used as a supplier for Building Gadgets or Better Builders Wands.
- Add in-world configuration picking on keybind (default: middle-click).

### Fixed
- Allow removed integrations to still persist their configurations, so that material is not lost if the user temporarily removes a mod or wants to recover the blocks. The material will not be selectable and will render as "invalid" in the GUI, but will be restored once the integration is added back.
- Fix GUI crashing when Chisel is not present.

### Changed
- Change the right-click behavior to open the GUI only on shift-click, instead of "whenever no block is placed". The old behavior can be restored in config.


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
