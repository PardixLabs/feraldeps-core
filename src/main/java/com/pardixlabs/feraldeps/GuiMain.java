package com.pardixlabs.feraldeps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.io.File;
import java.io.OutputStream;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class GuiMain extends JFrame {
    private JPanel allDepsPanel;
    private JPanel outdatedPanel;
    private JPanel upToDatePanel;
    private JPanel vulnerablePanel;
    private JPanel noInfoPanel;
    private JPanel ignoredPanel;
    private JPanel helpPanel;
    private JTabbedPane tabbedPane;
    private JButton selectButton;
    private JButton rescanButton;
    private JButton exportButton;
    private JButton updateButton;
    private JButton darkModeButton;
    private JButton contactButton;
    private JButton settingsButton;
    private JLabel statusLabel;
    private File currentProjectFolder;
    private IgnoredDependencies ignoredDependencies;
    private Map<String, List<DependencyCheckbox>> allCheckboxes = new HashMap<>();
    private final List<ScanResult> lastScanResults = new ArrayList<>();
    private boolean darkModeEnabled = true;
    private final Map<String, Object> defaultUiValues = new HashMap<>();
    private static final String TOS_ACCEPTED_KEY = "feraldeps.tos.accepted";

    private static final String UI_FONT_FAMILY = "Menlo";
    private static final Color DARK_BG = new Color(15, 20, 26);
    private static final Color DARK_PANEL = new Color(20, 26, 34);
    private static final Color DARK_HEADER = new Color(24, 31, 41);
    private static final Color DARK_CARD = new Color(26, 34, 45);
    private static final Color DARK_BORDER = new Color(42, 52, 66);
    private static final Color DARK_TEXT = new Color(220, 226, 234);
    private static final Color DARK_MUTED = new Color(152, 160, 170);
    private static final Color DARK_ACCENT = new Color(55, 120, 240);
    private static final Color DARK_SUCCESS = new Color(40, 130, 78);
    private static final Color DARK_WARNING = new Color(177, 126, 43);
    private static final Color DARK_DANGER = new Color(179, 68, 67);
    private static final Color DARK_BUTTON = new Color(32, 39, 49);

    public GuiMain() {
        setTitle("Feral Deps Community - Dependency Scanner");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set Metal look and feel for better cross-platform tab styling support
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            // Fall back to default if Metal is not available
        }

        cacheDefaultUiValues();
        configureTypography();
        ensureTosAccepted();

        // Create UI components
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.putClientProperty("role", "header");
        topPanel.setBorder(new EmptyBorder(10, 12, 10, 12));
        
        // Left side: logo and button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.putClientProperty("role", "header");
        leftPanel.setOpaque(false);
        
        // Add logo if available
        URL logoURL = getClass().getClassLoader().getResource("Logo.png");
        if (logoURL != null) {
            ImageIcon logoIcon = new ImageIcon(logoURL);
            // Scale logo to reasonable size
            Image scaledImage = logoIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
            logoLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            leftPanel.add(logoLabel);
        }
        
        selectButton = new JButton("Select Project Folder");
        markButton(selectButton, "secondary");
        leftPanel.add(selectButton);

        rescanButton = new JButton("Rescan");
        markButton(rescanButton, "secondary");
        rescanButton.setEnabled(false);
        rescanButton.setVisible(false);
        rescanButton.addActionListener(e -> rescanCurrentProject());
        leftPanel.add(rescanButton);

        exportButton = new JButton("Export HTML Report");
        markButton(exportButton, "primary");
        exportButton.setEnabled(false);
        exportButton.addActionListener(e -> exportHtmlReport());
        leftPanel.add(exportButton);

        updateButton = new JButton("Update App");
        markButton(updateButton, "secondary");
        updateButton.addActionListener(e -> checkForUpdates());
        leftPanel.add(updateButton);

        darkModeButton = new JButton("Light Mode");
        markButton(darkModeButton, "secondary");
        darkModeButton.addActionListener(e -> toggleDarkMode());
        leftPanel.add(darkModeButton);

        settingsButton = new JButton("API Credentials");
        markButton(settingsButton, "secondary");
        settingsButton.addActionListener(e -> openSettingsDialog());
        leftPanel.add(settingsButton);
        
        // Right side: status
        statusLabel = new JLabel("No project selected");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.putClientProperty("role", "muted");
        
        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(statusLabel, BorderLayout.EAST);

        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(new EmptyBorder(4, 8, 8, 8));

        // All Dependencies tab
        allDepsPanel = new JPanel();
        allDepsPanel.setLayout(new BoxLayout(allDepsPanel, BoxLayout.Y_AXIS));
        JScrollPane allDepsScroll = new JScrollPane(allDepsPanel);
        allDepsScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        allDepsScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("All Dependencies", allDepsScroll);

        // Outdated Dependencies tab
        outdatedPanel = new JPanel();
        outdatedPanel.setLayout(new BoxLayout(outdatedPanel, BoxLayout.Y_AXIS));
        JScrollPane outdatedScroll = new JScrollPane(outdatedPanel);
        outdatedScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        outdatedScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("Outdated", outdatedScroll);

        // Up-to-Date Dependencies tab
        upToDatePanel = new JPanel();
        upToDatePanel.setLayout(new BoxLayout(upToDatePanel, BoxLayout.Y_AXIS));
        JScrollPane upToDateScroll = new JScrollPane(upToDatePanel);
        upToDateScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        upToDateScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("Up-to-Date", upToDateScroll);

        // Known Vulnerable Dependencies tab
        vulnerablePanel = new JPanel();
        vulnerablePanel.setLayout(new BoxLayout(vulnerablePanel, BoxLayout.Y_AXIS));
        JScrollPane vulnerableScroll = new JScrollPane(vulnerablePanel);
        vulnerableScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        vulnerableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("Known Vulnerable", vulnerableScroll);

        // No Info Available tab
        noInfoPanel = new JPanel();
        noInfoPanel.setLayout(new BoxLayout(noInfoPanel, BoxLayout.Y_AXIS));
        JScrollPane noInfoScroll = new JScrollPane(noInfoPanel);
        noInfoScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        noInfoScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("No Info Available", noInfoScroll);

        // Ignored Dependencies tab
        ignoredPanel = new JPanel();
        ignoredPanel.setLayout(new BoxLayout(ignoredPanel, BoxLayout.Y_AXIS));
        JScrollPane ignoredScroll = new JScrollPane(ignoredPanel);
        ignoredScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        ignoredScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("Ignored", ignoredScroll);

        // Help tab
        helpPanel = new JPanel(new BorderLayout());
        JTextArea helpText = new JTextArea();
        helpText.setEditable(false);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpText.setText(
            "FeralDeps Help\n\n" +
            "1) Select Project Folder\n" +
            "   - Scans build files: pom.xml, build.gradle, build.gradle.kts\n" +
            "   - Direct dependencies only (no transitive resolution)\n" +
            "   - Maven parent POM properties are resolved\n\n" +
            "2) Tabs\n" +
            "   - All Dependencies: everything found\n" +
            "   - Outdated: not on latest version\n" +
            "   - Up-to-Date: latest version detected\n" +
            "   - Known Vulnerable: vulnerabilities reported by sources\n" +
            "   - No Info Available: no version metadata found\n" +
            "   - Ignored: hidden dependencies\n\n" +
            "3) Update Buttons\n" +
            "   - Updates the version in the build file\n\n" +
            "4) CVSS & Vulnerability Data\n" +
            "   - Sources: OSV, OSS Index, NVD, GitHub Advisories\n" +
            "   - Some records do not include CVSS scores\n\n" +
            "5) Settings\n" +
            "   - Add OSS Index and GitHub tokens for richer CVSS data\n\n" +
            "6) Reports\n" +
            "   - Export HTML report after a scan\n\n" +
            "7) Dark Mode\n" +
            "   - Toggle UI theme from the top bar\n\n" +
            "8) Contact\n" +
            "   - Use the Contact button to send a support request"
        );
        JScrollPane helpScroll = new JScrollPane(helpText);
        helpScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        helpScroll.getVerticalScrollBar().setUnitIncrement(16);
        helpPanel.add(helpScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Help", helpPanel);

        // Layout
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        contactButton = new JButton("Contact");
        markButton(contactButton, "secondary");
        contactButton.setToolTipText("Contact support");
        contactButton.addActionListener(e -> openContactDialog());
        bottomPanel.add(contactButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Button action
        selectButton.addActionListener(e -> selectAndScanProject());

        applyTheme(darkModeEnabled);
    }

    private void selectAndScanProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Project Folder");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            startScan(fileChooser.getSelectedFile());
        }
    }

    private void rescanCurrentProject() {
        if (currentProjectFolder == null) {
            return;
        }
        startScan(currentProjectFolder);
    }

    private void startScan(File projectFolder) {
        currentProjectFolder = projectFolder;
        ignoredDependencies = new IgnoredDependencies(currentProjectFolder);
        rescanButton.setEnabled(false);
        statusLabel.setText("Scanning: " + currentProjectFolder.getName());
        exportButton.setEnabled(false);
        synchronized (lastScanResults) {
            lastScanResults.clear();
        }
        
        // Clear all panels
        allDepsPanel.removeAll();
        outdatedPanel.removeAll();
        upToDatePanel.removeAll();
        vulnerablePanel.removeAll();
        noInfoPanel.removeAll();
        ignoredPanel.removeAll();
        allCheckboxes.clear();
        
        String header = "Searching for build files (pom.xml / build.gradle / build.gradle.kts) in: " + currentProjectFolder.getAbsolutePath();
        addHeaderLabel(allDepsPanel, header);
        addHeaderLabel(outdatedPanel, header);
        addHeaderLabel(upToDatePanel, header);
        addHeaderLabel(vulnerablePanel, header);
        addHeaderLabel(noInfoPanel, header);
        addHeaderLabel(ignoredPanel, header);

        String note = "Note: Maven parent POM properties are resolved for direct dependencies.";
        addInfoLabel(allDepsPanel, note);
        addInfoLabel(outdatedPanel, note);
        addInfoLabel(upToDatePanel, note);
        addInfoLabel(vulnerablePanel, note);
        addInfoLabel(noInfoPanel, note);
        addInfoLabel(ignoredPanel, note);

        // Run scan in background thread
        new Thread(() -> scanFolder(currentProjectFolder)).start();
    }
    
    private void addHeaderLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(uiFont(Font.BOLD, 12));
        label.setBorder(new EmptyBorder(10, 10, 10, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }

    private void scanFolder(File folder) {
        List<File> buildFiles = new ArrayList<>();
        findBuildFiles(folder, buildFiles);

        if (buildFiles.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                addInfoLabel(allDepsPanel, "No build files found (pom.xml / build.gradle / build.gradle.kts / package.json).");
                addInfoLabel(outdatedPanel, "No build files found (pom.xml / build.gradle / build.gradle.kts / package.json).");
                addInfoLabel(upToDatePanel, "No build files found (pom.xml / build.gradle / build.gradle.kts / package.json).");
                addInfoLabel(vulnerablePanel, "No build files found (pom.xml / build.gradle / build.gradle.kts / package.json).");
                addInfoLabel(noInfoPanel, "No build files found (pom.xml / build.gradle / build.gradle.kts / package.json).");
                addInfoLabel(ignoredPanel, "No build files found (pom.xml / build.gradle / build.gradle.kts / package.json).");
                statusLabel.setText("No build files found");
                rescanButton.setVisible(true);
                rescanButton.setEnabled(true);
            });
            return;
        }

        SwingUtilities.invokeLater(() -> {
            StringBuilder found = new StringBuilder("Found " + buildFiles.size() + " build file(s):");
            for (File build : buildFiles) {
                found.append("\n  • ").append(build.getAbsolutePath());
            }
            
            addInfoLabel(allDepsPanel, found.toString());
            addInfoLabel(outdatedPanel, found.toString());
            addInfoLabel(upToDatePanel, found.toString());
            addInfoLabel(vulnerablePanel, found.toString());
            addInfoLabel(noInfoPanel, found.toString());
            addInfoLabel(ignoredPanel, found.toString());
        });

        // Scan each pom.xml file
        int outdatedCount = 0;
        for (File buildFile : buildFiles) {
            outdatedCount += scanBuildFile(buildFile);
        }

        final int finalOutdatedCount = outdatedCount;
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Scan complete - " + buildFiles.size() + " file(s), " + finalOutdatedCount + " outdated");
            exportButton.setEnabled(!lastScanResults.isEmpty());
            rescanButton.setVisible(true);
            rescanButton.setEnabled(true);
        });
    }

    private void findBuildFiles(File directory, List<File> buildFiles) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Skip common directories that won't have project pom.xml files
                String name = file.getName();
                if (!name.equals("target") && !name.equals("node_modules") && 
                    !name.startsWith(".") && !name.equals("build")) {
                    findBuildFiles(file, buildFiles);
                }
            } else if (isBuildFile(file)) {
                buildFiles.add(file);
            }
        }
    }

    private boolean isBuildFile(File file) {
        String name = file.getName();
        return name.equals("pom.xml") || name.equals("build.gradle") || name.equals("build.gradle.kts") || name.equals("package.json");
    }

    private int scanBuildFile(File buildFile) {
        SwingUtilities.invokeLater(() -> {
            addSectionLabel(allDepsPanel, "Scanning: " + buildFile.getAbsolutePath());
            addSectionLabel(outdatedPanel, "Scanning: " + buildFile.getAbsolutePath());
            addSectionLabel(upToDatePanel, "Scanning: " + buildFile.getAbsolutePath());
            addSectionLabel(vulnerablePanel, "Scanning: " + buildFile.getAbsolutePath());
            addSectionLabel(noInfoPanel, "Scanning: " + buildFile.getAbsolutePath());
            addSectionLabel(ignoredPanel, "Scanning: " + buildFile.getAbsolutePath());
        });

        int outdatedCount = 0;

        try {
            List<Dependency> dependencies = parseBuildFile(buildFile);

            if (dependencies.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    addInfoLabel(allDepsPanel, "   No dependencies found.");
                    addInfoLabel(outdatedPanel, "   No dependencies found.");
                    addInfoLabel(upToDatePanel, "   No dependencies found.");
                    addInfoLabel(vulnerablePanel, "   No dependencies found.");
                    addInfoLabel(noInfoPanel, "   No dependencies found.");
                    addInfoLabel(ignoredPanel, "   No dependencies found.");
                });
                return 0;
            }

            List<DependencyCheckbox> allCheckboxList = new ArrayList<>();

            for (Dependency dep : dependencies) {
                // Handle ignored dependencies separately
                if (ignoredDependencies.isIgnored(dep)) {
                    // Add to Ignored tab with checked checkbox
                    boolean isVulnerable = VulnerabilityDatabase.isVulnerable(dep);
                    String latestVersion = VulnerabilityDatabase.latestVersion(dep).orElse(null);
                    Double cvssScore = null;
                    String cvssSource = null;
                    java.util.Optional<VulnerabilityDatabase.CvssResult> cvss = VulnerabilityDatabase.getCvssResult(dep);
                    if (cvss.isPresent()) {
                        cvssScore = cvss.get().score;
                        cvssSource = cvss.get().source;
                    }
                    addScanResult(new ScanResult(dep, latestVersion, isVulnerable, true, buildFile, cvssScore, cvssSource));
                    
                    IgnoredDependencyCheckbox ignoredCheckbox = new IgnoredDependencyCheckbox(dep, buildFile, latestVersion, isVulnerable);
                    SwingUtilities.invokeLater(() -> {
                        ignoredPanel.add(ignoredCheckbox);
                    });
                    continue;
                }

                boolean isVulnerable = VulnerabilityDatabase.isVulnerable(dep);
                String latestVersion = VulnerabilityDatabase.latestVersion(dep).orElse(null);
                boolean isOutdated = latestVersion != null && !latestVersion.equals(dep.version);
                Double cvssScore = null;
                String cvssSource = null;
                java.util.Optional<VulnerabilityDatabase.CvssResult> cvss = VulnerabilityDatabase.getCvssResult(dep);
                if (cvss.isPresent()) {
                    cvssScore = cvss.get().score;
                    cvssSource = cvss.get().source;
                }
                addScanResult(new ScanResult(dep, latestVersion, isVulnerable, false, buildFile, cvssScore, cvssSource));

                // Create checkbox for All Dependencies
                DependencyCheckbox allCheckbox = new DependencyCheckbox(dep, buildFile, latestVersion, isVulnerable);
                allCheckboxList.add(allCheckbox);
                
                SwingUtilities.invokeLater(() -> {
                    allDepsPanel.add(allCheckbox);
                });

                if (isVulnerable) {
                    DependencyCheckbox vulnCheckbox = new DependencyCheckbox(dep, buildFile, latestVersion, true);
                    allCheckboxList.add(vulnCheckbox);
                    SwingUtilities.invokeLater(() -> {
                        vulnerablePanel.add(vulnCheckbox);
                    });
                }

                if (latestVersion != null) {
                    if (isOutdated) {
                        outdatedCount++;
                        DependencyCheckbox outdatedCheckbox = new DependencyCheckbox(dep, buildFile, latestVersion, isVulnerable);
                        allCheckboxList.add(outdatedCheckbox);
                        SwingUtilities.invokeLater(() -> {
                            outdatedPanel.add(outdatedCheckbox);
                        });
                    } else {
                        DependencyCheckbox upToDateCheckbox = new DependencyCheckbox(dep, buildFile, latestVersion, isVulnerable);
                        allCheckboxList.add(upToDateCheckbox);
                        SwingUtilities.invokeLater(() -> {
                            upToDatePanel.add(upToDateCheckbox);
                        });
                    }
                } else {
                    DependencyCheckbox noInfoCheckbox = new DependencyCheckbox(dep, buildFile, null, isVulnerable);
                    allCheckboxList.add(noInfoCheckbox);
                    SwingUtilities.invokeLater(() -> {
                        noInfoPanel.add(noInfoCheckbox);
                    });
                }
            }

            // Store checkboxes for this pom file
            allCheckboxes.put(buildFile.getAbsolutePath(), allCheckboxList);

            SwingUtilities.invokeLater(() -> {
                allDepsPanel.revalidate();
                allDepsPanel.repaint();
                outdatedPanel.revalidate();
                outdatedPanel.repaint();
                upToDatePanel.revalidate();
                upToDatePanel.repaint();
                vulnerablePanel.revalidate();
                vulnerablePanel.repaint();
                noInfoPanel.revalidate();
                noInfoPanel.repaint();
                ignoredPanel.revalidate();
                ignoredPanel.repaint();
            });

        } catch (Exception e) {
            String errorMsg = "   Error parsing file: " + e.getMessage();
            SwingUtilities.invokeLater(() -> {
                addInfoLabel(allDepsPanel, errorMsg);
                addInfoLabel(outdatedPanel, errorMsg);
                addInfoLabel(upToDatePanel, errorMsg);
                addInfoLabel(vulnerablePanel, errorMsg);
                addInfoLabel(noInfoPanel, errorMsg);
                addInfoLabel(ignoredPanel, errorMsg);
            });
        }

        return outdatedCount;
    }

    private List<Dependency> parseBuildFile(File buildFile) throws Exception {
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
        return new ArrayList<>();
    }
    
    private void addInfoLabel(JPanel panel, String text) {
        JLabel label = new JLabel("<html>" + text.replace("\n", "<br>") + "</html>");
        label.setFont(uiFont(Font.PLAIN, 11));
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }
    
    private void addSectionLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(uiFont(Font.BOLD, 11));
        label.setBorder(new EmptyBorder(10, 10, 5, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }
    
    /**
     * Custom panel that represents a dependency with a checkbox to hide it
     */
    private class DependencyCheckbox extends RoundedPanel {
        private Dependency dependency;
        private JCheckBox hideCheckbox;
        
        public DependencyCheckbox(Dependency dep, File pomFile, String latestVersion, boolean isVulnerable) {
            super(DARK_CARD, DARK_BORDER, 14);
            this.dependency = dep;
            
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 12, 10, 12));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            
            // Checkbox on the left
            hideCheckbox = new JCheckBox();
            hideCheckbox.setToolTipText("Hide this dependency from future scans");
            hideCheckbox.setOpaque(false);
            hideCheckbox.addActionListener(e -> {
                if (hideCheckbox.isSelected()) {
                    ignoredDependencies.ignore(dep);
                    // Remove all instances of this checkbox from all tabs
                    removeAllInstancesOfDependency(dep);
                }
            });
            
            JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            checkboxPanel.setOpaque(false);
            checkboxPanel.add(hideCheckbox);
            add(checkboxPanel, BorderLayout.WEST);
            
            // Dependency info in the center
            StringBuilder info = new StringBuilder();
            info.append("<html>");
            info.append("<b>").append(dep.coordinate()).append(":").append(dep.version).append("</b><br>");
            info.append("<span style='font-size:10px;color:#666;'>Scope: ").append(dep.scope).append(" | ");
            info.append(dep.getVersionConstraintType()).append("</span><br>");
            
            if (latestVersion != null) {
                if (!latestVersion.equals(dep.version)) {
                    info.append("<span style='color:#FF6600;'>WARNING: Outdated → latest: ").append(latestVersion).append("</span>");
                    int severityScore = dep.getOutdatedSeverityScore(latestVersion);
                    info.append("<br><span style='color:#9A6700;'>Severity (outdated): ").append(severityScore).append("/10</span>");
                    if (dep.isVersionLocked()) {
                        info.append("<span style='color:#FF0000;'> (Version LOCKED - may break if upgraded)</span>");
                    }
                    info.append("<br><span style='color:#0066CC;'>RECOMMENDED:</span>");
                    info.append("<br><span style='color:#0066CC;'>Latest: &lt;version&gt;").append(latestVersion).append("&lt;/version&gt;</span>");
                } else {
                    info.append("<span style='color:#00AA00;'>Up-to-date (latest: ").append(latestVersion).append(")</span>");
                }
            } else {
                info.append("<span style='color:#888;'>? No version info available</span>");
            }
            
            if (resultHasCvss(dep)) {
                VulnerabilityDatabase.getCvssResult(dep).ifPresent(result ->
                    info.append("<br><span style='color:#B00000;'>CVSS Score: ").append(result.score).append(" ( ").append(result.source).append(" )</span>")
                );
            }

            if (isVulnerable) {
                info.append("<br><span style='color:#FF0000;'><b>WARNING: VULNERABLE - Security issues detected</b></span>");
                if (dep.isVersionLocked()) {
                    info.append("<span style='color:#FF0000;'> (CRITICAL: Vulnerable version is LOCKED)</span>");
                }
                
                // Get specific remediation from OSV
                VulnerabilityDatabase.getRemediationInfo(dep).ifPresent(remediation -> {
                    if (remediation.hasRemediation && !remediation.fixedVersions.isEmpty()) {
                        String minimalFix = remediation.fixedVersions.get(0);
                        info.append("<br><span style='color:#CC0000;'><b>URGENT Remediation:</b></span>");
                        info.append("<br><span style='color:#CC0000;'>Minimal fixed: &lt;version&gt;").append(minimalFix).append("&lt;/version&gt;</span>");
                        if (latestVersion != null) {
                            info.append("<br><span style='color:#CC0000;'>Latest: &lt;version&gt;").append(latestVersion).append("&lt;/version&gt;</span>");
                        }
                        if (!remediation.summary.isEmpty()) {
                            info.append("<br><span style='color:#666;font-size:10px;'>Issue: ").append(remediation.summary).append("</span>");
                        }
                    } else if (latestVersion != null) {
                        info.append("<br><span style='color:#CC0000;'><b>URGENT Remediation:</b> Latest: &lt;version&gt;").append(latestVersion).append("&lt;/version&gt;</span>");
                    } else {
                        info.append("<br><span style='color:#CC0000;'><b>Remediation:</b> Find secure alternative at mvnrepository.com</span>");
                    }
                });
            }
            
            info.append("</html>");
            
            JLabel infoLabel = new JLabel(info.toString());
            infoLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
            add(infoLabel, BorderLayout.CENTER);
            if (darkModeEnabled) {
                refreshHtmlLabelColors(infoLabel, true);
            }
            
            // Add update button on the right for outdated or vulnerable dependencies
            if (latestVersion != null && !latestVersion.equals(dep.version)) {
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                buttonPanel.setOpaque(false);
                JButton updateButton = new JButton("Update");
                markButton(updateButton, isVulnerable ? "danger" : "success");
                updateButton.setToolTipText("Update to version " + latestVersion + " in pom.xml");
                updateButton.addActionListener(e -> updateDependency(dep, latestVersion, pomFile));
                buttonPanel.add(updateButton);
                add(buttonPanel, BorderLayout.EAST);
            } else if (isVulnerable) {
                // For vulnerable dependencies, show update button with first fixed version
                VulnerabilityDatabase.getRemediationInfo(dep).ifPresent(remediation -> {
                    if (remediation.hasRemediation && !remediation.fixedVersions.isEmpty()) {
                        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                        buttonPanel.setOpaque(false);
                        String fixVersion = remediation.fixedVersions.get(0);
                        JButton updateButton = new JButton("Update");
                        markButton(updateButton, "danger");
                        updateButton.setToolTipText("Update to secure version " + fixVersion + " in pom.xml");
                        updateButton.addActionListener(e -> updateDependency(dep, fixVersion, pomFile));
                        buttonPanel.add(updateButton);
                        add(buttonPanel, BorderLayout.EAST);
                    }
                });
            }
        }
    }
    
    /**
     * Update a dependency in the pom.xml file
     */
    private void updateDependency(Dependency dep, String newVersion, File buildFile) {
        // Show confirmation dialog with warning
        int choice = JOptionPane.showOptionDialog(this,
            "Update " + dep.coordinate() + " from " + dep.version + " to " + newVersion + "?\n\n" +
            "Options:\n" +
            "• Update: Update the dependency version\n" +
            "• Cancel: Don't update",
            "Confirm Update",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new Object[]{"Update", "Cancel"},
            "Update");
        
        if (choice == JOptionPane.NO_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return; // User cancelled
        }
        
        try {
            String name = buildFile.getName();
            boolean updated = false;
            String originalContent = new String(java.nio.file.Files.readAllBytes(buildFile.toPath()));
            String newContent = originalContent;

            if (name.equals("pom.xml")) {
                newContent = updatePomDependency(dep, newVersion, originalContent);
                updated = !newContent.equals(originalContent);
            } else if (name.equals("build.gradle") || name.equals("build.gradle.kts")) {
                newContent = updateGradleDependency(dep, newVersion, originalContent);
                updated = !newContent.equals(originalContent);
            }

            if (updated) {
                java.nio.file.Files.write(buildFile.toPath(), newContent.getBytes());

                JOptionPane.showMessageDialog(this,
                    "Successfully updated " + dep.coordinate() + " from " + dep.version + " to " + newVersion + "\n" +
                    "File: " + buildFile.getName() + "\n\n" +
                    "Please rescan the project to see the changes.",
                    "Dependency Updated",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Could not find the dependency in " + buildFile.getName() + "\n" +
                    "It may be defined in a parent or via variables.",
                    "Update Failed",
                    JOptionPane.WARNING_MESSAGE);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error updating dependency: " + ex.getMessage(),
                "Update Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String updatePomDependency(Dependency dep, String newVersion, String content) {
        String[] lines = content.split("\n");
        StringBuilder updatedContent = new StringBuilder();
        boolean inTargetDependency = false;
        boolean foundGroupId = false;
        boolean foundArtifactId = false;

        for (String line : lines) {
            if (line.trim().equals("<dependency>")) {
                inTargetDependency = true;
                foundGroupId = false;
                foundArtifactId = false;
            }

            if (inTargetDependency) {
                if (line.contains("<groupId>" + dep.groupId + "</groupId>")) {
                    foundGroupId = true;
                }
                if (line.contains("<artifactId>" + dep.artifactId + "</artifactId>")) {
                    foundArtifactId = true;
                }

                if (foundGroupId && foundArtifactId && line.contains("<version>")) {
                    String indent = line.substring(0, line.indexOf("<version>"));
                    line = indent + "<version>" + newVersion + "</version>";
                    inTargetDependency = false;
                }

                if (line.trim().equals("</dependency>")) {
                    inTargetDependency = false;
                }
            }

            updatedContent.append(line).append("\n");
        }

        return updatedContent.toString();
    }

    private String updateGradleDependency(Dependency dep, String newVersion, String content) {
        String[] lines = content.split("\n");
        StringBuilder updatedContent = new StringBuilder();
        boolean updated = false;

        java.util.regex.Pattern depPattern = java.util.regex.Pattern.compile("(['\"])" +
                java.util.regex.Pattern.quote(dep.groupId + ":" + dep.artifactId + ":") + "([^'\"]+)(['\"])" );

        // First pass: direct replacement and capture variable reference if present
        String varName = null;
        for (String line : lines) {
            java.util.regex.Matcher matcher = depPattern.matcher(line);
            if (matcher.find()) {
                String versionPart = matcher.group(2);
                if (versionPart.contains("$")) {
                    varName = extractGradleVarName(versionPart);
                } else {
                    line = matcher.replaceFirst(matcher.group(1) + dep.groupId + ":" + dep.artifactId + ":" + newVersion + matcher.group(3));
                    updated = true;
                }
            }
            updatedContent.append(line).append("\n");
        }

        if (updated) {
            return updatedContent.toString();
        }

        // Second pass: update variable definition if dependency uses $var or ${var}
        if (varName != null && !varName.isEmpty()) {
            StringBuilder varUpdated = new StringBuilder();
            boolean varChanged = false;

            java.util.regex.Pattern groovyVar = java.util.regex.Pattern.compile("^\\s*(?:ext\\.)?" + java.util.regex.Pattern.quote(varName) + "\\s*=\\s*['\"][^'\"]+['\"]\\s*$");
            java.util.regex.Pattern kotlinVal = java.util.regex.Pattern.compile("^\\s*val\\s+" + java.util.regex.Pattern.quote(varName) + "\\s*=\\s*['\"][^'\"]+['\"]\\s*$");
            java.util.regex.Pattern kotlinExtra = java.util.regex.Pattern.compile("^\\s*extra\\[\\\"" + java.util.regex.Pattern.quote(varName) + "\\\"\\]\\s*=\\s*['\"][^'\"]+['\"]\\s*$");

            for (String line : lines) {
                if (groovyVar.matcher(line).matches()) {
                    line = line.replaceAll("=['\"][^'\"]+['\"]", "= '" + newVersion + "'");
                    varChanged = true;
                } else if (kotlinVal.matcher(line).matches()) {
                    line = line.replaceAll("=['\"][^'\"]+['\"]", "= \"" + newVersion + "\"");
                    varChanged = true;
                } else if (kotlinExtra.matcher(line).matches()) {
                    line = line.replaceAll("=['\"][^'\"]+['\"]", "= \"" + newVersion + "\"");
                    varChanged = true;
                }
                varUpdated.append(line).append("\n");
            }

            return varChanged ? varUpdated.toString() : content;
        }

        return content;
    }

    private String extractGradleVarName(String versionPart) {
        String trimmed = versionPart.trim();
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            return trimmed.substring(2, trimmed.length() - 1);
        }
        if (trimmed.startsWith("$")) {
            return trimmed.substring(1);
        }
        return null;
    }
    
    /**
     * Remove all instances of a dependency from all tabs
     */
    private void removeAllInstancesOfDependency(Dependency dep) {
        String depKey = dep.coordinate() + ":" + dep.version;
        
        for (List<DependencyCheckbox> checkboxes : allCheckboxes.values()) {
            for (DependencyCheckbox checkbox : new ArrayList<>(checkboxes)) {
                if ((checkbox.dependency.coordinate() + ":" + checkbox.dependency.version).equals(depKey)) {
                    Container parent = checkbox.getParent();
                    if (parent != null) {
                        parent.remove(checkbox);
                        parent.revalidate();
                        parent.repaint();
                    }
                }
            }
        }
    }

    private boolean resultHasCvss(Dependency dep) {
        return VulnerabilityDatabase.getCvssResult(dep).map(r -> r.score != null).orElse(false);
    }

    private void addScanResult(ScanResult result) {
        synchronized (lastScanResults) {
            lastScanResults.add(result);
        }
    }

    private void exportHtmlReport() {
        if (currentProjectFolder == null) {
            JOptionPane.showMessageDialog(this,
                "Please scan a project first.",
                "No Scan Results",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<ScanResult> copy;
        synchronized (lastScanResults) {
            copy = new ArrayList<>(lastScanResults);
        }

        if (copy.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No scan results available.",
                "No Scan Results",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser(currentProjectFolder);
        fileChooser.setDialogTitle("Save HTML Report");
        fileChooser.setSelectedFile(new File(currentProjectFolder, "feraldeps-report.html"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            try {
                HtmlReportGenerator.writeHtmlReport(copy, outputFile, currentProjectFolder);
                JOptionPane.showMessageDialog(this,
                    "Report saved to:\n" + outputFile.getAbsolutePath(),
                    "Report Saved",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Error writing report: " + e.getMessage(),
                    "Report Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void toggleDarkMode() {
        darkModeEnabled = !darkModeEnabled;
        applyTheme(darkModeEnabled);
        darkModeButton.setText(darkModeEnabled ? "Light Mode" : "Dark Mode");
    }

    private void applyTheme(boolean dark) {
        Color bg = dark ? DARK_PANEL : (Color) defaultUiValues.get("Panel.background");
        Color fg = dark ? DARK_TEXT : (Color) defaultUiValues.get("Label.foreground");
        Color control = dark ? DARK_BG : (Color) defaultUiValues.get("control");

        UIManager.put("Panel.background", bg);
        UIManager.put("Viewport.background", bg);
        UIManager.put("ScrollPane.background", bg);
        UIManager.put("Label.foreground", fg);
        UIManager.put("Label.background", bg);
        restoreButtonDefaults();
        UIManager.put("CheckBox.background", bg);
        UIManager.put("CheckBox.foreground", fg);
        UIManager.put("TabbedPane.background", bg);
        UIManager.put("TabbedPane.foreground", fg);
        UIManager.put("TabbedPane.selected", dark ? new Color(70, 130, 240) : defaultUiValues.get("TabbedPane.selected"));
        UIManager.put("TabbedPane.selectedForeground", dark ? Color.WHITE : (Color) defaultUiValues.get("TabbedPane.foreground"));
        UIManager.put("TabbedPane.selectedBackground", dark ? new Color(70, 130, 240) : defaultUiValues.get("TabbedPane.selected"));
        UIManager.put("TabbedPane.contentAreaColor", dark ? new Color(70, 130, 240) : (Color) defaultUiValues.get("TabbedPane.contentAreaColor"));
        UIManager.put("TabbedPane.contentBorderInsets", new java.awt.Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.tabAreaBackground", bg);
        UIManager.put("TabbedPane.highlight", dark ? new Color(90, 150, 255) : (Color) defaultUiValues.get("control"));
        UIManager.put("TabbedPane.shadow", dark ? new Color(30, 30, 40) : UIManager.getColor("TabbedPane.shadow"));
        UIManager.put("control", control);
        UIManager.put("TextArea.background", bg);
        UIManager.put("TextArea.foreground", fg);
        UIManager.put("TextField.background", bg);
        UIManager.put("TextField.foreground", fg);
        UIManager.put("OptionPane.background", bg);
        UIManager.put("OptionPane.foreground", fg);
        UIManager.put("OptionPane.messageForeground", fg);

        SwingUtilities.updateComponentTreeUI(this);
        applyComponentTheme(this, bg, fg, dark);
        // Explicitly set tab colors after component theme
        if (tabbedPane != null) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                tabbedPane.setForegroundAt(i, dark ? Color.WHITE : fg);
            }
        }
        refreshHtmlLabelColors(this, dark);
        revalidate();
        repaint();
    }

    private void applyThemeToDialog(JDialog dialog) {
        if (dialog == null) {
            return;
        }
        Color bg = darkModeEnabled ? DARK_PANEL : (Color) defaultUiValues.get("Panel.background");
        Color fg = darkModeEnabled ? DARK_TEXT : (Color) defaultUiValues.get("Label.foreground");
        SwingUtilities.updateComponentTreeUI(dialog);
        applyComponentTheme(dialog.getContentPane(), bg, fg, darkModeEnabled);
        refreshHtmlLabelColors(dialog, darkModeEnabled);
        dialog.getContentPane().setBackground(bg);
    }

    private void restoreButtonDefaults() {
        UIManager.put("Button.background", defaultUiValues.get("Button.background"));
        UIManager.put("Button.foreground", defaultUiValues.get("Button.foreground"));
        UIManager.put("Button.border", defaultUiValues.get("Button.border"));
        UIManager.put("Button.focus", defaultUiValues.get("Button.focus"));
        UIManager.put("Button.select", defaultUiValues.get("Button.select"));
    }

    private void applyComponentTheme(Component component, Color bg, Color fg, boolean dark) {
        if (component instanceof RoundedPanel) {
            RoundedPanel panel = (RoundedPanel) component;
            if (dark) {
                panel.setColors(DARK_CARD, DARK_BORDER);
            } else {
                panel.setColors(new Color(245, 247, 250), new Color(220, 224, 230));
            }
        }

        if (component instanceof JPanel || component instanceof JLabel || component instanceof JCheckBox) {
            if (component instanceof javax.swing.JComponent) {
                Object role = ((javax.swing.JComponent) component).getClientProperty("role");
                if (dark && "header".equals(role)) {
                    component.setBackground(DARK_HEADER);
                    component.setForeground(fg);
                } else if (dark && "muted".equals(role) && component instanceof JLabel) {
                    component.setForeground(DARK_MUTED);
                } else {
                    component.setBackground(bg);
                    component.setForeground(fg);
                }
            } else {
                component.setBackground(bg);
                component.setForeground(fg);
            }
        }

        if (component instanceof JButton) {
            styleButton((JButton) component, dark);
        }

        if (component instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) component;
            scrollPane.getViewport().setBackground(bg);
            if (scrollPane.getViewport().getView() != null) {
                scrollPane.getViewport().getView().setBackground(bg);
                scrollPane.getViewport().getView().setForeground(fg);
            }
        }

        if (component instanceof JTabbedPane) {
            JTabbedPane pane = (JTabbedPane) component;
            pane.setBackground(bg);
            pane.setForeground(fg);
            // Make sure all tabs use appropriate text colors
            for (int i = 0; i < pane.getTabCount(); i++) {
                pane.setForegroundAt(i, dark ? Color.WHITE : fg);
            }
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyComponentTheme(child, bg, fg, dark);
            }
        }
    }

    private void cacheDefaultUiValues() {
        defaultUiValues.put("Panel.background", UIManager.getColor("Panel.background"));
        defaultUiValues.put("Label.foreground", UIManager.getColor("Label.foreground"));
        defaultUiValues.put("control", UIManager.getColor("control"));
        defaultUiValues.put("Button.background", UIManager.getColor("Button.background"));
        defaultUiValues.put("Button.foreground", UIManager.getColor("Button.foreground"));
        defaultUiValues.put("Button.border", UIManager.get("Button.border"));
        defaultUiValues.put("Button.focus", UIManager.get("Button.focus"));
        defaultUiValues.put("Button.shadow", UIManager.getColor("Button.shadow"));
        defaultUiValues.put("Button.select", UIManager.getColor("Button.select"));
        defaultUiValues.put("TabbedPane.selected", UIManager.getColor("TabbedPane.selected"));
        defaultUiValues.put("TabbedPane.foreground", UIManager.getColor("TabbedPane.foreground"));
        defaultUiValues.put("TabbedPane.contentAreaColor", UIManager.getColor("TabbedPane.contentAreaColor"));
    }

    private void configureTypography() {
        Font base = uiFont(Font.PLAIN, 12);
        Font bold = uiFont(Font.BOLD, 12);
        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base);
        UIManager.put("TabbedPane.font", base);
        UIManager.put("TextArea.font", base);
        UIManager.put("TextField.font", base);
        UIManager.put("CheckBox.font", base);
        UIManager.put("ToolTip.font", base);
        UIManager.put("OptionPane.messageFont", base);
        UIManager.put("OptionPane.buttonFont", bold);
    }

    private Font uiFont(int style, int size) {
        return new Font(UI_FONT_FAMILY, style, size);
    }

    private void enablePaste(JTextField textField) {
        // Ensure paste functionality works in dialogs
        Action pasteAction = new DefaultEditorKit.PasteAction();
        textField.getActionMap().put("paste", pasteAction);
        textField.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
            "paste"
        );
        textField.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.SHIFT_DOWN_MASK),
            "paste"
        );

        // Ensure transfer handler is set for clipboard operations
        textField.setTransferHandler(new javax.swing.TransferHandler("text"));
    }

    private void pasteIntoField(JTextField textField) {
        try {
            // Programmatically paste from clipboard
            java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            java.awt.datatransfer.Transferable contents = clipboard.getContents(null);
            if (contents != null && contents.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                String pastedText = (String) contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
                textField.setText(pastedText);
                textField.requestFocusInWindow();
            }
        } catch (Exception e) {
            // If programmatic paste fails, try the action
            Action pasteAction = textField.getActionMap().get("paste");
            if (pasteAction != null) {
                pasteAction.actionPerformed(new java.awt.event.ActionEvent(textField, 0, "paste"));
            }
        }
    }

    private void markButton(JButton button, String role) {
        button.putClientProperty("role", role);
        button.setFocusPainted(false);
    }

    private void styleButton(JButton button, boolean dark) {
        Object role = button.getClientProperty("role");
        Color fill;
        Color border;
        Color text;

        if (dark) {
            fill = DARK_BUTTON;
            border = DARK_BORDER;
            text = DARK_TEXT;

            if ("primary".equals(role)) {
                fill = DARK_ACCENT;
                border = DARK_ACCENT.darker();
                text = Color.WHITE;
            } else if ("success".equals(role)) {
                fill = DARK_SUCCESS;
                border = DARK_SUCCESS.darker();
                text = Color.WHITE;
            } else if ("warning".equals(role)) {
                fill = DARK_WARNING;
                border = DARK_WARNING.darker();
                text = Color.WHITE;
            } else if ("danger".equals(role)) {
                fill = DARK_DANGER;
                border = DARK_DANGER.darker();
                text = Color.WHITE;
            }
        } else {
            // Light mode - same styling as dark mode, just different colors
            fill = new Color(240, 240, 240);
            border = new Color(180, 180, 180);
            text = new Color(30, 30, 30);

            if ("primary".equals(role)) {
                fill = new Color(59, 131, 246);
                border = new Color(37, 99, 214);
                text = Color.WHITE;
            } else if ("success".equals(role)) {
                fill = new Color(34, 197, 94);
                border = new Color(22, 163, 74);
                text = Color.WHITE;
            } else if ("warning".equals(role)) {
                fill = new Color(217, 119, 6);
                border = new Color(180, 83, 9);
                text = Color.WHITE;
            } else if ("danger".equals(role)) {
                fill = new Color(239, 68, 68);
                border = new Color(220, 38, 38);
                text = Color.WHITE;
            }
        }

        button.setBackground(fill);
        button.setForeground(text);
        button.setBorder(createRoundedButtonBorder(border));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
    }

    private Border createRoundedButtonBorder(Color borderColor) {
        return BorderFactory.createCompoundBorder(
            new RoundedBorder(borderColor, 1, 12),
            new EmptyBorder(6, 12, 6, 12)
        );
    }

    private void ensureTosAccepted() {
        Preferences prefs = Preferences.userNodeForPackage(GuiMain.class);
        boolean accepted = prefs.getBoolean(TOS_ACCEPTED_KEY, false);
        if (accepted) return;

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(
            "FeralDeps Disclaimer & Terms\n\n" +
            "FeralDeps is provided \"as is\" without warranties of any kind.\n\n" +
            "You are responsible for verifying findings and for any actions taken\n" +
            "based on the results. FeralDeps does not guarantee security or\n" +
            "vulnerability-free software.\n\n" +
            "To the maximum extent permitted by law, the creator/company is not\n" +
            "liable for any damages, losses, or security incidents resulting\n" +
            "from use of this application.\n\n" +
            "By clicking Accept, you agree to these terms."
        );

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(520, 280));

        int choice = JOptionPane.showOptionDialog(
            this,
            scrollPane,
            "Terms of Use",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            new Object[]{"Accept", "Decline"},
            "Accept"
        );

        if (choice == JOptionPane.YES_OPTION) {
            prefs.putBoolean(TOS_ACCEPTED_KEY, true);
        } else {
            System.exit(0);
        }
    }

    private void openContactDialog() {
        JDialog dialog = new JDialog(this, "Contact", true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel nameLabel = new JLabel("Your name");
        JTextField nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel emailLabel = new JLabel("Your email");
        JTextField emailField = new JTextField();
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel messageLabel = new JLabel("Your message");
        JTextArea messageArea = new JTextArea(6, 40);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(messageArea);

        formPanel.add(nameLabel);
        formPanel.add(nameField);
        formPanel.add(javax.swing.Box.createVerticalStrut(8));
        formPanel.add(emailLabel);
        formPanel.add(emailField);
        formPanel.add(javax.swing.Box.createVerticalStrut(8));
        formPanel.add(messageLabel);
        formPanel.add(messageScroll);

        JButton submitButton = new JButton("Send");
        markButton(submitButton, "primary");
        submitButton.addActionListener(e -> submitContactForm(nameField.getText(), emailField.getText(), messageArea.getText(), dialog));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(submitButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setSize(520, 360);
        dialog.setLocationRelativeTo(this);

        applyThemeToDialog(dialog);

        dialog.setVisible(true);
    }

    private void openSettingsDialog() {
        JDialog dialog = new JDialog(this, "Settings", true); // Keep it modal
        dialog.setLayout(new BorderLayout());

        Preferences prefs = Preferences.userNodeForPackage(GuiMain.class);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel ossUserLabel = new JLabel("OSS Index Username");
        JPanel ossUserPanel = new JPanel(new BorderLayout());
        JTextField ossUserField = new JTextField(prefs.get("ossindex.user", ""));
        ossUserField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        ossUserField.setEditable(true);
        ossUserField.setFocusable(true);
        enablePaste(ossUserField);
        JButton ossUserPasteButton = new JButton("Paste");
        ossUserPasteButton.setPreferredSize(new Dimension(70, 28));
        ossUserPasteButton.addActionListener(e -> pasteIntoField(ossUserField));
        ossUserPanel.add(ossUserField, BorderLayout.CENTER);
        ossUserPanel.add(ossUserPasteButton, BorderLayout.EAST);

        JLabel ossTokenLabel = new JLabel("OSS Index Token");
        JPanel ossTokenPanel = new JPanel(new BorderLayout());
        JTextField ossTokenField = new JTextField(prefs.get("ossindex.token", ""));
        ossTokenField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        ossTokenField.setEditable(true);
        ossTokenField.setFocusable(true);
        enablePaste(ossTokenField);
        JButton ossTokenPasteButton = new JButton("Paste");
        ossTokenPasteButton.setPreferredSize(new Dimension(70, 28));
        ossTokenPasteButton.addActionListener(e -> pasteIntoField(ossTokenField));
        ossTokenPanel.add(ossTokenField, BorderLayout.CENTER);
        ossTokenPanel.add(ossTokenPasteButton, BorderLayout.EAST);

        JLabel githubTokenLabel = new JLabel("GitHub Token (for CVSS)");
        JPanel githubTokenPanel = new JPanel(new BorderLayout());
        JTextField githubTokenField = new JTextField(prefs.get("github.token", ""));
        githubTokenField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        githubTokenField.setEditable(true);
        githubTokenField.setFocusable(true);
        enablePaste(githubTokenField);
        JButton githubTokenPasteButton = new JButton("Paste");
        githubTokenPasteButton.setPreferredSize(new Dimension(70, 28));
        githubTokenPasteButton.addActionListener(e -> pasteIntoField(githubTokenField));
        githubTokenPanel.add(githubTokenField, BorderLayout.CENTER);
        githubTokenPanel.add(githubTokenPasteButton, BorderLayout.EAST);

        formPanel.add(ossUserLabel);
        formPanel.add(ossUserPanel);
        formPanel.add(javax.swing.Box.createVerticalStrut(8));
        formPanel.add(ossTokenLabel);
        formPanel.add(ossTokenPanel);
        formPanel.add(javax.swing.Box.createVerticalStrut(8));
        formPanel.add(githubTokenLabel);
        formPanel.add(githubTokenPanel);

        JLabel note = new JLabel("These values are stored locally on this device.");
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));
        note.setBorder(new EmptyBorder(8, 0, 0, 0));
        formPanel.add(note);

        JButton saveButton = new JButton("Save");
        markButton(saveButton, "primary");
        saveButton.addActionListener(e -> {
            prefs.put("ossindex.user", ossUserField.getText().trim());
            prefs.put("ossindex.token", ossTokenField.getText().trim());
            prefs.put("github.token", githubTokenField.getText().trim());
            dialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        markButton(cancelButton, "secondary");
        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setSize(650, 320); // Increased width further to accommodate wider paste buttons
        dialog.setLocationRelativeTo(this);

        applyThemeToDialog(dialog);

        // Use a window listener to ensure focus is set after dialog is fully shown
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                dialog.toFront();
                dialog.requestFocus();
                SwingUtilities.invokeLater(() -> {
                    ossUserField.requestFocusInWindow();
                    // Don't select all - let user paste directly
                });
            }
        });

        dialog.setVisible(true);
    }

    private void submitContactForm(String name, String email, String message, JDialog dialog) {
        String safeName = name == null ? "" : name.trim();
        String safeEmail = email == null ? "" : email.trim();
        String safeMessage = message == null ? "" : message.trim();

        if (safeName.isEmpty() || safeEmail.isEmpty() || safeMessage.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "All fields are required.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String data = "name=" + URLEncoder.encode(safeName, StandardCharsets.UTF_8) +
                    "&email=" + URLEncoder.encode(safeEmail, StandardCharsets.UTF_8) +
                    "&message=" + URLEncoder.encode(safeMessage, StandardCharsets.UTF_8);

            URL url = new URL("https://formspree.io/f/xbdajqag");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = data.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                JOptionPane.showMessageDialog(this,
                    "Message sent.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } else if (responseCode == 422) {
                JOptionPane.showMessageDialog(this,
                    "Email format error.",
                    "Send Failed",
                    JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Failed to send message. HTTP " + responseCode,
                    "Send Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Failed to send message: " + ex.getMessage(),
                "Send Failed",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshHtmlLabelColors(Component component, boolean dark) {
        if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            String text = label.getText();
            if (text != null && text.toLowerCase().contains("<html")) {
                if (dark) {
                    text = text.replace("#666", "#B0B8C4")
                               .replace("#888", "#C4CCDA")
                               .replace("#9A6700", "#E8B966")
                               .replace("#00AA00", "#4ADA76")
                               .replace("#0066CC", "#5B9FFF")
                               .replace("#FF6600", "#FFA540")
                               .replace("#FF0000", "#FF5555")
                               .replace("#CC0000", "#FF4444")
                               .replace("#B00000", "#FF3333");
                } else {
                    text = text.replace("#B0B8C4", "#666")
                               .replace("#C4CCDA", "#888")
                               .replace("#E8B966", "#9A6700")
                               .replace("#4ADA76", "#00AA00")
                               .replace("#5B9FFF", "#0066CC")
                               .replace("#FFA540", "#FF6600")
                               .replace("#FF5555", "#FF0000")
                               .replace("#FF4444", "#CC0000")
                               .replace("#FF3333", "#B00000");
                }
                label.setText(text);
            }
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                refreshHtmlLabelColors(child, dark);
            }
        }
    }

    private void checkForUpdates() {
        updateButton.setEnabled(false);

        JDialog progressDialog = new JDialog(this, "Checking for Updates", true);
        JLabel progressLabel = new JLabel("Checking GitHub for the latest release...");
        progressLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        progressDialog.add(progressLabel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);

        new Thread(() -> {
            try {
                UpdateManager.ReleaseInfo release = UpdateManager.fetchLatestRelease().orElse(null);
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    updateButton.setEnabled(true);

                    if (release == null) {
                        JOptionPane.showMessageDialog(this,
                            "You're up to date.",
                            "Up to Date",
                            JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    String current = UpdateManager.getCurrentVersion().orElse("unknown");
                    String message = "Latest release: " + (release.tagName != null ? release.tagName : "unknown") + "\n" +
                            "Current version: " + current + "\n\n" +
                            "Download and install the latest release now?";

                    int choice = JOptionPane.showConfirmDialog(this,
                        message,
                        "Update Available",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                    if (choice == JOptionPane.YES_OPTION) {
                        installUpdate(release);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    updateButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this,
                        "Update check failed: " + e.getMessage(),
                        "Update Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();

        progressDialog.setVisible(true);
    }

    private void installUpdate(UpdateManager.ReleaseInfo release) {
        JDialog progressDialog = new JDialog(this, "Updating", true);
        JLabel progressLabel = new JLabel("Downloading and installing update...");
        progressLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        progressDialog.add(progressLabel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);

        new Thread(() -> {
            try {
                java.util.Optional<File> jarOpt = UpdateManager.getCurrentJarFile();
                if (jarOpt.isPresent()) {
                    File currentJar = jarOpt.get();
                    File tempFile = File.createTempFile("feraldeps-update-", ".jar");
                    UpdateManager.downloadReleaseAsset(release.assetUrl, tempFile);

                    try {
                        UpdateManager.replaceJar(currentJar, tempFile);
                    } finally {
                        tempFile.delete();
                    }

                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(this,
                            "Update installed. Please restart the application.",
                            "Update Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    });
                    return;
                }

                File tempFile = UpdateManager.createTempFileForAsset(release.assetName);
                UpdateManager.downloadReleaseAsset(release.assetUrl, tempFile);
                UpdateManager.openInstaller(tempFile);

                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(this,
                        "Installer opened. Complete the update, then relaunch FeralDeps.\n\n" +
                        "Downloaded to: " + tempFile.getAbsolutePath(),
                        "Update Ready",
                        JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(this,
                        "Update failed: " + e.getMessage(),
                        "Update Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();

        progressDialog.setVisible(true);
    }
    
    /**
     * Custom panel for ignored dependencies with checked checkbox that can be unchecked to restore
     */
    private class IgnoredDependencyCheckbox extends RoundedPanel {
        private JCheckBox restoreCheckbox;
        
        public IgnoredDependencyCheckbox(Dependency dep, File pomFile, String latestVersion, boolean isVulnerable) {
            super(DARK_CARD, DARK_BORDER, 14);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 12, 10, 12));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            
            // Checkbox on the left - checked by default
            restoreCheckbox = new JCheckBox();
            restoreCheckbox.setSelected(true);
            restoreCheckbox.setToolTipText("Uncheck to restore this dependency to the scan results");
            restoreCheckbox.setOpaque(false);
            restoreCheckbox.addActionListener(e -> {
                if (!restoreCheckbox.isSelected()) {
                    // Remove from ignored list
                    ignoredDependencies.unignore(dep);
                    // Remove from ignored tab
                    Container parent = IgnoredDependencyCheckbox.this.getParent();
                    if (parent != null) {
                        parent.remove(IgnoredDependencyCheckbox.this);
                        parent.revalidate();
                        parent.repaint();
                    }
                    // Show message that they need to rescan
                    JOptionPane.showMessageDialog(GuiMain.this,
                        "Dependency restored. Please rescan the project to see it in other tabs.",
                        "Dependency Restored",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            });
            
            JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            checkboxPanel.setOpaque(false);
            checkboxPanel.add(restoreCheckbox);
            add(checkboxPanel, BorderLayout.WEST);
            
            // Dependency info in the center
            StringBuilder info = new StringBuilder();
            info.append("<html>");
            info.append("<b>").append(dep.coordinate()).append(":").append(dep.version).append("</b><br>");
            info.append("<span style='font-size:10px;color:#666;'>Scope: ").append(dep.scope).append(" | ");
            info.append(dep.getVersionConstraintType()).append("</span><br>");
            info.append("<span style='color:#888;'>Hidden from scan results</span>");
            
            if (latestVersion != null) {
                if (!latestVersion.equals(dep.version)) {
                    info.append("<br><span style='color:#FF6600;'>WARNING: Outdated → latest: ").append(latestVersion).append("</span>");
                    int severityScore = dep.getOutdatedSeverityScore(latestVersion);
                    info.append("<br><span style='color:#9A6700;'>Severity (outdated): ").append(severityScore).append("/10</span>");
                    if (dep.isVersionLocked()) {
                        info.append("<span style='color:#FF0000;'> (Version LOCKED)</span>");
                    }
                    info.append("<br><span style='color:#0066CC;'>RECOMMENDED:</span>");
                    info.append("<br><span style='color:#0066CC;'>Latest: &lt;version&gt;").append(latestVersion).append("&lt;/version&gt;</span>");
                }
            }
            
            if (resultHasCvss(dep)) {
                VulnerabilityDatabase.getCvssResult(dep).ifPresent(result ->
                    info.append("<br><span style='color:#B00000;'>CVSS Score: ").append(result.score).append(" ( ").append(result.source).append(" )</span>")
                );
            }

            if (isVulnerable) {
                info.append("<br><span style='color:#FF0000;'><b>WARNING: VULNERABLE - Security issues detected</b></span>");
                
                // Get specific remediation from OSV
                VulnerabilityDatabase.getRemediationInfo(dep).ifPresent(remediation -> {
                    if (remediation.hasRemediation && !remediation.fixedVersions.isEmpty()) {
                        String minimalFix = remediation.fixedVersions.get(0);
                        info.append("<br><span style='color:#CC0000;'><b>URGENT Remediation:</b></span>");
                        info.append("<br><span style='color:#CC0000;'>Minimal fixed: &lt;version&gt;").append(minimalFix).append("&lt;/version&gt;</span>");
                        if (latestVersion != null) {
                            info.append("<br><span style='color:#CC0000;'>Latest: &lt;version&gt;").append(latestVersion).append("&lt;/version&gt;</span>");
                        }
                        if (!remediation.summary.isEmpty()) {
                            info.append("<br><span style='color:#666;font-size:10px;'>Issue: ").append(remediation.summary).append("</span>");
                        }
                    } else if (latestVersion != null) {
                        info.append("<br><span style='color:#CC0000;'><b>URGENT Remediation:</b> Latest: &lt;version&gt;").append(latestVersion).append("&lt;/version&gt;</span>");
                    } else {
                        info.append("<br><span style='color:#CC0000;'><b>Remediation:</b> Find secure alternative at mvnrepository.com</span>");
                    }
                });
            }
            
            info.append("</html>");
            
            JLabel infoLabel = new JLabel(info.toString());
            infoLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
            add(infoLabel, BorderLayout.CENTER);
            if (darkModeEnabled) {
                refreshHtmlLabelColors(infoLabel, true);
            }
            
            // Add update button on the right for outdated or vulnerable dependencies  
            if (latestVersion != null && !latestVersion.equals(dep.version)) {
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                buttonPanel.setOpaque(false);
                JButton updateButton = new JButton("Update");
                markButton(updateButton, isVulnerable ? "danger" : "success");
                updateButton.setToolTipText("Update to version " + latestVersion + " in pom.xml");
                updateButton.addActionListener(e -> updateDependency(dep, latestVersion, pomFile));
                buttonPanel.add(updateButton);
                add(buttonPanel, BorderLayout.EAST);
            } else if (isVulnerable) {
                // For vulnerable dependencies, show update button with first fixed version
                VulnerabilityDatabase.getRemediationInfo(dep).ifPresent(remediation -> {
                    if (remediation.hasRemediation && !remediation.fixedVersions.isEmpty()) {
                        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                        buttonPanel.setOpaque(false);
                        String fixVersion = remediation.fixedVersions.get(0);
                        JButton updateButton = new JButton("Update");
                        markButton(updateButton, "danger");
                        updateButton.setToolTipText("Update to secure version " + fixVersion + " in pom.xml");
                        updateButton.addActionListener(e -> updateDependency(dep, fixVersion, pomFile));
                        buttonPanel.add(updateButton);
                        add(buttonPanel, BorderLayout.EAST);
                    }
                });
            }
        }
    }

    private static class RoundedPanel extends JPanel {
        private Color fillColor;
        private Color borderColor;
        private final int arc;

        RoundedPanel(Color fillColor, Color borderColor, int arc) {
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            this.arc = arc;
            setOpaque(false);
        }

        void setColors(Color fillColor, Color borderColor) {
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedBorder implements Border {
        private final Color color;
        private final int thickness;
        private final int arc;

        RoundedBorder(Color color, int thickness, int arc) {
            this.color = color;
            this.thickness = thickness;
            this.arc = arc;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
            g2.dispose();
        }

        @Override
        public java.awt.Insets getBorderInsets(Component c) {
            return new java.awt.Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default if system L&F fails
        }

        // Show splash screen first, then main GUI
        SplashScreen.show(() -> {
            SwingUtilities.invokeLater(() -> {
                GuiMain frame = new GuiMain();
                frame.setVisible(true);
            });
        });
    }
}
