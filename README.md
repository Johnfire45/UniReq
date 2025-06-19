# UniReq - HTTP Request Deduplicator for Burp Suite

**Author: Harshit Shah**

A Burp Suite extension that filters duplicate HTTP requests by computing unique fingerprints based on request structure and content. Built using Java and the Montoya API with a clean, modular architecture featuring proper separation of concerns and enhanced user experience.

## 🎯 Purpose

UniReq helps security testers and developers by:
- **Reducing noise** in HTTP history by filtering duplicate requests
- **Improving efficiency** during manual testing and automated scanning
- **Providing visibility** into request patterns and duplication statistics
- **Maintaining performance** through intelligent fingerprinting algorithms
- **Ensuring code quality** with modular architecture and type-safe data models
- **Enhancing user experience** with smart controls and professional interface

## ✨ Features

### Core Functionality
- **Smart Fingerprinting**: Computes unique fingerprints using `METHOD | NORMALIZED_PATH | HASH(CONTENT)`
- **Thread-Safe Operation**: Uses concurrent collections for safe multi-threaded operation
- **Flexible Filtering**: Toggle filtering on/off without losing stored fingerprints
- **Real-Time Statistics**: Live updates of request counts and duplication metrics
- **Modular Architecture**: Clean separation of concerns with dedicated model classes

### Enhanced User Interface
- **Smart Export Controls**: Export button automatically disabled when no data available
- **Context-Aware Tooltips**: Dynamic tooltips showing request counts and availability status
- **Target Host Display**: Shows target host information above response viewer
- **Read-Only Editors**: Prevents accidental modifications while maintaining full functionality
- **Professional Layout**: Consistent spacing and alignment throughout the interface
- **Full Path Feedback**: Export success messages display complete file paths

### Fingerprint Algorithm
- **HTTP Method**: GET, POST, PUT, DELETE, etc.
- **Normalized Path**: Lowercased with trailing slashes removed
- **Content Hash**: SHA-256 of request body (POST/PUT) or query string (GET)
- **Examples**:
  - `GET | /api/users | EMPTY`
  - `POST | /api/login | a1b2c3d4e5f6...` (SHA-256 of body)
  - `GET | /search | e5f6g7h8i9j0...` (SHA-256 of query string)

### User Interface
- **Dedicated Tab**: Clean, organized interface in Burp's main window
- **Control Panel**: Enable/disable filtering, clear fingerprints, refresh stats
- **Export Panel**: Smart export controls with format selection and state management
- **Statistics Display**: Real-time counters with color-coded indicators
- **Status Monitoring**: Current filtering state and system status

## 🛠️ Installation

### Prerequisites
- **Burp Suite Professional or Community Edition**
- **Java 11 or higher**
- **Maven 3.6+** (for building from source)

### Method 1: Build from Source
```bash
# Clone the repository
git clone <repository-url>
cd UniReq

# Build the extension
mvn clean compile package

# The JAR file will be created at: target/unireq-deduplicator-1.0.0.jar
```

### Method 2: Download Pre-built JAR
1. Download the latest release JAR from the releases page
2. Save it to a convenient location on your system

### Loading into Burp Suite
1. Open Burp Suite
2. Go to **Extensions** → **Installed**
3. Click **Add**
4. Select **Extension type**: Java
5. Choose the JAR file: `unireq-deduplicator-1.0.0.jar`
6. Click **Next** to load the extension

## 🚀 Usage

### Getting Started
1. After loading the extension, you'll see a new **"UniReq"** tab in Burp's interface
2. The extension starts with filtering **enabled** by default
3. HTTP requests passing through Burp's proxy will be automatically processed

### Interface Overview

The UniReq extension provides a comprehensive HTTP History-style interface with enhanced user experience:

#### Top Panel: Controls and Statistics
- **Controls Section**:
  - **Disable/Enable Filtering**: Toggle request filtering on/off
  - **Filter**: Show/hide the HTTP History filter panel
  - **Refresh**: Manually update the display
  - **Clear All Data**: Reset all stored requests and statistics (with confirmation)
  
- **Statistics Section** (real-time, color-coded):
  - **Unique**: Number of unique requests captured (green)
  - **Duplicates**: Number of duplicate requests filtered (red)
  - **Stored**: Total requests stored in memory (blue)
  - **Filtering**: Current status - ENABLED/DISABLED (green/red)

#### Export Panel: Smart Export Controls
- **Format Selection**: Choose from JSON, CSV, HTML, or Markdown formats
- **Smart Export Button**: 
  - **Enabled State**: Shows `"Export X unique requests to selected format"` tooltip
  - **Disabled State**: Shows `"Export is disabled when no requests are available"` tooltip
  - **Automatic State Management**: Button automatically disabled when no data available
