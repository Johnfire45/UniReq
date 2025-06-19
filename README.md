# UniReq - HTTP Request Deduplicator for Burp Suite

**Author: Harshit Shah**

A Burp Suite extension that filters duplicate HTTP requests by computing unique fingerprints based on request structure and content. Built using Java and the Montoya API.

## 🎯 Purpose

UniReq helps security testers and developers by:
- **Reducing noise** in HTTP history by filtering duplicate requests
- **Improving efficiency** during manual testing and automated scanning
- **Providing visibility** into request patterns and duplication statistics
- **Maintaining performance** through intelligent fingerprinting algorithms

## ✨ Features

### Core Functionality
- **Smart Fingerprinting**: Computes unique fingerprints using `METHOD | NORMALIZED_PATH | HASH(CONTENT)`
- **Thread-Safe Operation**: Uses concurrent collections for safe multi-threaded operation
- **Flexible Filtering**: Toggle filtering on/off without losing stored fingerprints
- **Real-Time Statistics**: Live updates of request counts and duplication metrics

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

The UniReq extension provides a comprehensive HTTP History-style interface with three main sections:

#### Top Panel: Controls and Statistics
- **Controls Section**:
  - **Disable/Enable Filtering**: Toggle request filtering on/off
  - **Clear All Data**: Reset all stored requests and statistics (with confirmation)
  - **Refresh**: Manually update the display
  
- **Statistics Section** (real-time, color-coded):
  - **Unique**: Number of unique requests captured (green)
  - **Duplicates**: Number of duplicate requests filtered (red)
  - **Stored**: Total requests stored in memory (blue)
  - **Filtering**: Current status - ENABLED/DISABLED (green/red)

#### Middle Panel: Request List Table
- **HTTP History-style table** showing all unique requests
- **Columns**: Method, Path, Status Code, Timestamp
- **Features**: 
  - Color-coded status codes (green for 2xx, red for 4xx/5xx)
  - Sortable columns and resizable layout
  - Single-click selection to view request/response details

#### Bottom Panel: Request/Response Viewers
- **Split-pane layout** with native Burp editors
- **Request Editor**: Full HTTP request with Pretty/Raw/Hex views
- **Response Editor**: Complete HTTP response with all formatting options
- **Synchronized selection**: Click table row to view corresponding request/response

### Workflow Examples

#### Manual Testing
1. Enable filtering before starting your testing session
2. Browse the target application normally
3. Monitor the UniReq tab to see deduplication in action
4. Duplicate requests will be annotated with `X-UniReq-Status: DUPLICATE` header

#### Automated Scanning
1. Clear fingerprints before starting a new scan
2. Run your automated tools (scanner, crawler, etc.)
3. Review statistics to understand request patterns
4. Use the data to optimize your testing approach

#### Performance Monitoring
1. Watch real-time statistics during heavy traffic periods
2. Use duplicate counts to identify repetitive application behavior
3. Clear fingerprints periodically to manage memory usage

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

## 🏗️ Architecture

### Component Overview
```
UniReqExtension (Main Entry Point)
├── RequestDeduplicator (Core Logic)
│   ├── Fingerprint Computation
│   ├── Duplicate Detection
│   └── Statistics Tracking
├── RequestFingerprintListener (Proxy Handler)
│   ├── Request Interception
│   ├── Deduplication Processing
│   └── GUI Updates
└── UniReqGui (User Interface)
    ├── Control Panel
    ├── Statistics Display
    └── Status Monitoring
```

### Key Classes

#### `UniReqExtension`
- Main extension entry point implementing `BurpExtension`
- Handles initialization and component registration
- Manages extension lifecycle and cleanup

#### `RequestDeduplicator`
- Core deduplication engine with thread-safe operations
- Computes SHA-256 fingerprints for request identification
- Maintains statistics and filtering state

#### `RequestFingerprintListener`
- Implements `HttpRequestHandler` for proxy integration
- Intercepts requests and applies deduplication logic
- Updates GUI statistics in real-time

#### `UniReqGui`
- Swing-based user interface implementing `SuiteTab`
- Provides controls and real-time statistics display
- Handles user interactions and feedback

## 🔍 Technical Details

### Thread Safety
- `ConcurrentSkipListSet` for fingerprint storage
- `AtomicBoolean` and `AtomicLong` for state management
- Thread-safe operations throughout the codebase

### Performance Optimization
- Efficient SHA-256 hashing with minimal memory overhead
- Normalized path computation for consistent fingerprinting
- Auto-refresh timer with configurable intervals

### Error Handling
- Graceful degradation on fingerprint computation errors
- Comprehensive exception handling with logging
- Fallback mechanisms to prevent extension crashes

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

#### High Memory Usage
- Clear fingerprints periodically during long sessions
- Monitor stored fingerprint count
- Consider restarting the extension for memory cleanup

### Debug Information
- Extension logs are available in Burp's **Extensions** → **Output**
- Error details are logged to Burp's **Extensions** → **Errors**
- Enable detailed logging for troubleshooting

## 📊 Performance Impact

### Memory Usage
- Approximately 50-100 bytes per unique fingerprint
- Typical usage: 1-10MB for normal testing sessions
- Scales linearly with unique request diversity

### Processing Overhead
- Minimal impact on request processing speed
- SHA-256 computation is highly optimized
- Concurrent operations prevent blocking

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

### Testing
- Manual testing with various request types
- Performance testing with high-volume traffic
- Error condition testing and recovery

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍💻 Author

**Harshit Shah** - Creator and maintainer of UniReq

## 🙏 Acknowledgments

- Built using Burp Suite's Montoya API
- Inspired by the need for efficient duplicate request filtering
- Thanks to the security testing community for feedback and requirements

---

**UniReq v1.0.0** by **Harshit Shah** - Making HTTP request analysis more efficient, one fingerprint at a time.

MIT License

Copyright (c) 2025 Harshit Shah 