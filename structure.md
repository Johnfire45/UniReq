# UniReq Extension Architecture

## Overview

UniReq is a Burp Suite extension that filters duplicate HTTP requests using fingerprinting logic and provides an HTTP History-style interface for inspecting unique requests and responses. The extension is built using Java and the Montoya API with a clean, modular architecture featuring proper separation of concerns.

## Project Structure

### Directory Layout
```
UniReq/
├── README.md                     # User guide and installation instructions
├── context.md                    # Project goals and use cases
├── structure.md                  # Technical architecture (this file)
├── pom.xml                       # Maven build configuration
├── build.sh                      # Unix/Linux build script
├── build.bat                     # Windows build script
├── src/                          # Source code directory
│   └── main/
│       └── java/
│           └── com/
│               └── burp/
│                   └── extension/
│                       └── unireq/
│                           ├── UniReqExtension.java                  (5.0KB, 127 lines)
│                           ├── RequestDeduplicator.java             (15KB, 401 lines)
│                           ├── RequestFingerprintListener.java      (11KB, 255 lines)
│                           ├── UniReqGui.java                       (104KB, 2522 lines)
│                           └── model/                               # Data models package
│                               ├── RequestResponseEntry.java       (5.9KB, 198 lines)
│                               ├── FilterCriteria.java             (7.2KB, 221 lines)
│                               └── ExportConfiguration.java        (8.4KB, 273 lines)
└── target/                       # Maven build output (generated)
    ├── unireq-deduplicator-1.0.0.jar    # Final extension JAR
    ├── classes/                          # Compiled Java classes
    ├── maven-archiver/                   # Maven metadata
    ├── maven-status/                     # Build status
    └── generated-sources/                # Generated source files
```

### File Sizes and Complexity
- **UniReqGui.java**: 104KB, 2522 lines - Largest file (comprehensive HTTP History-style GUI implementation)
- **RequestDeduplicator.java**: 15KB, 401 lines - Core deduplication logic
- **RequestFingerprintListener.java**: 11KB, 255 lines - HTTP interception
- **ExportConfiguration.java**: 8.4KB, 273 lines - Export settings model
- **FilterCriteria.java**: 7.2KB, 221 lines - Filter configuration model
- **RequestResponseEntry.java**: 5.9KB, 198 lines - HTTP request/response data model
- **UniReqExtension.java**: 5.0KB, 127 lines - Entry point and orchestration
- **Final JAR**: ~25KB - Compiled extension ready for Burp Suite

### Package Structure
- **Main Package**: `com.burp.extension.unireq`
- **Model Package**: `com.burp.extension.unireq.model`
- **Namespace**: Follows Burp extension conventions
- **Dependencies**: Only Montoya API (no external libraries)
- **Build Tool**: Maven with standard directory layout

## Extension Layers

### 1. Entry Point Layer
- **UniReqExtension**: Main extension entry point implementing `BurpExtension`
  - Initializes all components
  - Registers GUI tabs and proxy handlers
  - Manages extension lifecycle and cleanup

### 2. Core Logic Layer
- **RequestDeduplicator**: Core deduplication engine
  - Computes SHA-256 fingerprints for HTTP requests
  - Maintains thread-safe storage of seen fingerprints
  - Implements FIFO memory management (1000 request limit)
  - Provides statistics and filtering state management
  - Works with `RequestResponseEntry` model for data storage

### 3. Interception Layer
- **RequestFingerprintListener**: HTTP request/response interceptor
  - Implements `ProxyRequestHandler` and `ProxyResponseHandler`
  - Intercepts requests for deduplication processing
  - Captures responses to complete request/response pairs
  - Updates GUI with real-time data

### 4. Data Model Layer
- **RequestResponseEntry**: Immutable data container for HTTP request/response pairs
  - Stores request, response, timestamp, and metadata
  - Provides safe content previews with sensitive data sanitization
  - Implements proper encapsulation and data validation
- **FilterCriteria**: Configuration object for advanced filtering
  - Encapsulates all filter settings (method, status, host, path, etc.)
  - Provides validation and default values
  - Supports regex and case-sensitive options
- **ExportConfiguration**: Settings for multi-format data export
  - Defines export format, data inclusion options, and file settings
  - Validates export parameters and provides format-specific defaults
  - Supports CSV, HTML, Markdown, and JSON formats

