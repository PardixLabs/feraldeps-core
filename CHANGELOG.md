# Changelog

All notable changes to this project will be documented in this file.

## Unreleased

### Added
- JavaScript (npm) dependency scanning support with full ecosystem integration
- Cross-ecosystem CVSS scoring with ecosystem-aware API queries
- Paste buttons in API Credentials dialog for convenient token entry

### Fixed
- NVD query format for npm packages now uses ecosystem-aware keyword search format
- OSV CVSS score parsing improved to handle multiple response formats (severity arrays, database_specific objects, CVSS v3/v4)
- Settings dialog now properly displays all text fields and controls without truncation
- API credentials can now be easily pasted with dedicated paste buttons in the credentials dialog

### Changed
- Dialog sizing increased to accommodate expanded credential entry interface
- Settings dialog resized to 650x320 to fit paste buttons and ensure all text fields are fully visible
- NVD API queries differentiate between Maven (groupId:artifactId:version) and npm (name version) formats
