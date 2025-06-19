# UniReq - Project Context and Purpose

## Project Goal

UniReq is a Burp Suite extension designed to **eliminate noise from duplicate HTTP requests** during security testing and web application analysis. The extension provides an intelligent filtering system that identifies and manages duplicate requests while offering a comprehensive HTTP History-style interface for inspecting unique traffic.

### Core Objectives

1. **Noise Reduction**: Filter out repetitive HTTP requests that don't add value to security analysis
2. **Efficiency Improvement**: Help security testers focus on unique request patterns and behaviors
3. **Advanced Filtering**: Provide comprehensive filtering options similar to Burp's HTTP History
4. **Traffic Analysis**: Provide clear visibility into request diversity and duplication patterns
5. **Memory Management**: Maintain performance with intelligent storage limits and cleanup
6. **User Experience**: Offer a familiar HTTP History-style interface integrated with Burp Suite
7. **Code Quality**: Maintain clean, modular architecture with proper separation of concerns

## Intended Behavior

### Request Deduplication Logic

**Fingerprint Algorithm**: `METHOD | NORMALIZED_PATH | HASH(CONTENT)`

- **METHOD**: HTTP method (GET, POST, PUT, DELETE, etc.)
- **NORMALIZED_PATH**: URL path converted to lowercase with trailing slashes removed
- **HASH**: SHA-256 hash of relevant content:
  - For requests with bodies (POST, PUT, PATCH): Hash of request body
  - For GET requests: Hash of query string parameters
  - For requests with no relevant content: "EMPTY" marker

**Examples of Fingerprints**:
```
GET | /api/users | EMPTY
POST | /api/login | a1b2c3d4e5f6789... (SHA-256 of body)
GET | /search | e5f6g7h8i9j0abc... (SHA-256 of query string)
```

### Filtering Behavior

**Default Mode: Annotation**
- Unique requests: Pass through normally
- Duplicate requests: Annotated with `X-UniReq-Status: DUPLICATE` header
- All requests remain visible in Burp's standard tools
- Provides visibility without blocking functionality

**Alternative Mode: Aggressive Blocking** (available in code)
- Unique requests: Pass through normally  
- Duplicate requests: Dropped entirely from proxy chain
- Reduces traffic volume but may hide important patterns

### Memory Management

**Storage Limits**:
- Maximum 1000 unique request/response pairs stored
- FIFO (First In, First Out) eviction when limit exceeded
- Content truncation at 10KB for UI previews
- Sensitive header sanitization (Authorization, Cookie, etc.)

**Security Measures**:
- No sensitive data persisted to disk
- Headers containing credentials are redacted in previews
- Request/response content is truncated for UI display
- All data cleared on extension unload

## Integration with Burp Suite

### Proxy Integration

**Request Interception**:
- Registers as `ProxyRequestHandler` to intercept all HTTP requests
- Processes requests before they're sent to target servers
- Computes fingerprints and checks for duplicates
- Annotates or blocks requests based on configuration

**Response Interception**:
- Registers as `ProxyResponseHandler` to capture HTTP responses
- Associates responses with their corresponding requests
- Completes request/response pairs for GUI display
- Updates real-time statistics and request list

### GUI Integration

**Suite Tab Registration**:
- Appears as "UniReq" tab in Burp's main interface
- Provides HTTP History-style table of unique requests
- Integrates Burp's native request/response editors
- Maintains consistent look and feel with Burp Suite

**Real-time Updates**:
- Auto-refresh every 2 seconds for live statistics
- Immediate updates when requests are processed
- Synchronized table selection and viewer updates
- Color-coded status indicators for quick assessment

### Extension Lifecycle

**Initialization**:
1. Extension loaded by Burp Suite
2. Montoya API initialized and components created
3. GUI tab registered and proxy handlers activated
4. Auto-refresh timer started for real-time updates
5. Extension ready to process HTTP traffic

**Runtime Operation**:
1. HTTP requests intercepted and fingerprinted
2. Duplicate detection and appropriate action taken
3. GUI updated with statistics and new unique requests
4. Responses captured and associated with requests
5. User interactions handled (filtering toggle, data clearing)

**Cleanup**:
1. Extension unload triggered by user or Burp shutdown
2. Auto-refresh timer stopped
3. All stored data cleared from memory
4. Resources released and handlers unregistered

## Architecture and Code Organization

### Package Structure

The extension follows a clean, modular architecture with proper separation of concerns:

