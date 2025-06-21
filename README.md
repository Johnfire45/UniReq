# 🧠 UniReq - Unique Request Deduplicator for Burp Suite

**UniReq** is a lightweight Burp Suite extension that detects and deduplicates HTTP requests in real time — helping you eliminate noise, focus on unique requests, and export structured data for analysis or reporting.

---

## 🚀 Key Features

- ✅ **Automatic deduplication** of HTTP requests
- ✅ **Real-time filtering** by host, method, status, response, and highlights
- ✅ **Column sorting** - Click any column header to sort (Req#, Method, Host, Path, Status)
- ✅ **Multi-select support** with right-click context menu actions
- ✅ **Export to CSV / Markdown / JSON / HTML**
- ✅ **Modern UI** with rounded fields, compact layout, and theming
- ✅ **Lightweight & fast**, no external dependencies

---

## 🖥️ UI Overview

| Section            | Description |
|--------------------|-------------|
| **Filters**        | Filter by host (regex), method, status, show mode |
| **Main Table**     | Displays unique requests with sequence number. 🔽 Click headers to sort |
| **Request Viewers**| Request/Response preview panel |
| **Controls**       | Enable/disable filtering, refresh, clear |
| **Export Panel**   | Choose format and export selected requests |
| **Stats**          | View total, unique, and duplicate count |

---

## 📤 Export Options

- **Formats**: JSON, CSV, Markdown, HTML
- **Export Scope**:
  - All requests
  - Only selected requests
- **Security**:
  - CSV Injection protection (prefixes `=`, `@`, `+`, `-`)
  - UTF-8 encoding
  - Markdown escaping

---

## 🧠 How It Works

1. **Capture Mode**:
   - Listens to Burp Proxy, Repeater, Scanner, etc.
   - Computes hash fingerprint per request
2. **Deduplication Engine**:
   - Checks for uniqueness based on normalized fingerprint
   - Discards duplicates in real-time
3. **Filter Layer**:
   - UI filters operate on deduplicated view
   - Multi-criteria filter system
4. **Sorting Layer**:
   - Click column headers to sort by Req#, Method, Host, Path, or Status
   - Numeric sorting for Req# and Status, alphabetical for text columns
   - Sorting works with filtered results
5. **Export Layer**:
   - Modular exporters generate output in desired format
   - Respects filters, sorting, and selection scope

---

## 📦 Installation

1. **Build** the JAR using Maven:
   ```bash
   mvn clean package
   ```

2. **Load in Burp Suite**:
   - Go to *Extender > Extensions*
   - Click **Add**
   - Select the `target/unireq-deduplicator-1.0.0.jar`

3. The **UniReq** tab will appear in your Burp UI.

---

## 🛠️ Developer Guide

### 📁 Folder Structure

```
src/
├── core/                 # Core deduplication logic
├── model/                # Data models (RequestResponseEntry, FilterCriteria, etc.)
├── export/               # Modular exporters (CSV, Markdown, JSON, HTML)
├── extension/            # Burp Extender entry point
├── ui/                   # Swing-based UI components
└── utils/                # Swing helpers, fingerprint generator
```

### 🔄 Architecture

- **RequestDeduplicator**: Core engine that hashes and deduplicates requests
- **FilterEngine**: Evaluates UI filters against current requests
- **ExportManager**: Delegates to exporter modules for file generation
- **UI Components**: Modular Swing panels for layout and styling

---

## ✨ Example Output

### ✅ CSV

```csv
"Method","Host","Path","Status","Fingerprint"
"GET","example.com","/login","200","abc123..."
```

### ✅ Markdown

```markdown
## Export Summary
| Method | Host | Path | Status |
|--------|------|------|--------|
| GET | example.com | /login | 200 |
```

---

## 🧪 Testing

- ✅ Manual testing via Burp Proxy and Repeater
- ✅ Table updates in real-time with filters applied
- ✅ Exported files verified in all formats

---

## 🏁 Roadmap

- [ ] UI theming (light/dark toggle)
- [ ] JSON diff for response comparison
- [ ] Column visibility settings
- [ ] Full test automation and CI setup

---

## 📄 License

MIT License © Harshit Shah  
Built for security researchers, testers, and automation enthusiasts.

---
```