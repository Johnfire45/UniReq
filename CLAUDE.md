# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

UniReq is a Burp Suite extension (BApp) that deduplicates HTTP requests during security testing. It uses the Burp Montoya API and targets Burp Suite Pro v2023.12+.

## Build Commands

```bash
# Build the extension JAR
mvn clean package
# or use the provided scripts:
./build.sh       # macOS/Linux
build.bat        # Windows
```

Output: `target/unireq-deduplicator-1.0.0.jar`

**Prerequisites:** Java 11+, Maven 3.6+. No runtime dependencies — the Montoya API is `provided` scope (excluded from shaded JAR).

**Tests:** JUnit Jupiter 5.9.2 is available. Run with `mvn test`.

## Architecture

The extension follows a layered architecture:

### Entry Point
- `extension/UniReqExtension.java` — implements `BurpExtension`, initializes all components, registers the proxy listener
- `extension/RequestFingerprintListener.java` — intercepts HTTP requests/responses via Burp's proxy hooks, applies deduplication, updates the GUI

### Core Logic
- `core/FingerprintGenerator.java` — generates SHA-256 fingerprints with format `METHOD | NORMALIZED_PATH | HASH(CONTENT)`. Normalizes paths (lowercase, trims trailing slashes) and hashes body (POST/PUT/PATCH) or query string (GET)
- `core/RequestDeduplicator.java` — thread-safe deduplication using `ConcurrentSkipListSet`, capped at 1000 entries (FIFO eviction), atomic counters for stats
- `core/FilterEngine.java` — filters by method, status code, host, path/URL (with regex support), and Burp scope

### UI
- `ui/UniReqGui.java` — main GUI coordinator; uses a 250ms debounced refresh to avoid lag
- `ui/components/` — modular Swing components: `ControlPanel`, `ExportPanel`, `FilterPanel`, `FilterFieldPanel`, `RequestTablePanel`, `StatsPanel`, `UniReqFilterDialog`, `ViewerPanel`

### Export
- `export/ExportManager.java` — coordinates export operations
- Format exporters: `JsonExporter`, `CsvExporter`, `MarkdownExporter`

### Models
- `model/RequestResponseEntry.java` — wraps a complete HTTP transaction (request + response), fingerprint, timestamp, sequence number
- `model/FilterCriteria.java` — filter configuration (method, status, host, path, regex, scope, case sensitivity)
- `model/ExportConfiguration.java` — export job spec (format, path, entries, metadata flags)

## Key Design Decisions

- **Thread safety**: All shared state in `RequestDeduplicator` uses concurrent data structures and atomic operations. UI updates from non-EDT threads must go through `SwingUtilities.invokeLater`.
- **No external runtime dependencies**: The fat JAR (via Maven Shade Plugin) excludes the Montoya API, which Burp provides at runtime.
- **Burp integration points**: Repeater/Comparer sends, scope filtering, and HTTP editors use Montoya API interfaces passed in at initialization. Guard against null API references when Burp features are unavailable.
