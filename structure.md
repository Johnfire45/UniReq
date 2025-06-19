# UniReq Extension Architecture

## Overview

UniReq is a Burp Suite extension that filters duplicate HTTP requests using fingerprinting logic and provides an HTTP History-style interface for inspecting unique requests and responses. The extension is built using Java and the Montoya API.

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
│                           ├── UniReqExtension.java           (5.0KB, 127 lines)
│                           ├── RequestDeduplicator.java      (17KB, 464 lines)
│                           ├── RequestFingerprintListener.java (11KB, 255 lines)
│                           └── UniReqGui.java                (30KB, 767 lines)
└── target/                       # Maven build output (generated)
    ├── unireq-deduplicator-1.0.0.jar    # Final extension JAR
    ├── classes/                          # Compiled Java classes
    ├── maven-archiver/                   # Maven metadata
    ├── maven-status/                     # Build status
    └── generated-sources/                # Generated source files
```

### File Sizes and Complexity
- **UniReqGui.java**: 30KB, 767 lines - Largest file (HTTP History-style GUI implementation)
- **RequestDeduplicator.java**: 17KB, 464 lines - Core logic
- **RequestFingerprintListener.java**: 11KB, 255 lines - HTTP interception
- **UniReqExtension.java**: 5.0KB, 127 lines - Entry point and orchestration
- **Final JAR**: 25KB - Compiled extension ready for Burp Suite

### Package Structure
- **Package**: `com.burp.extension.unireq`
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

### 3. Interception Layer
- **RequestFingerprintListener**: HTTP request/response interceptor
  - Implements `ProxyRequestHandler` and `ProxyResponseHandler`
  - Intercepts requests for deduplication processing
  - Captures responses to complete request/response pairs
  - Updates GUI with real-time data

### 4. Presentation Layer
- **UniReqGui**: HTTP History-style user interface
  - Displays unique requests in a table format with Method, Path, Status, Timestamp columns
  - Provides Burp-native request/response editors with split-pane layout
  - Shows real-time statistics with native Swing components (no HTML rendering)
  - Implements auto-refresh for live updates (2-second intervals)
  - Features color-coded statistics and status indicators
  - Includes memory management with FIFO eviction display

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
- Store unique request/response pairs with memory management
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
- `RequestResponseEntry`: Inner class storing request, response, timestamp, and safe previews

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

### UniReqGui
**Role**: User interface and data visualization

**Responsibilities**:
- Display HTTP History-style interface with request table
- Provide Burp-native request/response editors
- Show real-time statistics and filtering controls
- Handle user interactions (toggle filtering, clear data, refresh)
- Implement auto-refresh timer for live updates
- Manage table selection and viewer synchronization

**Key Components**:
- `RequestTableModel`: Custom table model for request display
- `StatusCodeRenderer`: Custom cell renderer with color coding
- `HttpRequestEditor`/`HttpResponseEditor`: Burp-native content viewers
- Auto-refresh timer for real-time updates

## Data Flow

### Request Processing Flow
```
1. HTTP Request → RequestFingerprintListener.handleRequestReceived()
2. RequestFingerprintListener → RequestDeduplicator.isUniqueRequest()
3. RequestDeduplicator computes fingerprint and checks uniqueness
4. If unique: RequestDeduplicator.storeUniqueRequest() → FIFO queue
5. RequestFingerprintListener → UniReqGui.updateStatistics()
6. Request continues with annotation if duplicate
```

### Response Processing Flow
```
1. HTTP Response → RequestFingerprintListener.handleResponseReceived()
2. RequestFingerprintListener → RequestDeduplicator.updateResponse()
3. RequestDeduplicator matches response to stored request by fingerprint
4. Response data stored in RequestResponseEntry
5. RequestFingerprintListener → UniReqGui.updateStatistics()
```

### GUI Update Flow
```
1. Auto-refresh timer (2s interval) → UniReqGui.refreshDisplay()
2. UniReqGui.updateStatistics() → RequestDeduplicator statistics
3. UniReqGui.updateRequestTable() → RequestDeduplicator.getStoredRequests()
4. Table selection → UniReqGui.updateViewersForSelectedRequest()
5. Selected request/response → Burp native editors
```

### Memory Management Flow
```
1. New unique request → RequestDeduplicator.storeUniqueRequest()
2. Check queue size against MAX_STORED_REQUESTS (1000)
3. If over limit: FIFO eviction removes oldest entries
4. Content sanitization removes sensitive headers
5. Content truncation limits preview size to MAX_PREVIEW_LENGTH (10KB)
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

### Synchronization Points
- RequestDeduplicator methods are thread-safe through concurrent collections
- GUI updates are serialized through EDT
- No explicit synchronization blocks needed due to concurrent data structures

## Security Considerations

### Sensitive Data Protection
- Authorization headers are sanitized in previews (`[REDACTED]`)
- Cookie headers are sanitized in previews
- API keys and Bearer tokens are sanitized
- Only non-sensitive request metadata logged

### Memory Management
- FIFO eviction prevents unlimited memory growth
- Content truncation limits memory per entry
- In-memory only storage (no disk persistence)
- Cleanup on extension unload

### Error Handling
- Graceful degradation on fingerprint computation errors
- Exception handling prevents extension crashes
- Fallback fingerprints for error cases
- Comprehensive logging for debugging

## Performance Characteristics

### Memory Usage
- ~50-100 bytes per unique fingerprint
- ~1-10KB per stored request/response entry (with truncation)
- Maximum ~10MB for 1000 stored requests
- Automatic cleanup and FIFO eviction

### Processing Overhead
- SHA-256 computation: ~1ms per request
- Concurrent collections: minimal locking overhead
- GUI updates: 2-second intervals to reduce CPU usage
- Minimal impact on proxy performance

### Scalability
- Handles high-volume traffic through concurrent processing
- Memory-bounded through FIFO eviction
- Statistics tracking with atomic operations
- Efficient fingerprint lookup in sorted set

## Extension Points

### Customizable Components
- Fingerprint algorithm (currently SHA-256)
- Memory limits (MAX_STORED_REQUESTS, MAX_PREVIEW_LENGTH)
- Auto-refresh interval (REFRESH_INTERVAL_MS)
- Filtering behavior (annotation vs. blocking)

### Potential Enhancements
- Configurable fingerprint algorithms
- Export/import functionality
- Advanced filtering rules
- Custom request/response viewers
- Integration with other Burp tools 