package com.pardixlabs.feraldeps;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HtmlReportGenerator {

    public static void writeHtmlReport(List<ScanResult> results, File outputFile, File projectRoot) throws IOException {
        if (results == null) {
            results = new ArrayList<>();
        }

        Map<String, List<ScanResult>> byPom = new LinkedHashMap<>();
        for (ScanResult result : results) {
            String pomPath = result.pomFile != null ? result.pomFile.getAbsolutePath() : "Unknown build file";
            byPom.computeIfAbsent(pomPath, k -> new ArrayList<>()).add(result);
        }

        int total = results.size();
        int outdated = 0;
        int vulnerable = 0;
        int upToDate = 0;
        int noInfo = 0;
        int ignored = 0;

        int stdLow = 0;      // 1-3
        int stdMedium = 0;   // 4-6
        int stdHigh = 0;     // 7-8
        int stdCritical = 0; // 9-10

        int cvssLow = 0;     // 0.1-3.9
        int cvssMedium = 0;  // 4.0-6.9
        int cvssHigh = 0;    // 7.0-8.9
        int cvssCritical = 0;// 9.0-10.0

        for (ScanResult result : results) {
            if (result.isIgnored) {
                ignored++;
                continue;
            }
            if (result.isVulnerable) vulnerable++;
            if (!result.hasVersionInfo()) {
                noInfo++;
            } else if (result.isOutdated()) {
                outdated++;
                int score = result.getSeverityScore();
                if (score >= 9) {
                    stdCritical++;
                } else if (score >= 7) {
                    stdHigh++;
                } else if (score >= 4) {
                    stdMedium++;
                } else if (score > 0) {
                    stdLow++;
                }
            } else {
                upToDate++;
            }

            if (result.cvssScore != null) {
                double score = result.cvssScore;
                if (score >= 9.0) {
                    cvssCritical++;
                } else if (score >= 7.0) {
                    cvssHigh++;
                } else if (score >= 4.0) {
                    cvssMedium++;
                } else if (score > 0.0) {
                    cvssLow++;
                }
            }
        }

        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset='utf-8'>");
        html.append("<title>FeralDeps Report</title>");
        html.append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>");
        html.append("<style>");
        html.append("body{font-family:Arial,Helvetica,sans-serif;margin:24px;color:#222;}");
        html.append("h1{margin-bottom:4px;} .muted{color:#666;font-size:12px;}");
        html.append(".summary{display:flex;gap:16px;flex-wrap:wrap;margin:16px 0;}");
        html.append(".card{border:1px solid #ddd;border-radius:8px;padding:12px;min-width:160px;background:#fafafa;}");
        html.append("table{width:100%;border-collapse:collapse;margin:12px 0 24px 0;}");
        html.append("th,td{border:1px solid #e0e0e0;padding:8px;text-align:left;font-size:13px;}");
        html.append("th{background:#f5f5f5;}");
        html.append(".tag{padding:2px 6px;border-radius:4px;font-size:12px;}");
        html.append(".tag.outdated{background:#fff3cd;color:#856404;}");
        html.append(".tag.vulnerable{background:#f8d7da;color:#721c24;}");
        html.append(".tag.uptodate{background:#d4edda;color:#155724;}");
        html.append(".tag.noinfo{background:#e2e3e5;color:#383d41;}");
        html.append(".tag.ignored{background:#e0f0ff;color:#0c5460;}");
        html.append(".tabs{display:flex;gap:8px;margin:16px 0 12px 0;}");
        html.append(".tab-btn{padding:8px 12px;border:1px solid #ddd;border-radius:6px;background:#fafafa;cursor:pointer;font-size:13px;}");
        html.append(".tab-btn.active{background:#e8f2ff;border-color:#c7dcff;}");
        html.append(".tab-content{display:none;}");
        html.append(".tab-content.active{display:block;}");
        html.append(".chart-wrap{width:100%;max-width:720px;margin-top:12px;}");
        html.append("canvas{width:100% !important;height:auto !important;}");
        html.append("</style></head><body>");

        html.append("<h1>FeralDeps Report</h1>");
        html.append("<div class='muted'>Generated: ").append(escapeHtml(generatedAt)).append("</div>");
        if (projectRoot != null) {
            html.append("<div class='muted'>Project: ").append(escapeHtml(projectRoot.getAbsolutePath())).append("</div>");
        }

        html.append("<details style='margin-top:12px;'>");
        html.append("<summary><strong>Info: Build File Scanning</strong></summary>");
        html.append("<ul>");
        html.append("<li>Scans direct dependencies from pom.xml, build.gradle, build.gradle.kts, and package.json</li>");
        html.append("<li>Maven parent POM properties are resolved for direct dependencies</li>");
        html.append("</ul>");
        html.append("</details>");

        html.append("<details style='margin-top:12px;'>");
        html.append("<summary><strong>Info: Outdated Severity Scoring</strong></summary>");
        html.append("<ul>");
        html.append("<li>If major version behind: 9/10</li>");
        html.append("<li>Else if minor behind: 6/10</li>");
        html.append("<li>Else if patch behind: 3/10</li>");
        html.append("<li>Unknown version format but outdated: 5/10</li>");
        html.append("<li>Same or ahead: 0/10</li>");
        html.append("</ul>");
        html.append("</details>");

        html.append("<div class='summary'>");
        html.append(card("Total", String.valueOf(total)));
        html.append(card("Outdated", String.valueOf(outdated)));
        html.append(card("Vulnerable", String.valueOf(vulnerable)));
        html.append(card("Up-to-date", String.valueOf(upToDate)));
        html.append(card("No info", String.valueOf(noInfo)));
        html.append(card("Ignored", String.valueOf(ignored)));
        html.append("</div>");

        html.append("<div class='tabs'>");
        html.append("<button class='tab-btn active' data-tab='tab-report'>Report</button>");
        html.append("<button class='tab-btn' data-tab='tab-charts'>Charts</button>");
        html.append("</div>");

        html.append("<div id='tab-report' class='tab-content active'>");

        for (Map.Entry<String, List<ScanResult>> entry : byPom.entrySet()) {
            html.append("<h2>").append(escapeHtml(entry.getKey())).append("</h2>");
            html.append("<table>");
            html.append("<thead><tr>");
            html.append("<th>Dependency</th>");
            html.append("<th>Scope</th>");
            html.append("<th>Current</th>");
            html.append("<th>Latest</th>");
            html.append("<th>Status</th>");
            html.append("<th>Severity (Outdated)</th>");
            html.append("<th>CVSS</th>");
            html.append("<th>Vulnerable</th>");
            html.append("<th>Recommended</th>");
            html.append("</tr></thead><tbody>");

            for (ScanResult result : entry.getValue()) {
                Dependency dep = result.dependency;
                String status = buildStatus(result);
                String severity = result.isOutdated() ? String.valueOf(result.getSeverityScore()) + "/10" : "-";
                String cvss = result.cvssScore != null
                    ? String.valueOf(result.cvssScore) + (result.cvssSource != null ? " (" + result.cvssSource + ")" : "")
                    : "-";
                String vulnerableText = result.isVulnerable ? "Yes" : "No";
                String recommended = buildRecommendedVersion(result);

                html.append("<tr>");
                html.append("<td>").append(escapeHtml(dep.coordinate())).append("</td>");
                html.append("<td>").append(escapeHtml(dep.scope)).append("</td>");
                html.append("<td>").append(escapeHtml(dep.version)).append("</td>");
                html.append("<td>").append(escapeHtml(result.latestVersion != null ? result.latestVersion : "-")).append("</td>");
                html.append("<td>").append(status).append("</td>");
                html.append("<td>").append(escapeHtml(severity)).append("</td>");
                html.append("<td>").append(escapeHtml(cvss)).append("</td>");
                html.append("<td>").append(escapeHtml(vulnerableText)).append("</td>");
                html.append("<td>").append(escapeHtml(recommended)).append("</td>");
                html.append("</tr>");
            }

            html.append("</tbody></table>");
        }

        html.append("</div>");

        html.append("<div id='tab-charts' class='tab-content'>");
        html.append("<h2>Charts</h2>");
        html.append("<label for='chartSelect' class='muted'>Select chart</label><br>");
        html.append("<select id='chartSelect'>");
        html.append("<option value='status'>Status (Outdated/Vulnerable/Up-to-date/No info/Ignored)</option>");
        html.append("<option value='standard'>Standard severity (Outdated)</option>");
        html.append("<option value='cvss'>CVSS severity</option>");
        html.append("</select>");
        html.append("<div class='chart-wrap'><canvas id='chartCanvas'></canvas></div>");
        html.append("</div>");

        html.append("<script>");
        html.append("document.querySelectorAll('.tab-btn').forEach(btn=>{btn.addEventListener('click',()=>{");
        html.append("document.querySelectorAll('.tab-btn').forEach(b=>b.classList.remove('active'));" );
        html.append("document.querySelectorAll('.tab-content').forEach(c=>c.classList.remove('active'));" );
        html.append("btn.classList.add('active');" );
        html.append("document.getElementById(btn.dataset.tab).classList.add('active');" );
        html.append("});});");

        html.append("const chartData={");
        html.append("status:{type:'pie',labels:['Outdated','Vulnerable','Up-to-date','No info','Ignored'],");
        html.append("data:[").append(outdated).append(",").append(vulnerable).append(",").append(upToDate).append(",").append(noInfo).append(",").append(ignored).append("],");
        html.append("colors:['#fff3cd','#f8d7da','#d4edda','#e2e3e5','#e0f0ff'],title:'Status'},");
        html.append("standard:{type:'doughnut',labels:['Low (1-3)','Medium (4-6)','High (7-8)','Critical (9-10)'],");
        html.append("data:[").append(stdLow).append(",").append(stdMedium).append(",").append(stdHigh).append(",").append(stdCritical).append("],");
        html.append("colors:['#cfe2ff','#ffe5b4','#ffb3b3','#ff7b7b'],title:'Standard Severity (Outdated)'} ,");
        html.append("cvss:{type:'doughnut',labels:['Low (0.1-3.9)','Medium (4.0-6.9)','High (7.0-8.9)','Critical (9.0-10)'],");
        html.append("data:[").append(cvssLow).append(",").append(cvssMedium).append(",").append(cvssHigh).append(",").append(cvssCritical).append("],");
        html.append("colors:['#cfe2ff','#ffe5b4','#ffb3b3','#ff7b7b'],title:'CVSS Severity'}");
        html.append("};");

        html.append("let activeChart=null;\n");
        html.append("function renderChart(key){const cfg=chartData[key];");
        html.append("const ctx=document.getElementById('chartCanvas');");
        html.append("if(activeChart){activeChart.destroy();}\n");
        html.append("activeChart=new Chart(ctx,{type:cfg.type,data:{labels:cfg.labels,datasets:[{data:cfg.data,backgroundColor:cfg.colors}]},");
        html.append("options:{responsive:true,maintainAspectRatio:true,plugins:{legend:{position:'bottom'},title:{display:true,text:cfg.title}}}});}");
        html.append("const select=document.getElementById('chartSelect');\n");
        html.append("renderChart(select.value);\n");
        html.append("select.addEventListener('change',()=>renderChart(select.value));\n");
        html.append("</script>");

        html.append("</body></html>");

        Files.write(outputFile.toPath(), html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String buildStatus(ScanResult result) {
        if (result.isIgnored) {
            return "<span class='tag ignored'>Ignored</span>";
        }
        if (result.isVulnerable) {
            return "<span class='tag vulnerable'>Vulnerable</span>";
        }
        if (!result.hasVersionInfo()) {
            return "<span class='tag noinfo'>No info</span>";
        }
        if (result.isOutdated()) {
            return "<span class='tag outdated'>Outdated</span>";
        }
        return "<span class='tag uptodate'>Up-to-date</span>";
    }

    private static String buildRecommendedVersion(ScanResult result) {
        if (!result.isVulnerable && !result.isOutdated()) {
            return "-";
        }

        String minimalFix = null;
        if (result.isVulnerable) {
            Optional<VulnerabilityDatabase.RemediationInfo> remediation = VulnerabilityDatabase.getRemediationInfo(result.dependency);
            if (remediation.isPresent() && !remediation.get().fixedVersions.isEmpty()) {
                minimalFix = remediation.get().fixedVersions.get(0);
            }
        }

        String latest = result.latestVersion;

        if (minimalFix != null && latest != null) {
            return "Minimal fixed: " + minimalFix + " | Latest: " + latest;
        }
        if (minimalFix != null) {
            return "Minimal fixed: " + minimalFix;
        }
        if (latest != null) {
            return "Latest: " + latest;
        }

        return "-";
    }

    private static String card(String title, String value) {
        return "<div class='card'><div class='muted'>" + escapeHtml(title) + "</div><div><strong>" +
                escapeHtml(value) + "</strong></div></div>";
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