- **Status Display**: Shows export progress and results with full file paths
- **Thread-Safe Operations**: All export operations properly synchronized

#### Filter Panel: Advanced Request Filtering
- **HTTP Method Filter**: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, or All
- **Status Code Filter**: 2xx, 3xx, 4xx, 5xx ranges or specific codes (200, 302, 404, 500)
- **Host Filter**: Filter by hostname/domain (supports regex)
- **Path Filter**: Filter by request path substring (supports regex)
- **Advanced Options**:
  - **Show only items with responses**: Hide requests without captured responses
  - **Case sensitive**: Toggle case-sensitive text matching
  - **Regex**: Enable regular expression matching for text filters
- **Filter Controls**: Clear all filters or hide the panel

#### Middle Panel: Request List Table
- **HTTP History-style table** showing all unique requests
- **Columns**: Method, Path, Status Code, Timestamp
- **Features**: 
  - Color-coded status codes (green for 2xx, red for 4xx/5xx)
  - Sortable columns and resizable layout
  - **Multi-select support**: Ctrl/Cmd + click for multiple selection, Shift + click for range selection
  - Single-click selection to view request/response details
  - **Context menu integration**: Right-click for Burp tool integration and copy operations

#### Bottom Panel: Enhanced Request/Response Viewers
- **Split-pane layout** with native Burp editors (read-only for security)
- **Target Host Display**: Shows target host information above response viewer
  - **Smart Protocol Detection**: Automatically shows http/https
  - **Intelligent Port Display**: Shows port only when non-standard (not 80/443)
  - **Dynamic Updates**: Updates with table selection changes
- **Request Editor**: Full HTTP request with Pretty/Raw/Hex views (read-only)
- **Response Editor**: Complete HTTP response with all formatting options (read-only)
- **Synchronized selection**: Click table row to view corresponding request/response
- **Multi-select navigation**: Navigate through multiple selected requests with Prev/Next buttons
- **Selection indicator**: Shows current position when multiple requests are selected (e.g., "Showing 2 of 5 selected requests")

### Workflow Examples

#### Manual Testing
1. Enable filtering before starting your testing session
2. Browse the target application normally
3. Monitor the UniReq tab to see deduplication in action with smart export controls
4. Use target host display to quickly identify request destinations
5. Export unique patterns with full path feedback for documentation
6. Duplicate requests will be annotated with `X-UniReq-Status: DUPLICATE` header

#### Automated Scanning
1. Clear fingerprints before starting a new scan
2. Run your automated tools (scanner, crawler, etc.)
3. Review statistics to understand request patterns
4. Use smart export controls to document findings when data is available
5. Use the data to optimize your testing approach

#### Performance Monitoring
1. Watch real-time statistics during heavy traffic periods
2. Use duplicate counts to identify repetitive application behavior
3. Export data with intelligent state management for analysis
4. Clear fingerprints periodically to manage memory usage

### 🖱️ Context Menu Actions

Right-click on any request in the table to access a comprehensive context menu:

#### Send to Tools:
- **Repeater**: Send the selected request(s) to Burp's Repeater for manual testing
- **Intruder**: Send the selected request(s) to Burp's Intruder for automated attacks  
- **Comparer**: Send the selected request(s) to Burp's Comparer for diff analysis
- **Multi-select support**: All tools handle multiple selections by sending each request individually

#### Copy Options:
- **Copy URL**: Copy the request URL to clipboard
- **Copy as cURL**: Generate and copy a complete cURL command with headers and body

#### Request Management:
- **Highlight**: Mark the request for easy identification (coming soon)
- **Add Comment**: Add custom notes to the request (coming soon)
- **Delete Item**: Remove the request from the list (coming soon)

### 📊 Export & Reporting

Export your filtered request data to various formats with enhanced user experience:

#### Supported Export Formats:
- **CSV**: Comma-separated values for spreadsheet analysis
- **HTML**: Styled HTML report with color-coded status codes
- **Markdown**: GitHub-flavored markdown for documentation
- **JSON**: Structured data format for programmatic processing

#### Enhanced Export Features:
- **Smart State Management**: Export button automatically disabled when no data available
- **Context-Aware Tooltips**: Shows available request count or explains why export is disabled
- **Full Path Feedback**: Success messages display complete absolute file paths
  - Example: `"Exported 25 requests to /Users/harshit/Desktop/unireq_export.json"`
- **Thread-Safe Operations**: All export operations properly synchronized
- **Professional Status Updates**: Color-coded status messages for success, warning, and error states