### 5. Presentation Layer
- **UniReqGui**: HTTP History-style user interface with advanced interaction capabilities
  - Displays unique requests in a table format with Method, Path, Status, Timestamp columns
  - Provides comprehensive HTTP History filter panel (collapsible)
  - Supports advanced filtering by method, status, host, path, and response presence
  - Includes regex and case-sensitive filtering options
  - Provides Burp-native request/response editors with split-pane layout
  - Shows real-time statistics with native Swing components (no HTML rendering)
  - Implements auto-refresh for live updates (2-second intervals)
  - Features color-coded statistics and status indicators
  - Includes memory management with FIFO eviction display
  - **Context menu integration with Burp tools (Repeater, Intruder, Comparer)**
  - **Multi-format export system (CSV, HTML, Markdown, JSON)**
  - **Advanced clipboard operations (URL copy, cURL generation)**
  - **Multi-select support with batch operations**

## Class Roles and Responsibilities

### UniReqExtension
**Role**: Extension lifecycle manager and component orchestrator

**Responsibilities**:
- Initialize Montoya API integration
- Create and wire all extension components
- Register GUI tabs and proxy handlers
- Handle extension cleanup and resource management

**Key Methods**:
- `initialize(MontoyaApi)`: Main initialization entry point
- `cleanup()`: Resource cleanup on extension unload

### RequestDeduplicator
**Role**: Core deduplication engine and data storage

**Responsibilities**:
- Compute unique fingerprints using `METHOD | NORMALIZED_PATH | HASH(CONTENT)` format
- Maintain thread-safe storage of seen fingerprints using `ConcurrentSkipListSet`
- Store unique request/response pairs with memory management using `RequestResponseEntry` model
- Provide filtering state management and statistics
- Implement security measures (sanitize sensitive headers, truncate long content)

**Key Methods**:
- `isUniqueRequest(HttpRequest)`: Main deduplication logic
- `storeUniqueRequest()`: Store requests for GUI display with FIFO eviction
- `updateResponse()`: Associate responses with stored requests
- `getStoredRequests()`: Provide data for GUI table

**Data Structures**:
- `ConcurrentSkipListSet<String>`: Thread-safe fingerprint storage
- `ConcurrentLinkedQueue<RequestResponseEntry>`: FIFO queue for request/response pairs
- Uses `RequestResponseEntry` model for type-safe data storage

### RequestFingerprintListener
**Role**: HTTP traffic interceptor and data collector

**Responsibilities**:
- Intercept HTTP requests via `ProxyRequestHandler`
- Intercept HTTP responses via `ProxyResponseHandler`
- Apply deduplication logic to incoming requests
- Capture and associate responses with requests
- Trigger GUI updates for real-time display
- Handle annotation of duplicate requests

**Key Methods**:
- `handleRequestReceived()`: Process incoming requests for deduplication
- `handleResponseReceived()`: Capture responses and update stored entries
- `handleRequestToBeSent()`: Pre-send request processing (currently pass-through)
- `handleResponseToBeSent()`: Pre-send response processing (currently pass-through)

### Model Classes

#### RequestResponseEntry
**Role**: Immutable data container for HTTP request/response pairs

**Responsibilities**:
- Store HTTP request, response, timestamp, and metadata
- Provide safe content previews with sensitive data sanitization
- Implement proper encapsulation and data validation
- Support JSON serialization for export functionality

**Key Features**:
- Immutable design for thread safety
- Sensitive header sanitization (Authorization, Cookie, etc.)
- Content truncation for memory efficiency
- Builder pattern for flexible construction

#### FilterCriteria
**Role**: Configuration object for advanced filtering

**Responsibilities**:
- Encapsulate all filter settings (method, status, host, path, etc.)
- Provide validation and default values
- Support regex and case-sensitive options
- Enable complex filter combinations

**Key Features**:
- Type-safe filter configuration
- Validation of filter parameters
- Support for multiple filter types
- Default value management

#### ExportConfiguration
**Role**: Settings for multi-format data export

**Responsibilities**:
- Define export format, data inclusion options, and file settings
- Validate export parameters and provide format-specific defaults
- Support CSV, HTML, Markdown, and JSON formats
- Handle UTF-8 encoding and file operations

**Key Features**:
- Multi-format export support
- Configurable data inclusion (basic/full)
- File path and encoding management
- Format-specific validation

### UniReqGui
**Role**: User interface and data visualization with advanced interaction capabilities

