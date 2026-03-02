# feraldeps-core

Open‑source, local dependency and vulnerability scanner for Java projects.  The
primary deliverable is a desktop GUI (`.jar` + macOS `.dmg` bundle); most of the
work (parsing your project files and generating reports) runs locally.  However,
to check for newer versions, vulnerabilities and CVSS severity scores the tool
does make outbound HTTP requests to public APIs such as Maven Central, the OSV
(vulnerability) service and various CVSS providers (OSS Index, NVD, GitHub,
etc.).

## Features

- Scans local Gradle/Maven projects for declared dependencies
- Detects outdated versions and known vulnerabilities
- Generates an HTML/CSV report
- Simple GUI with manual update checks

## Building and running

```bash
# ensure you are in the repository root
cd /path/to/feraldeps-core/feraldeps-core

# compile and package
mvn clean package

# after a successful build the runnable jar is located at:
# target/feraldeps-<version>.jar
java -jar target/feraldeps-*.jar
```

## Ignored files
See `.gitignore` for details; build artifacts live under `target/` and are
excluded from version control.

## Contributing
Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting
issues or pull requests.

## Code of Conduct
All contributors are expected to abide by the
[Code of Conduct](CODE_OF_CONDUCT.md).