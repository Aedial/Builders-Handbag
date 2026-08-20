# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project adheres to Semantic Versioning.

- Keep a Changelog: https://keepachangelog.com/en/1.1.0/
- Semantic Versioning: https://semver.org/spec/v2.0.0.html


## [1.0.2] - 2026-08-21
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
