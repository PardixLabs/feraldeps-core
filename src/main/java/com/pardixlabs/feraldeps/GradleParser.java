package com.pardixlabs.feraldeps;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleParser {
    private static final Pattern GROOVY_DEP_PATTERN = Pattern.compile("^\\s*([a-zA-Z]+)\\s+['\"]([^:'\"]+):([^:'\"]+):([^'\"]+)['\"]\\s*$");
    private static final Pattern KOTLIN_DEP_PATTERN = Pattern.compile("^\\s*([a-zA-Z]+)\\(\\s*['\"]([^:'\"]+):([^:'\"]+):([^'\"]+)['\"]\\s*\\)\\s*$");
    private static final Pattern GROOVY_VAR_PATTERN = Pattern.compile("^\\s*(?:ext\\.)?([A-Za-z0-9_.-]+)\\s*=\\s*['\"]([^'\"]+)['\"]\\s*$");
    private static final Pattern KOTLIN_VAL_PATTERN = Pattern.compile("^\\s*val\\s+([A-Za-z0-9_.-]+)\\s*=\\s*['\"]([^'\"]+)['\"]\\s*$");
    private static final Pattern KOTLIN_EXTRA_PATTERN = Pattern.compile("^\\s*extra\\[\\\"([^\\\"]+)\\\"\\]\\s*=\\s*['\"]([^'\"]+)['\"]\\s*$");

    public static List<Dependency> parse(File buildFile) throws Exception {
        List<Dependency> deps = new ArrayList<>();
        List<String> lines = Files.readAllLines(buildFile.toPath(), StandardCharsets.UTF_8);

        Map<String, String> variables = parseVariables(lines);

        for (String line : lines) {
            Dependency dep = parseLine(line, variables);
            if (dep != null) {
                deps.add(dep);
            }
        }

        return deps;
    }

    private static Dependency parseLine(String line, Map<String, String> variables) {
        Matcher groovy = GROOVY_DEP_PATTERN.matcher(line);
        if (groovy.matches()) {
            String scope = groovy.group(1);
            String groupId = groovy.group(2);
            String artifactId = groovy.group(3);
            String version = resolveVersion(groovy.group(4), variables);
            Dependency dep = new Dependency(groupId, artifactId, version);
            dep.scope = scope;
            return dep;
        }

        Matcher kotlin = KOTLIN_DEP_PATTERN.matcher(line);
        if (kotlin.matches()) {
            String scope = kotlin.group(1);
            String groupId = kotlin.group(2);
            String artifactId = kotlin.group(3);
            String version = resolveVersion(kotlin.group(4), variables);
            Dependency dep = new Dependency(groupId, artifactId, version);
            dep.scope = scope;
            return dep;
        }

        return null;
    }

    private static Map<String, String> parseVariables(List<String> lines) {
        Map<String, String> vars = new HashMap<>();
        for (String line : lines) {
            Matcher groovyVar = GROOVY_VAR_PATTERN.matcher(line);
            if (groovyVar.matches()) {
                vars.put(groovyVar.group(1), groovyVar.group(2));
                continue;
            }

            Matcher kotlinVal = KOTLIN_VAL_PATTERN.matcher(line);
            if (kotlinVal.matches()) {
                vars.put(kotlinVal.group(1), kotlinVal.group(2));
                continue;
            }

            Matcher kotlinExtra = KOTLIN_EXTRA_PATTERN.matcher(line);
            if (kotlinExtra.matches()) {
                vars.put(kotlinExtra.group(1), kotlinExtra.group(2));
            }
        }
        return vars;
    }

    private static String resolveVersion(String version, Map<String, String> variables) {
        if (version == null) return null;
        String resolved = version;

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            resolved = resolved.replace("${" + key + "}", value);
            resolved = resolved.replace("$" + key, value);
        }

        return resolved;
    }
}
