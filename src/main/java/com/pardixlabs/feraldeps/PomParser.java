package com.pardixlabs.feraldeps;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class PomParser {

    public static List<Dependency> parse(File pomFile) throws Exception {
        List<Dependency> deps = new ArrayList<>();

        DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(pomFile);

        // Parse properties first (including parent properties)
        Map<String, String> properties = parsePropertiesWithParents(pomFile, doc, new ArrayList<>());

        NodeList nodes = doc.getElementsByTagName("dependency");

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);

            String groupId = getTag(el, "groupId");
            String artifactId = getTag(el, "artifactId");
            String version = getTag(el, "version");
            String scope = getTag(el, "scope"); // Get scope if specified

            if (version != null) {
                // Resolve property placeholders
                version = resolveProperties(version, properties);
                Dependency dep = new Dependency(groupId, artifactId, version);
                if (scope != null) {
                    dep.scope = scope;
                }
                deps.add(dep);
            }
        }

        return deps;
    }

    private static Map<String, String> parseProperties(Document doc) {
        Map<String, String> properties = new HashMap<>();
        
        NodeList propertiesNodes = doc.getElementsByTagName("properties");
        if (propertiesNodes.getLength() > 0) {
            Element propertiesEl = (Element) propertiesNodes.item(0);
            NodeList children = propertiesEl.getChildNodes();
            
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element) {
                    Element prop = (Element) children.item(i);
                    properties.put(prop.getTagName(), prop.getTextContent().trim());
                }
            }
        }
        
        return properties;
    }

    private static Map<String, String> parsePropertiesWithParents(File pomFile, Document doc, List<File> visited) throws Exception {
        Map<String, String> properties = new HashMap<>();

        // Avoid cycles
        if (visited.contains(pomFile)) {
            return properties;
        }
        visited.add(pomFile);

        // Load parent properties first
        File parentPom = findParentPom(pomFile, doc);
        if (parentPom != null && parentPom.exists()) {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document parentDoc = builder.parse(parentPom);
            properties.putAll(parsePropertiesWithParents(parentPom, parentDoc, visited));
        }

        // Child overrides parent
        properties.putAll(parseProperties(doc));

        return properties;
    }

    private static File findParentPom(File pomFile, Document doc) {
        NodeList parentNodes = doc.getElementsByTagName("parent");
        if (parentNodes.getLength() == 0) return null;

        Element parentEl = (Element) parentNodes.item(0);
        String relPath = getTag(parentEl, "relativePath");

        File baseDir = pomFile.getParentFile();
        if (relPath == null || relPath.trim().isEmpty()) {
            relPath = "../pom.xml";
        }

        File parentPom = new File(baseDir, relPath).getAbsoluteFile();
        return parentPom.exists() ? parentPom : null;
    }

    private static String resolveProperties(String value, Map<String, String> properties) {
        if (value == null) return null;
        
        // Replace ${property.name} with actual value
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            value = value.replace(placeholder, entry.getValue());
        }
        
        return value;
    }

    private static String getTag(Element el, String tag) {
        NodeList list = el.getElementsByTagName(tag);
        if (list.getLength() == 0) return null;
        return list.item(0).getTextContent().trim();
    }
}