```
com.burp.unireq/
├── core/                             # Core business logic
│   ├── RequestDeduplicator.java     # Main deduplication engine
│   ├── FingerprintGenerator.java    # SHA-256 fingerprinting logic
│   └── FilterEngine.java            # Advanced filtering system
├── model/                            # Immutable data models
│   ├── RequestResponseEntry.java    # HTTP request/response data model
│   ├── FilterCriteria.java          # Filter configuration model
│   └── ExportConfiguration.java     # Export settings model
├── export/                           # Multi-format export system
│   ├── ExportManager.java           # Export coordination
│   └── JsonExporter.java            # JSON export specialization
├── extension/                        # Burp Suite integration
│   ├── UniReqExtension.java         # Main extension entry point
│   └── RequestFingerprintListener.java # HTTP traffic interception
├── ui/                              # User interface components
│   └── UniReqGui.java               # Main GUI component
└── utils/                           # Shared utilities
    ├── HttpUtils.java               # HTTP analysis utilities
    └── SwingUtils.java              # GUI component utilities
```

### Design Principles

1. **Separation of Concerns**: Each class has a single, well-defined responsibility
2. **Model-View Separation**: Data models are separate from GUI components
3. **Thread Safety**: Concurrent data structures and proper synchronization
4. **Memory Efficiency**: FIFO eviction and content truncation
5. **Extensibility**: Modular design allows for future enhancements
6. **Maintainability**: Clean code with comprehensive documentation

### Key Components

**Core Engine** (`core/`):
- `RequestDeduplicator`: Thread-safe deduplication engine delegating to specialized components
- `FingerprintGenerator`: SHA-256 fingerprinting with path normalization and content analysis
- `FilterEngine`: Comprehensive filtering system with regex support and multiple criteria

**Data Models** (`model/`):
- `RequestResponseEntry`: Immutable data container for HTTP request/response pairs
- `FilterCriteria`: Configuration object for advanced filtering options
- `ExportConfiguration`: Settings for multi-format data export

**Export System** (`export/`):
- `ExportManager`: Coordinates multiple export formats (JSON, CSV, HTML, Markdown)
- `JsonExporter`: Specialized JSON export with metadata support and proper escaping

**Extension Integration** (`extension/`):
- `UniReqExtension`: Main extension entry point with Montoya API integration
- `RequestFingerprintListener`: HTTP traffic interception and processing

**User Interface** (`ui/`):
- `UniReqGui`: Comprehensive HTTP History-style interface with advanced features

**Utilities** (`utils/`):
- `HttpUtils`: HTTP analysis, content type detection, security sanitization
- `SwingUtils`: Consistent GUI component creation and styling utilities

## Use Cases and Scenarios

### Manual Security Testing

**Scenario**: Penetration tester exploring a web application
- **Problem**: Repetitive AJAX calls and background requests create noise
- **Solution**: UniReq filters duplicates, showing only unique request patterns
- **Benefit**: Tester focuses on new functionality and potential attack vectors

**Workflow**:
1. Enable filtering before starting testing session
2. Browse application normally through Burp proxy
3. Monitor UniReq tab to see unique request patterns
4. Click on requests to inspect details in native Burp editors
5. Clear data between testing different application areas

### Automated Scanning

**Scenario**: Running automated security scanners through Burp
- **Problem**: Scanners generate thousands of similar requests
- **Solution**: UniReq identifies truly unique request structures
- **Benefit**: Faster analysis of scanner results and reduced storage

**Workflow**:
1. Clear existing data before starting scan
2. Run automated tools with UniReq filtering enabled
3. Review unique request patterns discovered by scanner
4. Use statistics to understand application request diversity
5. Export or analyze unique requests for manual follow-up

### API Testing

**Scenario**: Testing REST APIs with repetitive endpoint calls
- **Problem**: Similar API calls with different parameters create clutter
- **Solution**: UniReq groups similar requests by structure
- **Benefit**: Clear view of API endpoint coverage and unique patterns

**Workflow**:
1. Configure API testing tools to use Burp proxy
2. Monitor UniReq for new API endpoints and methods
3. Analyze request/response patterns for each unique endpoint
4. Identify missing authentication, error handling, or edge cases
5. Use filtering to focus on specific API areas

### Traffic Analysis

**Scenario**: Analyzing web application behavior and patterns
- **Problem**: Need to understand application request diversity
- **Solution**: UniReq provides statistics and unique request catalog
- **Benefit**: Insights into application architecture and user flows

**Workflow**:
1. Enable passive monitoring during normal application use
2. Review statistics to understand request patterns
3. Analyze unique requests to map application functionality
4. Export data for reporting or further analysis
5. Use filtering controls to focus on specific time periods

## Benefits and Value Proposition

### For Security Testers
- **Reduced Noise**: Focus on unique requests, not repetitive traffic
- **Improved Efficiency**: Spend time on analysis, not data filtering
- **Better Coverage**: Ensure all unique request patterns are examined
- **Clear Visibility**: Understand application request diversity at a glance

