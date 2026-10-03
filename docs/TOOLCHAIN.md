# Toolchain & Environment Verification

This document records the verified development environment, compilers, build tools, runtimes, and container toolchains for the **Enterprise Payment & Transaction Processing Platform**.

---

## 1. Verified Core Toolchain Components

| Tool / Runtime | Verified Version | Installation Path / Source | Status |
| :--- | :--- | :--- | :--- |
| **Java JDK** | `21.0.5 LTS` (Oracle HotSpot 64-Bit) | `C:\Program Files\Java\jdk-21` | **VERIFIED / ACTIVE** |
| **Apache Maven** | `3.9.9` | `C:\Users\LAPTOP\.tools\apache-maven-3.9.9\bin` | **VERIFIED / ACTIVE** |
| **Git** | `2.45.2.windows.1` | `C:\Program Files\Git\cmd\git.exe` | **VERIFIED / ACTIVE** |
| **Operating System** | Windows 11 (build 10.0, amd64) | Host OS | **VERIFIED / ACTIVE** |
| **Linux Subsystem** | WSL 2 (Ubuntu 2) | `wsl.exe` | **VERIFIED / ACTIVE** |
| **IDE / Editor** | Antigravity IDE / VS Code & IntelliJ IDEA compatible | Workspace | **VERIFIED / ACTIVE** |
| **Docker Engine** | Docker / WSL 2 / Testcontainers backend | Container Runtime (Required Phase 9/10) | **CONFIGURED / READY** |

---

## 2. Environment Variables & Paths

- `JAVA_HOME`: `C:\Program Files\Java\jdk-21`
- `MAVEN_HOME`: `C:\Users\LAPTOP\.tools\apache-maven-3.9.9`
- `PATH`: Includes JDK 21 `bin`, Maven 3.9.9 `bin`, Git `cmd`.

---

## 3. Verification Commands & Diagnostics

### Java 21 Check
```bash
$ java -version
java version "21.0.5" 2024-10-15 LTS
Java(TM) SE Runtime Environment (build 21.0.5+9-LTS-239)
Java HotSpot(TM) 64-Bit Server VM (build 21.0.5+9-LTS-239, mixed mode, sharing)
```

### Maven 3.9 Check
```bash
$ mvn -v
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: C:\Users\LAPTOP\.tools\apache-maven-3.9.9
Java version: 21.0.5, vendor: Oracle Corporation, runtime: C:\Program Files\Java\jdk-21
Default locale: en_US, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
```

### Git Check
```bash
$ git --version
git version 2.45.2.windows.1
```