#### Export Options:
- **Basic Export**: Method, URL, Status Code, Timestamp
- **Full Export**: Includes complete HTTP request and response data
- **Filtered Export**: Only exports currently visible (filtered) requests

#### Export Features:
- **UTF-8 Encoding**: Proper character encoding for international content
- **File Dialog**: Choose custom file names and locations
- **Error Handling**: Robust error reporting for file operations
- **Progress Feedback**: Visual confirmation of export completion
- **Smart Selection**: Exports selected requests if any are selected, otherwise exports all filtered requests
- **JSON Validation**: Built-in validation ensures exported JSON files are syntactically correct

## 🔧 Configuration

### Filtering Behavior
- **Default**: Filtering is enabled on startup
- **Annotation Mode**: Duplicates are marked but allowed through (current default)
- **Blocking Mode**: Duplicates can be dropped entirely (available in code)

### Memory Management
- Fingerprints are stored in memory only (not persisted)
- Use "Clear Fingerprints" to free memory when needed
- Extension automatically cleans up on unload

### Security Considerations
- **No Sensitive Data Logging**: Request content is hashed, not stored
- **In-Memory Only**: No data persisted to disk
- **Safe Logging**: Only non-sensitive request metadata is logged
- **Read-Only Editors**: Prevents accidental modifications to request/response data

## 🏗️ Architecture

### Enhanced Modular Package Structure
```
src/main/java/com/burp/unireq/
├── core/                         # Core business logic
│   ├── RequestDeduplicator       # Main deduplication engine
│   ├── FingerprintGenerator      # SHA-256 fingerprinting logic
│   └── FilterEngine              # Advanced filtering system
├── model/                        # Immutable data models
│   ├── RequestResponseEntry      # HTTP transaction container
│   ├── FilterCriteria           # Filter configuration
│   └── ExportConfiguration      # Export settings
├── export/                       # Multi-format export system
│   ├── ExportManager            # Export coordination
│   └── JsonExporter             # JSON export specialization
├── extension/                    # Burp Suite integration
│   ├── UniReqExtension          # Main extension entry point
│   └── RequestFingerprintListener # HTTP proxy interception
├── ui/                          # Enhanced user interface components
│   ├── UniReqGui                # Main GUI coordinator with export integration
│   └── components/              # Modular UI components
│       ├── StatsPanel           # Statistics display
│       ├── RequestTablePanel    # Request table with selection
│       ├── ViewerPanel          # Enhanced viewers with target host display
│       ├── ControlPanel         # Action buttons and status
│       └── ExportPanel          # Smart export controls with state management
└── utils/                       # Shared utilities
    ├── HttpUtils                # HTTP analysis utilities
    └── SwingUtils               # Enhanced GUI component utilities
```

### Why Enhanced Modular Design?

**Improved User Experience**: Smart state management and enhanced feedback provide a professional, intuitive interface.

**Scalability**: Each package has a focused responsibility, making it easy to extend functionality without affecting other components.

**Maintainability**: Clear separation of concerns reduces coupling and makes the codebase easier to understand and modify.

**Testability**: Modular design enables isolated unit testing of individual components.

**Reusability**: Utility classes and models can be reused across different parts of the extension.

**Team Development**: Multiple developers can work on different packages simultaneously without conflicts.

### Key Enhanced Components

#### Core Package (`core/`)
- **`RequestDeduplicator`**: Thread-safe deduplication engine with ConcurrentSkipListSet storage
- **`FingerprintGenerator`**: SHA-256 fingerprinting with path normalization and content analysis
- **`FilterEngine`**: Comprehensive filtering with regex support and multiple criteria

#### Model Package (`model/`)
- **`RequestResponseEntry`**: Immutable HTTP transaction data with security sanitization
- **`FilterCriteria`**: Type-safe filter configuration with validation
- **`ExportConfiguration`**: Export settings with format-specific options

#### Export Package (`export/`)
- **`ExportManager`**: Coordinates JSON, CSV, HTML, and Markdown export formats
- **`JsonExporter`**: Specialized JSON export with metadata and proper escaping

#### Extension Package (`extension/`)
- **`UniReqExtension`**: Main Burp extension implementing modern Montoya API
- **`RequestFingerprintListener`**: HTTP proxy integration with request/response interception

#### Enhanced UI Package (`ui/`)
- **`UniReqGui`**: Enhanced Swing-based interface with export integration and smart state management
- **`components/ExportPanel`**: Smart export controls with intelligent state management
- **`components/ViewerPanel`**: Enhanced viewers with target host display and read-only editors
- **`components/StatsPanel`**: Real-time statistics with color-coded indicators
- **`components/RequestTablePanel`**: HTTP request table with selection handling
- **`components/ControlPanel`**: Action buttons and status display