### For Development Teams
- **Performance Insights**: Identify repetitive or unnecessary requests
- **API Documentation**: Catalog unique endpoints and request patterns
- **Testing Validation**: Ensure test coverage includes all unique scenarios
- **Architecture Understanding**: Visualize application request flows

### For Compliance and Auditing
- **Request Cataloging**: Document all unique request types processed
- **Pattern Analysis**: Demonstrate thorough coverage of application functionality
- **Evidence Collection**: Maintain records of unique security test scenarios
- **Reporting**: Generate statistics and summaries for audit reports

## Technical Advantages

### Performance
- **Minimal Overhead**: Efficient fingerprinting with minimal processing delay
- **Memory Bounded**: FIFO eviction prevents unlimited memory growth
- **Concurrent Processing**: Thread-safe design handles high-volume traffic
- **Real-time Updates**: Live statistics without impacting proxy performance

### Reliability
- **Error Handling**: Graceful degradation prevents extension crashes
- **Thread Safety**: Concurrent data structures eliminate race conditions
- **Resource Management**: Automatic cleanup and memory management
- **Fallback Mechanisms**: Continues operation even with fingerprint errors

### Integration
- **Native Burp Components**: Uses Burp's request/response editors
- **Consistent UI**: Follows Burp Suite design patterns and conventions
- **Standard APIs**: Built on official Montoya API for compatibility
- **Extension Ecosystem**: Complements other Burp extensions and tools

### Code Quality
- **Modular Architecture**: Clean separation of concerns with dedicated model classes
- **Type Safety**: Strong typing with dedicated data models
- **Maintainability**: Well-organized package structure and comprehensive documentation
- **Extensibility**: Model-based design facilitates future enhancements

## Enhanced Features (Current Implementation)

### Advanced User Interaction
1. **Burp Tool Integration**: Seamless integration with Repeater, Intruder, and Comparer through context menu
2. **Context Menu Actions**: Right-click functionality for quick access to common operations
3. **Clipboard Operations**: Copy URLs and generate cURL commands with proper header handling
4. **Workflow Acceleration**: Reduce manual steps in common testing workflows
5. **Multi-Select Support**: Batch operations on multiple selected requests

### Data Export and Reporting
1. **Multi-format Export**: Support for CSV, HTML, Markdown, and JSON export formats
2. **Professional Reports**: Styled HTML reports with color-coded status codes for stakeholder communication
3. **Configurable Data Inclusion**: Option to include basic metadata or full request/response content
4. **UTF-8 Encoding**: Proper character encoding for international content and special characters
5. **JSON Validation**: Built-in validation ensures exported JSON files are syntactically correct

### Enhanced Productivity Features
1. **Advanced Copy Operations**: Generate ready-to-use cURL commands with headers and body content
2. **Export Filtering**: Export only currently visible (filtered) requests for targeted analysis
3. **File Dialog Integration**: User-friendly file selection and naming for exports
4. **Error Handling**: Robust error reporting for file operations and tool integration
5. **Smart Selection Export**: Automatically exports selected requests or all filtered requests

### Future Enhancement Framework
1. **Request Highlighting**: Visual marking of important requests (architecture ready)
2. **Comment System**: Add custom notes and annotations to requests (architecture ready)
3. **Request Deletion**: Remove specific requests from the interface (architecture ready)
4. **Bulk Operations**: Multi-select operations for efficient data management (partially implemented)

## Future Enhancements

### Planned Features
- **Configurable Algorithms**: Support for different fingerprinting methods
- **Advanced Filtering**: Custom rules based on headers, content, or patterns
- **Integration APIs**: Programmatic access for other tools and scripts
- **Request Management**: Complete implementation of highlighting, commenting, and deletion features

### Potential Integrations
- **Scanner Integration**: Feed unique requests to active scanners
- **Reporting Tools**: Export data to security reporting platforms
- **CI/CD Pipelines**: Automated request pattern validation
- **Threat Intelligence**: Compare patterns against known attack signatures

## Conclusion

UniReq addresses a fundamental challenge in web application security testing: **managing the overwhelming volume of HTTP traffic to focus on what matters**. By providing intelligent deduplication with a familiar interface, it enhances the efficiency and effectiveness of security testing workflows while maintaining the flexibility and power that Burp Suite users expect.

The extension serves as both a practical tool for immediate use and a foundation for more advanced traffic analysis and security testing automation. Its clean, modular design prioritizes reliability, performance, and user experience while providing the extensibility needed for future enhancements and integrations.

The recent refactoring has established a solid architectural foundation with proper separation of concerns, making the codebase more maintainable, testable, and ready for future feature additions. The model-based approach ensures type safety and provides a clear structure for data management throughout the application. 