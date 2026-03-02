package com.pardixlabs.feraldeps;

import java.io.File;

public class ScanResult {
    public final Dependency dependency;
    public final String latestVersion;
    public final boolean isVulnerable;
    public final boolean isIgnored;
    public final File pomFile;
    public final Double cvssScore;
    public final String cvssSource;

    public ScanResult(Dependency dependency, String latestVersion, boolean isVulnerable, boolean isIgnored, File pomFile, Double cvssScore, String cvssSource) {
        this.dependency = dependency;
        this.latestVersion = latestVersion;
        this.isVulnerable = isVulnerable;
        this.isIgnored = isIgnored;
        this.pomFile = pomFile;
        this.cvssScore = cvssScore;
        this.cvssSource = cvssSource;
    }

    public boolean hasVersionInfo() {
        return latestVersion != null && !latestVersion.isEmpty();
    }

    public boolean isOutdated() {
        return hasVersionInfo() && !latestVersion.equals(dependency.version);
    }

    public int getSeverityScore() {
        if (!isOutdated()) return 0;
        return dependency.getOutdatedSeverityScore(latestVersion);
    }
}