**Responsibilities**:
- Display HTTP History-style interface with request table
- Provide Burp-native request/response editors
- Show real-time statistics and filtering controls
- Handle user interactions (toggle filtering, clear data, refresh, export)
- Implement auto-refresh timer for live updates
- **Manage multi-select table functionality and viewer synchronization**
- **Provide context menu integration with Burp tools**
- **Support multi-format data export functionality with JSON validation**
- **Utilize model classes for type-safe data handling**

**Key Components**:
- `RequestTableModel`: Custom table model for request display using `RequestResponseEntry`
- `StatusCodeRenderer`: Custom cell renderer with color coding
- `HttpRequestEditor`/`HttpResponseEditor`: Burp-native content viewers
- HTTP History filter panel with comprehensive filtering using `FilterCriteria`
- Real-time filter application with regex and case-sensitive support
- Auto-refresh timer for real-time updates
- **Context menu system with Burp tool integration**
- **Export dialog and multi-format export engine using `ExportConfiguration`**

**Context Menu Features**:
- Send to Repeater, Intruder, and Comparer tools with multi-select support
- Copy URL and generate cURL commands with proper header handling
- Request highlighting and commenting (placeholders for future enhancement)
- Request deletion from table (placeholder for future enhancement)
- **Batch operations**: All context menu actions support multiple selected requests

**Export Capabilities**:
- **CSV Export**: Comma-separated values with proper escaping for spreadsheet analysis
- **HTML Export**: Styled reports with color-coded status codes and responsive layout
- **Markdown Export**: GitHub-flavored markdown for documentation and reporting
- **JSON Export**: Structured data format with built-in validation for programmatic processing
- **Configurable Data Inclusion**: Basic metadata or full request/response content
- **UTF-8 Encoding**: Proper character encoding for international content
- **File Dialog Integration**: User-friendly file selection and naming
- **Smart Selection Export**: Automatically exports selected requests or all filtered requests
- **JSON Validation**: Pre-export validation prevents syntax errors in generated files

## Data Flow

### Request Processing Flow
```
1. HTTP Request → RequestFingerprintListener.handleRequestReceived()
2. RequestFingerprintListener → RequestDeduplicator.isUniqueRequest()
3. RequestDeduplicator computes fingerprint and checks uniqueness
4. If unique: RequestDeduplicator.storeUniqueRequest() → FIFO queue (RequestResponseEntry)
5. RequestFingerprintListener → UniReqGui.updateStatistics()
6. Request continues with annotation if duplicate
```

### Response Processing Flow
```
1. HTTP Response → RequestFingerprintListener.handleResponseReceived()
2. RequestFingerprintListener → RequestDeduplicator.updateResponse()
3. RequestDeduplicator matches response to stored request by fingerprint
4. Response data stored in RequestResponseEntry model
5. RequestFingerprintListener → UniReqGui.updateStatistics()
```

### GUI Update Flow
```
1. Auto-refresh timer (2s interval) → UniReqGui.refreshDisplay()
2. UniReqGui.updateStatistics() → RequestDeduplicator statistics
3. UniReqGui.updateRequestTable() → RequestDeduplicator.getStoredRequests() (List<RequestResponseEntry>)
4. Table selection → UniReqGui.updateViewersForSelectedRequests()
5. Selected request/response → Burp native editors with navigation controls
```

### Multi-Select Interaction Flow
```
1. User selects multiple rows (Ctrl/Cmd + click, Shift + click)
2. UniReqGui.updateViewersForSelectedRequests() → selectedEntries list updated (List<RequestResponseEntry>)
3. UniReqGui.updateCurrentViewer() → displays first selected request
4. Navigation buttons → UniReqGui.navigateToNextRequest() / navigateToPreviousRequest()
5. Selection info updated → "Showing X of Y selected requests"
6. Context menu actions → operate on all selected entries
7. Export operations → export only selected entries (if any selected) using ExportConfiguration
```

### Export Data Flow
```
1. User initiates export → UniReqGui.showExportDialog()
2. Export dialog → ExportConfiguration created with user preferences
3. UniReqGui.getCurrentTableEntries() → List<RequestResponseEntry> for export
4. Export engine → format-specific export method (CSV/HTML/Markdown/JSON)
5. ExportConfiguration validates settings and file parameters
6. Data serialization → RequestResponseEntry provides safe data access
7. File writing → UTF-8 encoding with error handling
8. JSON validation → syntax check for JSON exports
```

### Memory Management Flow
```
1. New unique request → RequestDeduplicator.storeUniqueRequest()
2. Check queue size against MAX_STORED_REQUESTS (1000)
3. If over limit: FIFO eviction removes oldest RequestResponseEntry objects
4. Content sanitization → RequestResponseEntry handles sensitive header removal
5. Content truncation → RequestResponseEntry limits preview size to MAX_PREVIEW_LENGTH (10KB)
```

