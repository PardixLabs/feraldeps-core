package com.pardixlabs.feraldeps;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class Main {

    public static void main(String[] args) throws Exception {
        // If no arguments, launch GUI
        if (args.length == 0) {
            GuiMain.main(args);
            return;
        }

        // Otherwise run CLI mode
        String pomPath = null;
        String htmlPath = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--html".equals(arg) && i + 1 < args.length) {
                htmlPath = args[++i];
            } else if ("--cvss-debug".equals(arg)) {
                VulnerabilityDatabase.setCvssDebug(true);
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsageAndExit();
            } else if (!arg.startsWith("--") && pomPath == null) {
                pomPath = arg;
            }
        }

        if (pomPath == null) {
            printUsageAndExit();
        }

        File buildFile = new File(pomPath);
        List<Dependency> deps = parseBuildFile(buildFile);
        List<ScanResult> results = new java.util.ArrayList<>();

        System.out.println("FeralDeps scan results:\n");
        System.out.println("Scanning build file: " + buildFile.getName());
        if (buildFile.getName().equals("pom.xml")) {
            System.out.println("Maven parent POM properties are resolved for direct dependencies.\n");
        } else if (buildFile.getName().equals("package.json")) {
            System.out.println("JavaScript dependencies are resolved from package.json direct sections.\n");
        }

        for (Dependency dep : deps) {
            System.out.println("• " + dep.coordinate() + ":" + dep.version);
            System.out.println("  Scope: " + dep.scope);
            System.out.println("  Version Constraint: " + dep.getVersionConstraintType());

            String latest = VulnerabilityDatabase.latestVersion(dep).orElse(null);
            if (latest != null && !latest.equals(dep.version)) {
                System.out.println("  Outdated → latest: " + latest);
                System.out.println("  Severity (outdated): " + dep.getOutdatedSeverityScore(latest) + "/10");
                if (dep.isVersionLocked()) {
                    System.out.println("  WARNING: Version is locked - upgrading may require code changes");
                }
                System.out.println("  REMEDIATION: Update pom.xml version to: <version>" + latest + "</version>");
            }

            boolean isVulnerable = VulnerabilityDatabase.isVulnerable(dep);
            Double cvssScore = null;
            String cvssSource = null;
            Optional<VulnerabilityDatabase.CvssResult> cvss = VulnerabilityDatabase.getCvssResult(dep);
            if (cvss.isPresent()) {
                cvssScore = cvss.get().score;
                cvssSource = cvss.get().source;
                System.out.println("  CVSS Score: " + cvssScore + " (" + cvssSource + ")");
            }
            if (isVulnerable) {
                System.out.println("  WARNING: Known vulnerable version");
                if (dep.isVersionLocked()) {
                    System.out.println("  CRITICAL: Vulnerable version is LOCKED - upgrade blocked by version constraint");
                }
                
                // Get specific remediation information from OSV
                VulnerabilityDatabase.getRemediationInfo(dep).ifPresent(remediation -> {
                    if (remediation.hasRemediation && !remediation.fixedVersions.isEmpty()) {
                        System.out.println("  REMEDIATION: URGENT - Upgrade to a secure version:");
                        for (String fixedVer : remediation.fixedVersions) {
                            System.out.println("     • <version>" + fixedVer + "</version> (fixes known vulnerabilities)");
                        }
                        if (!remediation.summary.isEmpty()) {
                            System.out.println("     Issue: " + remediation.summary);
                        }
                        System.out.println("     Steps:");
                        System.out.println("       1. Update version in pom.xml");
                        System.out.println("       2. Run: mvn clean install");
                        System.out.println("       3. Test your application thoroughly");
                    } else {
                        // Fallback to latest version if no specific fix versions found
                        VulnerabilityDatabase.latestVersion(dep.coordinate())
                            .ifPresent(latestVersion -> {
                                System.out.println("  REMEDIATION: URGENT - Update to latest version: <version>" + latestVersion + "</version>");
                                System.out.println("     1. Update version in pom.xml");
                                System.out.println("     2. Run: mvn clean install");
                                System.out.println("     3. Test your application thoroughly");
                            });
                    }
                });
                
                // If no remediation info available at all
                if (VulnerabilityDatabase.getRemediationInfo(dep).isEmpty()) {
                    System.out.println("  REMEDIATION: Check https://mvnrepository.com/artifact/" + dep.groupId + "/" + dep.artifactId + " for secure alternatives");
                }
            }

            results.add(new ScanResult(dep, latest, isVulnerable, false, buildFile, cvssScore, cvssSource));

            System.out.println();
        }

        if (htmlPath != null) {
            HtmlReportGenerator.writeHtmlReport(results, new File(htmlPath), buildFile.getParentFile());
            System.out.println("HTML report written to: " + htmlPath);
        }
    }

    private static List<Dependency> parseBuildFile(File buildFile) throws Exception {
        String name = buildFile.getName();
        if (name.equals("pom.xml")) {
            return PomParser.parse(buildFile);
        }
        if (name.equals("build.gradle") || name.equals("build.gradle.kts")) {
            return GradleParser.parse(buildFile);
        }
        if (name.equals("package.json")) {
            return NpmParser.parse(buildFile);
        }
        throw new IllegalArgumentException("Unsupported build file: " + buildFile.getName());
    }

    private static void printUsageAndExit() {
        System.out.println("Usage: java -jar feraldeps.jar <path/to/pom.xml|build.gradle|build.gradle.kts|package.json> [--html <output.html>] [--cvss-debug]");
        System.exit(1);
    }
}
