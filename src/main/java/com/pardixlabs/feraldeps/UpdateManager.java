package com.pardixlabs.feraldeps;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UpdateManager {
    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/PardixLabs/feraldeps-core/releases/latest";

    private enum OsType {
        WINDOWS,
        MAC,
        LINUX,
        OTHER
    }

    private static class ReleaseAsset {
        private final String name;
        private final String url;

        private ReleaseAsset(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    public static class ReleaseInfo {
        public final String tagName;
        public final String assetName;
        public final String assetUrl;

        public ReleaseInfo(String tagName, String assetName, String assetUrl) {
            this.tagName = tagName;
            this.assetName = assetName;
            this.assetUrl = assetUrl;
        }
    }

    public static Optional<String> getCurrentVersion() {
        Package pkg = Main.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return Optional.of(pkg.getImplementationVersion());
        }
        return Optional.empty();
    }

    public static Optional<File> getCurrentJarFile() {
        try {
            URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();
            if (url == null) return Optional.empty();
            File file = new File(url.toURI());
            if (file.isFile() && file.getName().endsWith(".jar")) {
                return Optional.of(file);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public static Optional<ReleaseInfo> fetchLatestRelease() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "FeralDeps-Updater");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);

            int code = conn.getResponseCode();
            if (code != 200) return Optional.empty();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();

            String json = sb.toString();
            String tag = extractField(json, "tag_name");

            List<ReleaseAsset> assets = parseAssets(json);
            ReleaseAsset best = selectBestAsset(assets);
            if (best == null) return Optional.empty();

            return Optional.of(new ReleaseInfo(tag, best.name, best.url));
        } catch (Exception e) {
            System.err.println("Update check failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static File createTempFileForAsset(String assetName) throws IOException {
        String suffix = getFileSuffix(assetName);
        if (suffix.isEmpty()) {
            suffix = ".bin";
        }
        return File.createTempFile("feraldeps-update-", suffix);
    }

    public static void openInstaller(File file) throws Exception {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
            return;
        }

        OsType os = detectOs();
        if (os == OsType.MAC) {
            new ProcessBuilder("open", file.getAbsolutePath()).start();
            return;
        }
        if (os == OsType.WINDOWS) {
            new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
            return;
        }
        if (os == OsType.LINUX) {
            new ProcessBuilder("xdg-open", file.getAbsolutePath()).start();
            return;
        }

        throw new IllegalStateException("Unsupported OS for installer launch");
    }

    public static File downloadReleaseAsset(String assetUrl, File targetFile) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(assetUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "FeralDeps-Updater");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new Exception("Download failed with HTTP " + code);
        }

        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        return targetFile;
    }

    public static void replaceJar(File currentJar, File downloadedJar) throws Exception {
        File backup = new File(currentJar.getParentFile(), currentJar.getName() + ".bak");
        Files.deleteIfExists(backup.toPath());

        Files.move(currentJar.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.move(downloadedJar.toPath(), currentJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(backup.toPath());
    }

    private static String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int index = json.indexOf(key);
        if (index == -1) return null;
        int start = json.indexOf("\"", index + key.length());
        if (start == -1) return null;
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end);
    }

    private static List<ReleaseAsset> parseAssets(String json) {
        List<ReleaseAsset> assets = new ArrayList<>();
        int idx = 0;
        while ((idx = json.indexOf("\"browser_download_url\"", idx)) != -1) {
            int urlStart = json.indexOf("\"", idx + 23);
            if (urlStart == -1) break;
            int urlEnd = json.indexOf("\"", urlStart + 1);
            if (urlEnd == -1) break;
            String urlVal = json.substring(urlStart + 1, urlEnd);

            String nameVal = null;
            int objStart = json.lastIndexOf("{", idx);
            if (objStart != -1) {
                int nameIdx = json.indexOf("\"name\"", objStart);
                if (nameIdx != -1 && nameIdx < idx) {
                    int nameStart = json.indexOf("\"", nameIdx + 6);
                    if (nameStart != -1) {
                        int nameEnd = json.indexOf("\"", nameStart + 1);
                        if (nameEnd != -1) {
                            nameVal = json.substring(nameStart + 1, nameEnd);
                        }
                    }
                }
            }

            assets.add(new ReleaseAsset(nameVal != null ? nameVal : "", urlVal));
            idx = urlEnd + 1;
        }
        return assets;
    }

    private static ReleaseAsset selectBestAsset(List<ReleaseAsset> assets) {
        if (assets.isEmpty()) {
            return null;
        }

        OsType os = detectOs();
        String arch = normalizeArch(System.getProperty("os.arch", ""));

        ReleaseAsset best = null;
        int bestScore = -1;
        for (ReleaseAsset asset : assets) {
            int score = scoreAsset(asset, os, arch);
            if (score > bestScore) {
                bestScore = score;
                best = asset;
            }
        }

        if (bestScore > 0) {
            return best;
        }

        return findJarAsset(assets);
    }

    private static ReleaseAsset findJarAsset(List<ReleaseAsset> assets) {
        for (ReleaseAsset asset : assets) {
            String name = asset.name.toLowerCase();
            String url = asset.url.toLowerCase();
            if (name.endsWith(".jar") || url.endsWith(".jar")) {
                return asset;
            }
        }
        return null;
    }

    private static int scoreAsset(ReleaseAsset asset, OsType os, String arch) {
        String name = asset.name.toLowerCase();
        String url = asset.url.toLowerCase();
        String candidate = name.isEmpty() ? url : name;

        if (!matchesOsExtension(candidate, os)) {
            return -1;
        }

        int archScore = archMatchScore(candidate, arch);
        if (archScore < 0) {
            return -1;
        }

        int score = 10;
        score += osTokenScore(candidate, os);
        score += archScore;
        return score;
    }

    private static boolean matchesOsExtension(String value, OsType os) {
        if (os == OsType.WINDOWS) {
            return value.endsWith(".msi") || value.endsWith(".exe");
        }
        if (os == OsType.MAC) {
            return value.endsWith(".dmg") || value.endsWith(".pkg");
        }
        if (os == OsType.LINUX) {
            return value.endsWith(".deb") || value.endsWith(".rpm") || value.endsWith(".appimage");
        }
        return value.endsWith(".jar");
    }

    private static int osTokenScore(String value, OsType os) {
        if (os == OsType.WINDOWS && (value.contains("win") || value.contains("windows"))) {
            return 5;
        }
        if (os == OsType.MAC && (value.contains("mac") || value.contains("osx") || value.contains("darwin"))) {
            return 5;
        }
        if (os == OsType.LINUX && (value.contains("linux") || value.contains("ubuntu") || value.contains("debian") || value.contains("fedora"))) {
            return 5;
        }
        return 0;
    }

    private static int archMatchScore(String value, String arch) {
        boolean hasArchToken = value.contains("arm64") || value.contains("aarch64") || value.contains("x64")
                || value.contains("x86_64") || value.contains("amd64") || value.contains("x86")
                || value.contains("i386") || value.contains("i686");

        if (!hasArchToken) {
            return 1;
        }

        if (arch.equals("arm64") && (value.contains("arm64") || value.contains("aarch64"))) {
            return 4;
        }
        if (arch.equals("x64") && (value.contains("x64") || value.contains("x86_64") || value.contains("amd64"))) {
            return 4;
        }
        if (arch.equals("x86") && (value.contains("x86") || value.contains("i386") || value.contains("i686"))) {
            return 4;
        }

        return -1;
    }

    private static OsType detectOs() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            return OsType.WINDOWS;
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return OsType.MAC;
        }
        if (osName.contains("nux") || osName.contains("nix") || osName.contains("linux")) {
            return OsType.LINUX;
        }
        return OsType.OTHER;
    }

    private static String normalizeArch(String arch) {
        String value = arch.toLowerCase();
        if (value.contains("aarch64") || value.contains("arm64")) {
            return "arm64";
        }
        if (value.contains("x86_64") || value.contains("amd64") || value.contains("x64")) {
            return "x64";
        }
        if (value.contains("x86") || value.contains("i386") || value.contains("i686")) {
            return "x86";
        }
        return value;
    }

    private static String getFileSuffix(String name) {
        int idx = name.lastIndexOf('.');
        if (idx == -1 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx);
    }
}
