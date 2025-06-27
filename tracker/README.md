# 🧠 UniReq - Unique Request Deduplicator for Burp Suite

**UniReq** is a lightweight Burp Suite extension that detects and deduplicates HTTP requests in real time — helping you eliminate noise, focus on unique requests, and export structured data for analysis or reporting.

---

## 🚀 Key Features

- ✅ **Automatic deduplication** of HTTP requests using SHA-256 fingerprinting
- ✅ **Real-time filtering** by host, method, status, response, and highlights with **labeled checkboxes**
- ✅ **Professional filter bar** with `[☐ Regex] [☐ Case] [☐ Invert]` instead of cryptic icons
- ✅ **Advanced filtering system** with comprehensive modal dialog and MIME type/scope support
- ✅ **Responsive table layout** with fixed column widths and dynamic Path column
- ✅ **Column sorting** - Click any column header to sort (Req#, Method, Host, Path, Status)
- ✅ **Multi-select support** with right-click context menu actions
- ✅ **Smart export system** with scope selection (All Visible vs Selected Only) and **accurate mapping**
- ✅ **Export to CSV / Markdown / JSON / HTML** with security-focused escaping
- ✅ **Enhanced statistics** showing Total, Unique, Duplicates, and **synchronized Visible counts**
- ✅ **Modern UI** with polished layout, visual feedback, and professional design
- ✅ **Lightweight & fast** - optimized 121KB JAR (vs original 1.6MB), no external dependencies

---

## 🖥️ UI Overview

| Section            | Description |
|--------------------|-------------|
| **Title & Stats**  | Extension title with real-time statistics (Total, Unique, Duplicates, Visible: X of Y) |
| **Filters**        | Filter by host (regex), method, status, show mode with **Reset button** for quick clearing |
| **Main Table**     | Responsive table with fixed column widths, no right-side whitespace. 🔽 Click headers to sort |
| **Request Viewers**| Split-pane Request/Response preview with target host display |
| **Controls**       | Enable/disable filtering, refresh, clear with status feedback |
| **Export Panel**   | Format selection + scope dropdown (All Visible/Selected Only) with smart state management |

### 🎯 **Recent UI Enhancements**
- **Professional Filter Bar**: Replaced cryptic `[.*] [Aa] [!]` with clear `[☐ Regex] [☐ Case] [☐ Invert]` checkboxes
- **Advanced Filtering System**: Comprehensive modal dialog with MIME type, scope, and extension filters
- **MIME Type Integration**: Real Content-Type header parsing with Burp scope checking
- **Visual Polish**: Forest green/amber active states, bold Host label, optimized spacing
- **Responsive Layout**: GridBagLayout with logical grouping and professional alignment
- **Fixed Export Mapping**: Export correctly maps filtered/sorted requests eliminating wrong data exports
- **Synchronized Visible Count**: "Visible: X" display perfectly matches actual export count
- **JAR Optimization**: Reduced from 1.6MB to 121KB (93% reduction) for BApp Store submission
- **Smart State Management**: Export controls adapt to data availability and selection state

---

## 📤 Export Options

- **Formats**: JSON, CSV, Markdown, HTML with format-specific optimizations
- **Export Scope**:
  - **All Visible Requests**: Exports currently filtered/visible requests
  - **Only Selected Requests**: Exports only selected table rows (auto-disabled when no selection)
- **Security Features**:
  - CSV Injection protection (prefixes `=`, `@`, `+`, `-`)
  - JSON control character escaping (0x00-0x1F as unicode)
  - Markdown special character escaping
  - UTF-8 encoding with full international character support
- **User Experience**:
  - Smart button states with context-aware tooltips
  - Full absolute file path feedback on successful export
  - Thread-safe operations with proper error handling

---

## 🧠 How It Works

1. **Capture Mode**:
   - Listens to Burp Proxy, Repeater, Scanner, etc.
   - Computes SHA-256 fingerprint per request: `METHOD | NORMALIZED_PATH | HASH(CONTENT)`
2. **Deduplication Engine**:
   - Checks for uniqueness based on normalized fingerprint
   - Discards duplicates in real-time with thread-safe operations
3. **Filter Layer**:
   - UI filters operate on deduplicated view
   - Multi-criteria filter system with regex support
4. **Responsive Table**:
   - Fixed widths for key columns (Req#: 40px, Method: 60px, Status: 60px)
   - Dynamic Path column fills remaining viewport space
   - Sort state preserved during refreshes
5. **Export Layer**:
   - Modular exporters generate output in desired format
   - Respects filters, sorting, and selection scope
   - Intelligent state management with user feedback

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

3. The **UniReq** tab will appear in your Burp UI with enhanced layout and functionality.

---

## 🛠️ Developer Guide

### 📁 Folder Structure

```
src/
├── core/                 # Core deduplication logic with thread-safe operations
├── model/                # Immutable data models (RequestResponseEntry, FilterCriteria, etc.)
├── export/               # Modular exporters (CSV, Markdown, JSON, HTML) with security focus
├── extension/            # Burp Extender entry point with Montoya API integration
├── ui/                   # Enhanced Swing-based UI components
│   └── components/       # Modular UI components with smart state management
└── utils/                # Swing helpers, HTTP utilities, fingerprint generator
```

### 🔄 Enhanced Architecture

- **RequestDeduplicator**: Thread-safe core engine with FIFO memory management
- **FilterEngine**: Advanced filtering with regex support and multiple criteria
- **ExportManager**: Intelligent export coordination with scope-aware operations
- **UI Components**: Modular Swing panels with responsive design and state management
- **RequestTablePanel**: Enhanced table with fixed columns and dynamic Path column sizing
- **ExportPanel**: Smart export controls with scope selection and state adaptation
- **StatsPanel**: Real-time statistics with "Visible: X of Y" format

---

## ✨ Example Output

### ✅ CSV (with injection protection)

```csv
"Method","Host","Path","Status","Fingerprint"
"GET","example.com","/login","200","abc123..."
"POST","api.example.com","/auth","201","def456..."
```

### ✅ Markdown (GitHub-flavored)

```markdown
## UniReq Export Summary
**Export Date**: 2024-01-15 14:30:25  
**Total Requests**: 2  
**Export Scope**: All Visible Requests

| Method | Host | Path | Status | Target |
|--------|------|------|--------|--------|
| GET | example.com | /login | 200 | https://example.com |
| POST | api.example.com | /auth | 201 | https://api.example.com |
```

---

## 🧪 Testing & Quality Assurance

### ✅ **Build Status**
- **Compilation**: Successful (`mvn clean compile -q`)
- **Packaging**: Successful (`mvn clean package -q`)
- **No Regressions**: All existing functionality preserved
- **Thread Safety**: All UI updates properly synchronized

### ✅ **UI Testing Priorities**
- **Table Layout**: Fixed column widths with responsive Path column
- **Export Functionality**: Both "All Visible" and "Selected Only" modes
- **State Management**: Export controls adapt to selection changes
- **Statistics Display**: Accurate "Visible: X of Y" counts
- **Sort Persistence**: Sorting maintained during data refreshes

---

## 🏁 Roadmap

### ✅ **Recently Completed**
- [x] **Fixed export mapping bug** - Export correctly maps filtered/sorted requests
- [x] **Synchronized visible count** - Display matches actual filtered data used for export  
- [x] **Reset Filters button** - One-click filter clearing with automatic refresh
- [x] Fixed table layout with responsive columns
- [x] Export scope dropdown (All Visible vs Selected Only)
- [x] Enhanced statistics with visible count display
- [x] Sort state persistence during refreshes
- [x] Compact UI design with optimized spacing
- [x] Smart export state management

### 🔄 **In Progress**
- [ ] UI theming (light/dark toggle)
- [ ] Advanced filtering with saved filter sets
- [ ] Request highlighting and annotation system

### 📋 **Future Enhancements**
- [ ] JSON diff for response comparison
- [ ] Column visibility settings
- [ ] Custom fingerprinting algorithms
- [ ] Full test automation and CI setup
- [ ] Integration APIs for external tools

---

## 📄 License

MIT License © Harshit Shah  
Built for security researchers, testers, and automation enthusiasts.

**Version**: 1.0.0 (BApp Store Ready - Professional UI Polish Complete)  
**JAR Size**: 121KB (optimized from 1.6MB)  
**Last Updated**: January 2024

---
```