#### Utils Package (`utils/`)
- **`HttpUtils`**: Content type detection, status analysis, URL parsing, security sanitization
- **`SwingUtils`**: Enhanced GUI component creation with improved status types and styling utilities

#### Enhanced Model Classes

##### `RequestResponseEntry`
- Immutable data container for HTTP request/response pairs
- Provides safe content previews with sensitive data sanitization
- Supports JSON serialization for export functionality
- Thread-safe through immutable design

##### `FilterCriteria`
- Configuration object for advanced filtering
- Encapsulates all filter settings with validation
- Supports regex and case-sensitive options
- Type-safe filter configuration

##### `ExportConfiguration`
- Settings for multi-format data export
- Validates export parameters and provides format-specific defaults
- Handles UTF-8 encoding and file operations
- Supports CSV, HTML, Markdown, and JSON formats

## 🔍 Technical Details

### Thread Safety
- `ConcurrentSkipListSet` for fingerprint storage
- `AtomicBoolean` and `AtomicLong` for state management
- Thread-safe operations throughout the codebase
- Immutable model objects eliminate concurrency issues
- Synchronized UI updates using `SwingUtilities.invokeLater()`

### Performance Optimization
- Efficient SHA-256 hashing with minimal memory overhead
- Normalized path computation for consistent fingerprinting
- Auto-refresh timer with configurable intervals
- Type-safe model operations prevent runtime errors
- Smart UI state updates tied to actual data availability

### Error Handling
- Graceful degradation on fingerprint computation errors
- Comprehensive exception handling with logging
- Fallback mechanisms to prevent extension crashes
- Model validation prevents invalid data states
- Enhanced user feedback for export operations

### Enhanced Architecture Benefits
- **Maintainability**: Clear separation of concerns with dedicated model classes
- **Type Safety**: Strong typing prevents runtime errors
- **Testability**: Models can be unit tested independently
- **Extensibility**: Model-based design facilitates future enhancements
- **User Experience**: Smart state management and enhanced feedback improve usability
- **Professional Polish**: Consistent spacing, tooltips, and status messages

## 🐛 Troubleshooting

### Common Issues

#### Extension Not Loading
- Verify Java version (11+ required)
- Check Burp Suite version compatibility
- Review extension error logs in Burp's output

#### No Statistics Updates
- Ensure proxy is active and intercepting traffic
- Check if filtering is enabled in the UniReq tab
- Try manually refreshing statistics

#### Export Button Disabled
- Check if any requests are available in the table
- Export button automatically disables when no data is present
- Hover over the button to see tooltip explaining the state

#### High Memory Usage
- Clear fingerprints periodically during long sessions
- Monitor stored fingerprint count
- Consider restarting the extension for memory cleanup

### Debug Information
- Extension logs are available in Burp's **Extensions** → **Output**
- Error details are logged to Burp's **Extensions** → **Errors**
- Enable detailed logging for troubleshooting
- Export status messages provide detailed feedback for operations

## 📊 Performance Impact

### Memory Usage
- Approximately 50-100 bytes per unique fingerprint
- ~1-10KB per stored `RequestResponseEntry` (with truncation)
- Typical usage: 1-10MB for normal testing sessions
- Scales linearly with unique request diversity
- Immutable models reduce memory fragmentation

### Processing Overhead
- Minimal impact on request processing speed
- SHA-256 computation is highly optimized
- Concurrent operations prevent blocking
- Model object creation has minimal overhead
- Smart UI updates have sub-millisecond response times

## 🤝 Contributing

### Development Setup
1. Clone the repository
2. Import as Maven project in your IDE
3. Ensure Java 11+ and Maven are configured
4. Build with `mvn clean compile`

### Code Style
- Comprehensive Javadoc comments for all public methods
- Inline comments explaining complex logic
- Consistent naming conventions and formatting
- Proper separation of concerns with model classes
- Enhanced UX considerations in UI components

### Testing
- Manual testing with various request types
- Performance testing with high-volume traffic
- Error condition testing and recovery
- Model validation and thread safety testing
- UI state management and export functionality testing

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍💻 Author

**Harshit Shah** - Creator and maintainer of UniReq

## 🙏 Acknowledgments

- Built using Burp Suite's Montoya API
- Inspired by the need for efficient duplicate request filtering
- Thanks to the security testing community for feedback and requirements

---

**UniReq v1.0.0** by **Harshit Shah** - Making HTTP request analysis more efficient with enhanced user experience, one fingerprint at a time.

MIT License

Copyright (c) 2025 Harshit Shah 