## Thread Safety

### Concurrent Data Structures
- `ConcurrentSkipListSet<String>`: Thread-safe fingerprint storage
- `ConcurrentLinkedQueue<RequestResponseEntry>`: Thread-safe request/response queue
- `AtomicBoolean`/`AtomicLong`: Thread-safe state and statistics

### EDT (Event Dispatch Thread) Management
- All GUI updates performed on EDT using `SwingUtilities.invokeLater()`
- Auto-refresh timer executes on background thread but updates GUI on EDT
- User interactions handled on EDT

### Model Thread Safety
- `RequestResponseEntry`: Immutable objects are inherently thread-safe
- `FilterCriteria`: Immutable configuration objects
- `ExportConfiguration`: Immutable settings objects
- No shared mutable state in model classes

### Synchronization Points
- RequestDeduplicator methods are thread-safe through concurrent collections
- GUI updates are serialized through EDT
- Model objects eliminate need for explicit synchronization
- No explicit synchronization blocks needed due to concurrent data structures and immutable models

## Security Considerations

### Sensitive Data Protection
- Authorization headers are sanitized in previews (`[REDACTED]`)
- Cookie headers are sanitized in previews
- API keys and Bearer tokens are sanitized
- Only non-sensitive request metadata logged
- `RequestResponseEntry` handles sanitization automatically

### Memory Management
- FIFO eviction prevents unlimited memory growth
- Content truncation limits memory per entry
- In-memory only storage (no disk persistence)
- Cleanup on extension unload
- Model objects are immutable, preventing accidental data modification

### Error Handling
- Graceful degradation on fingerprint computation errors
- Exception handling prevents extension crashes
- Fallback fingerprints for error cases
- Comprehensive logging for debugging
- Model validation prevents invalid data states

## Performance Characteristics

### Memory Usage
- ~50-100 bytes per unique fingerprint
- ~1-10KB per stored `RequestResponseEntry` (with truncation)
- Maximum ~10MB for 1000 stored requests
- Automatic cleanup and FIFO eviction
- Immutable models reduce memory fragmentation

### Processing Overhead
- SHA-256 computation: ~1ms per request
- Concurrent collections: minimal locking overhead
- GUI updates: 2-second intervals to reduce CPU usage
- Minimal impact on proxy performance
- Model object creation: minimal overhead due to efficient design

### Scalability
- Handles high-volume traffic through concurrent processing
- Memory-bounded through FIFO eviction
- Statistics tracking with atomic operations
- Efficient fingerprint lookup in sorted set
- Type-safe model operations prevent runtime errors

## Extension Points

### Customizable Components
- Fingerprint algorithm (currently SHA-256)
- Memory limits (MAX_STORED_REQUESTS, MAX_PREVIEW_LENGTH)
- Auto-refresh interval (REFRESH_INTERVAL_MS)
- Filtering behavior (annotation vs. blocking)
- Export formats and configurations via `ExportConfiguration`
- Filter criteria via `FilterCriteria` model

### Model Extensibility
- `RequestResponseEntry`: Can be extended with additional metadata
- `FilterCriteria`: Easy to add new filter types
- `ExportConfiguration`: Simple to add new export formats
- Clean interfaces for adding new model classes

### Potential Enhancements
- Configurable fingerprint algorithms
- Export/import functionality with enhanced model support
- Advanced filtering rules using extended `FilterCriteria`
- Custom request/response viewers
- Integration with other Burp tools
- Plugin architecture using model-based configuration

## Architecture Benefits

### Maintainability
- **Separation of Concerns**: Clear boundaries between logic, data, and presentation
- **Type Safety**: Strong typing with dedicated model classes prevents runtime errors
- **Testability**: Models can be unit tested independently
- **Documentation**: Self-documenting code through model structure

### Extensibility
- **Model-Based Design**: Easy to add new data types and configurations
- **Immutable Models**: Safe to pass between components without side effects
- **Clean Interfaces**: Well-defined contracts between layers
- **Plugin-Ready**: Architecture supports future plugin system

### Reliability
- **Data Integrity**: Models enforce data validation and consistency
- **Thread Safety**: Immutable models eliminate concurrency issues
- **Error Prevention**: Type safety catches errors at compile time
- **Resource Management**: Clear ownership and lifecycle management 