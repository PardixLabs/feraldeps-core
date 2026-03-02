package com.pardixlabs.feraldeps;

import java.io.*;
import java.util.*;

/**
 * Manages the list of dependencies that should be ignored (hidden) for a specific project.
 * Stores ignored dependencies in a .feraldeps-ignore file in the project directory.
 */
public class IgnoredDependencies {
    private Set<String> ignoredDeps = new HashSet<>();
    private File ignoreFile;

    /**
     * Load ignored dependencies for a specific project folder
     */
    public IgnoredDependencies(File projectFolder) {
        this.ignoreFile = new File(projectFolder, ".feraldeps-ignore");
        load();
    }

    /**
     * Check if a dependency should be ignored
     */
    public boolean isIgnored(Dependency dep) {
        return ignoredDeps.contains(dep.coordinate() + ":" + dep.version);
    }

    /**
     * Add a dependency to the ignore list
     */
    public void ignore(Dependency dep) {
        ignoredDeps.add(dep.coordinate() + ":" + dep.version);
        save();
    }

    /**
     * Remove a dependency from the ignore list
     */
    public void unignore(Dependency dep) {
        ignoredDeps.remove(dep.coordinate() + ":" + dep.version);
        save();
    }

    /**
     * Load ignored dependencies from file
     */
    private void load() {
        if (!ignoreFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(ignoreFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    ignoredDeps.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading ignored dependencies: " + e.getMessage());
        }
    }

    /**
     * Save ignored dependencies to file
     */
    private void save() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(ignoreFile))) {
            writer.println("# FeralDeps - Ignored Dependencies");
            writer.println("# Dependencies listed here will be hidden from scan results");
            writer.println();
            
            List<String> sorted = new ArrayList<>(ignoredDeps);
            Collections.sort(sorted);
            
            for (String dep : sorted) {
                writer.println(dep);
            }
        } catch (IOException e) {
            System.err.println("Error saving ignored dependencies: " + e.getMessage());
        }
    }
}
