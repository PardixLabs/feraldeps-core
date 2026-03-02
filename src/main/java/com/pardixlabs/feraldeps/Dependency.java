package com.pardixlabs.feraldeps;

public class Dependency {
    public String groupId;
    public String artifactId;
    public String version;
    public String scope; // compile, test, provided, runtime, etc.

    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = "compile"; // default scope
    }

    public String coordinate() {
        return groupId + ":" + artifactId;
    }

    /**
     * Checks if the version is locked to a specific version (hard-coded)
     * vs using a version range that allows upgrades
     */
    public boolean isVersionLocked() {
        if (version == null) return false;
        
        // Version ranges use brackets/parentheses: [1.0,2.0), (,1.0], etc.
        // Hard-locked versions are simple: 1.2.3, 1.2.3-SNAPSHOT, etc.
        return !version.contains("[") && 
               !version.contains("]") && 
               !version.contains("(") && 
               !version.contains(")") &&
               !version.contains(",");
    }

    /**
     * Returns a human-readable description of the version constraint
     */
    public String getVersionConstraintType() {
        if (!isVersionLocked()) {
            return "FLEXIBLE (version range allows upgrades)";
        }
        return "LOCKED (specific version pinned)";
    }

    /**
     * Returns a severity score (0-10) for how outdated this dependency is.
     */
    public int getOutdatedSeverityScore(String latestVersion) {
        return calculateOutdatedSeverity(this.version, latestVersion);
    }

    /**
     * Calculate a severity score (0-10) based on version distance.
     */
    public static int calculateOutdatedSeverity(String currentVersion, String latestVersion) {
        if (currentVersion == null || latestVersion == null) return 0;
        if (currentVersion.equals(latestVersion)) return 0;

        int[] current = parseVersionNumbers(currentVersion);
        int[] latest = parseVersionNumbers(latestVersion);

        if (current.length == 0 || latest.length == 0) {
            return 5; // Unknown format but outdated
        }

        int currentMajor = current[0];
        int latestMajor = latest[0];
        int currentMinor = current.length > 1 ? current[1] : 0;
        int latestMinor = latest.length > 1 ? latest[1] : 0;
        int currentPatch = current.length > 2 ? current[2] : 0;
        int latestPatch = latest.length > 2 ? latest[2] : 0;

        if (latestMajor > currentMajor) return 9;
        if (latestMajor < currentMajor) return 0;
        if (latestMinor > currentMinor) return 6;
        if (latestMinor < currentMinor) return 0;
        if (latestPatch > currentPatch) return 3;

        return 1;
    }

    private static int[] parseVersionNumbers(String version) {
        if (version == null) return new int[0];

        String[] parts = version.split("[^0-9]+");
        int count = 0;
        for (String part : parts) {
            if (!part.isEmpty()) count++;
        }
        if (count == 0) return new int[0];

        int[] numbers = new int[count];
        int i = 0;
        for (String part : parts) {
            if (!part.isEmpty()) {
                try {
                    numbers[i++] = Integer.parseInt(part);
                } catch (NumberFormatException ignored) {
                    // skip invalid segment
                }
            }
        }
        return numbers;
    }
}
