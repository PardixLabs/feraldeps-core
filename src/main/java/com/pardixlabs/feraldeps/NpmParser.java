package com.pardixlabs.feraldeps;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NpmParser {
    private static final Pattern DEP_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");

    public static List<Dependency> parse(File packageJson) throws Exception {
        String content = Files.readString(packageJson.toPath(), StandardCharsets.UTF_8);

        Map<String, String> dependencies = parseSection(content, "dependencies");
        Map<String, String> devDependencies = parseSection(content, "devDependencies");

        List<Dependency> deps = new ArrayList<>();
        dependencies.forEach((name, version) -> deps.add(new Dependency(name, version, "runtime", "npm")));
        devDependencies.forEach((name, version) -> deps.add(new Dependency(name, version, "dev", "npm")));

        return deps;
    }

    private static Map<String, String> parseSection(String json, String sectionName) {
        Map<String, String> deps = new HashMap<>();
        int sectionIndex = json.indexOf('"' + sectionName + '"');
        if (sectionIndex < 0) {
            return deps;
        }

        int braceOpen = json.indexOf('{', sectionIndex);
        if (braceOpen < 0) {
            return deps;
        }

        int braceClose = findMatchingBrace(json, braceOpen);
        if (braceClose < 0) {
            return deps;
        }

        String sectionBody = json.substring(braceOpen + 1, braceClose);
        Matcher matcher = DEP_PATTERN.matcher(sectionBody);
        while (matcher.find()) {
            String packageName = matcher.group(1);
            String version = matcher.group(2);
            deps.put(packageName, version);
        }

        return deps;
    }

    private static int findMatchingBrace(String text, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--; 
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
