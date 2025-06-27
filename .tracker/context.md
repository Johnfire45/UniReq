# UniReq - Project Context and Purpose

## Project Goal

UniReq is a Burp Suite extension designed to **eliminate noise from duplicate HTTP requests** during security testing and web application analysis. The extension provides an intelligent filtering system that identifies and manages duplicate requests while offering a comprehensive HTTP History-style interface for inspecting unique traffic.

### Core Objectives

1. **Noise Reduction**: Filter out repetitive HTTP requests that don't add value to security analysis
2. **Efficiency Improvement**: Help security testers focus on unique request patterns and behaviors
3. **Advanced Filtering**: Provide comprehensive filtering options with Reset button for quick clearing
4. **Traffic Analysis**: Provide clear visibility into request diversity and duplication patterns with synchronized statistics
5. **Memory Management**: Maintain performance with intelligent storage limits and cleanup
6. **User Experience**: Offer a familiar HTTP History-style interface with export bug fixes and enhanced polish
7. **Code Quality**: Maintain clean, modular architecture with proper separation of concerns and callback mechanisms
8. **Production Readiness**: Deliver a polished, professional tool ready for BApp Store submission

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
│   ├── UniReqGui.java               # Main GUI coordinator
│   └── components/                  # Modular UI components
│       ├── StatsPanel.java          # Statistics display
│       ├── RequestTablePanel.java   # Request table with selection
│       ├── ViewerPanel.java         # Request/Response viewers
│       ├── ControlPanel.java        # Action buttons and status
│       └── ExportPanel.java         # Export functionality with smart state management
└── utils/                           # Shared utilities
    ├── HttpUtils.java               # HTTP analysis utilities
    └── SwingUtils.java              # GUI component utilities
```

### Design Principles

1. **Separation of Concerns**: Each class has a single, well-defined responsibility
2. **Model-View Separation**: Data models are separate from GUI components
3. **Modular UI Architecture**: UI split into reusable, independent components
4. **Thread Safety**: Concurrent data structures and proper synchronization
5. **Memory Efficiency**: FIFO eviction and content truncation
6. **Event-Driven Communication**: Components communicate via listeners and coordinator pattern
7. **Extensibility**: Modular design allows for future enhancements
8. **Maintainability**: Clean code with comprehensive documentation

### Enhanced UI Architecture

The user interface follows a **component-based architecture** with clear separation of responsibilities and **enhanced polish for production use**:

**Layout Structure**:
```
UniReqGui (BorderLayout Coordinator)
├── NORTH: Title + StatsPanel (enhanced statistics with visible count)
├── CENTER: Main JSplitPane (VERTICAL)
│   ├── TOP: RequestTablePanel (responsive table with fixed columns)
│   └── BOTTOM: ViewerPanel (Horizontal JSplitPane)
│       ├── LEFT: Request Editor (Burp native, read-only)
│       ├── RIGHT: Response Editor (Burp native, read-only)
│       └── TARGET: Host label (above response viewer)
└── SOUTH: Combined Panel (BorderLayout)
    ├── WEST: ControlPanel (compact buttons + status)
    └── EAST: ExportPanel (format + scope selection with smart controls)
```

**Component Communication**:
- **Control Actions**: `ControlPanel` → `UniReqGui` → `RequestDeduplicator`
- **Export Actions**: `ExportPanel` (with scope selection) → `UniReqGui` → `ExportManager`
- **Statistics Updates**: `RequestDeduplicator` → `UniReqGui` → `StatsPanel` (with synchronized visible count via callback)
- **Table Updates**: `RequestDeduplicator` → `UniReqGui` → `RequestTablePanel` (with sort persistence)
- **Selection Changes**: `RequestTablePanel` → `UniReqGui` → {`ViewerPanel`, `ExportPanel` (scope state)}
- **Filter Reset**: `FilterPanel` (Reset button) → `clearFilters()` → automatic table refresh
- **Visible Count Sync**: `RequestTablePanel` → callback → `UniReqGui` → `StatsPanel` (synchronized count)
- **Resize Events**: `RequestTablePanel` → Dynamic column width adjustment

**Enhanced Benefits**:
- **Fixed Export Mapping**: Export operations now correctly map filtered/sorted data eliminating wrong exports
- **Synchronized Statistics**: Visible count display perfectly matches actual filtered data used for export
- **Reset Filters**: One-click filter clearing with automatic table refresh and statistics update
- **Responsive Design**: Table layout adapts to viewport size eliminating whitespace
- **Smart State Management**: Export controls adapt to data availability and selection state
- **Enhanced Feedback**: Context-aware tooltips and status messages
- **Sort Persistence**: User sorting preferences maintained during data operations
- **Compact Layout**: Optimized spacing for better screen real estate utilization
- **Professional Polish**: Consistent styling and behavior throughout the interface

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
- `ExportManager`: Coordinates export operations and delegates to specialized exporters
- `JsonExporter`: JSON export with RFC 8259 compliant escaping for all control characters
- `CsvExporter`: CSV export with RFC 4180 compliance and injection prevention
- `MarkdownExporter`: GitHub-flavored Markdown export with professional table formatting

**Extension Integration** (`extension/`):
- `UniReqExtension`: Main extension entry point with Montoya API integration
- `RequestFingerprintListener`: HTTP traffic interception and processing

**User Interface** (`ui/`):
- `UniReqGui`: Main coordinator managing layout and inter-component communication
- `components/StatsPanel`: Color-coded statistics display with thread-safe updates
- `components/RequestTablePanel`: HTTP request table with selection handling and refresh
- `components/ViewerPanel`: Request/Response viewers using Burp's native editors with target host display
- `components/ControlPanel`: Action buttons and status display with event handling
- `components/ExportPanel`: Export functionality with intelligent state management and enhanced UX

**Utilities** (`utils/`):
- `HttpUtils`: HTTP analysis, content type detection, security sanitization
- `SwingUtils`: Consistent GUI component creation and styling utilities

## Enhanced Features (Current Implementation)

### Recent UI Polish Enhancements (v1.0.0)

#### 🎯 **Responsive Table Layout**
1. **Fixed Column Widths**: Consistent sizing for key columns eliminates layout jumping
   - **Req#**: 40px (fixed) - Sequence number display
   - **Method**: 60px (fixed) - HTTP method (GET, POST, etc.)
   - **Status**: 60px (fixed) - HTTP status code
   - **Host**: 150px (flexible, min 100px) - Target hostname
   - **Path**: Dynamic width - Fills remaining viewport space
2. **Whitespace Elimination**: Path column expands to fill available space, removing right-side gaps
3. **Multi-Layer Resize Handling**: 
   - Table component resize listener for direct table resizing
   - Viewport resize listener for scroll pane changes
   - Initial sizing timer for proper component initialization
4. **Thread-Safe Updates**: All resize calculations use `SwingUtilities.invokeLater()`

#### 📊 **Enhanced Statistics Display**
1. **Comprehensive Metrics**: Extended statistics format showing:
   - **Total**: All requests processed by the extension
   - **Unique**: Requests with distinct fingerprints
   - **Duplicates**: Blocked duplicate requests
   - **Visible**: "X of Y" format showing filtered vs total unique requests
2. **Real-Time Updates**: Statistics refresh with every data change and filter application
3. **Filter Awareness**: Visible count accurately reflects current filter state
4. **Color-Coded Display**: Green for unique, red for duplicates, black for totals and visible

#### 🎛️ **Smart Export System**
1. **Export Scope Dropdown**: User choice between export modes:
   - **"All Visible Requests"**: Exports currently filtered/visible requests (default)
   - **"Only Selected Requests"**: Exports only selected table rows
2. **Intelligent State Management**:
   - Scope dropdown automatically adapts to selection availability
   - Export button disabled when no requests available
   - Context-aware tooltips show current state and request counts
3. **Enhanced User Feedback**:
   - Success messages display complete absolute file paths
   - Scope-specific status messages for clarity
   - Thread-safe state updates prevent UI inconsistencies

#### 🔄 **Sort State Persistence**
1. **Persistent Sorting**: User's sort preferences maintained during:
   - Data refreshes from the deduplicator
   - Filter changes and applications
   - Table content updates
2. **Smart Comparators**: 
   - **Numeric Columns (Req#, Status)**: Proper numeric ordering (1, 2, 10 vs "1", "10", "2")
   - **Text Columns (Method, Host, Path)**: Case-insensitive alphabetical sorting
3. **View-to-Model Conversion**: Proper index mapping preserves selections during sorting operations
4. **Single-Column Focus**: Only one column can be sorted at a time for clarity

#### 🎨 **Compact UI Design**
1. **Optimized Spacing**: Reduced padding throughout the interface:
   - Title panel: `(5,10,3,10)` → `(3,10,2,10)`
   - Stats/export panel: vertical gap `2` → `1`, border `(2,10,2,10)` → `(1,10,1,10)`
2. **Space Utilization**: Better use of available screen real estate
3. **Visual Consistency**: Maintained readability while optimizing space usage
4. **Component Alignment**: Professional layout with proper component positioning

### Advanced User Interaction
1. **Burp Tool Integration**: Seamless integration with Repeater, Intruder, and Comparer through context menu
2. **Context Menu Actions**: Right-click functionality for quick access to common operations
3. **Clipboard Operations**: Copy URLs and generate cURL commands with proper header handling
4. **Workflow Acceleration**: Reduce manual steps in common testing workflows
5. **Multi-Select Support**: Batch operations on multiple selected requests

### Advanced Export System
1. **Modular Export Architecture**: Specialized exporters for each format with consistent interfaces
2. **Multi-format Support**: CSV, HTML, Markdown, and JSON export formats with format-specific optimizations
3. **Security-First Design**: 
   - **CSV**: Formula injection prevention, RFC 4180 compliant escaping
   - **JSON**: All control characters (0x00-0x1F) properly escaped as unicode sequences
   - **Markdown**: Special character escaping, table-optimized content truncation
4. **Professional Output**: 
   - **CSV**: Proper quoting and escaping for Excel compatibility
   - **JSON**: RFC 8259 compliant with proper control character handling
   - **Markdown**: GitHub-flavored tables with export summaries and metadata
   - **HTML**: Styled reports with color-coded status codes (embedded format)
5. **Configurable Options**: Metadata inclusion, full request/response data, format-specific settings
6. **UTF-8 Encoding**: Full international character support across all formats
7. **Size Estimation**: Accurate export size prediction for progress indication

### Enhanced Productivity Features
1. **Advanced Copy Operations**: Generate ready-to-use cURL commands with headers and body content
2. **Export Filtering**: Export only currently visible (filtered) requests for targeted analysis
3. **File Dialog Integration**: User-friendly file selection and naming for exports
4. **Error Handling**: Robust error reporting for file operations and tool integration
5. **Smart Selection Export**: Automatically exports selected requests or all filtered requests

### Column Sorting Capabilities
1. **Full Column Sorting**: Click any column header to sort requests by that column
2. **Smart Comparators**: 
   - **Req# and Status**: Numeric sorting (1, 2, 10 vs "1", "10", "2")
   - **Method, Host, Path**: Case-insensitive alphabetical sorting
3. **Single-Column Focus**: Only one column can be sorted at a time for clarity
4. **Visual Indicators**: Sort arrows automatically appear in column headers
5. **Sort State Persistence**: Sorting is maintained during filter changes and table refreshes
6. **Selection Preservation**: User selections are preserved when sorting is applied
7. **Filter Integration**: Sorting works seamlessly with filtered results

### UI Polish and User Experience
1. **Smart Export Controls**: Export button automatically disabled when no data available
2. **Enhanced Tooltips**: Context-aware tooltips showing request counts and availability status
3. **Full Path Feedback**: Success messages display complete absolute file paths
4. **Target Host Display**: Shows target host information above response viewer
5. **Read-Only Editors**: Prevents accidental modifications while maintaining full functionality
6. **Consistent Spacing**: Professional layout with proper component alignment
7. **Thread-Safe Updates**: All UI changes properly synchronized for smooth operation

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

## Enhanced User Experience Features

#### Smart State Management
1. **Export Button Intelligence**: Export button automatically disabled when no requests are available
2. **Context-Aware Tooltips**: Dynamic tooltips showing:
   - Available request count when enabled
   - Disabled reason when no data available
   - Current export scope and selection state
3. **Full Path Feedback**: Success messages display complete absolute file paths for exported files
4. **Thread-Safe Operations**: All UI updates properly synchronized for smooth user experience

#### Advanced UI Polish
1. **Consistent Spacing**: Professional layout with optimized padding throughout
2. **Responsive Components**: UI elements adapt to data state changes automatically
3. **Status Integration**: Export panel status updates tied to data availability and selection state
4. **Visual Hierarchy**: Clear component organization with proper alignment and spacing

#### Target Host Display
1. **Smart Host Labels**: ViewerPanel shows target host information above response viewer
2. **Protocol Detection**: Automatically detects and displays http/https protocols
3. **Port Logic**: Shows port numbers only when non-standard (not 80 for http, not 443 for https)
4. **Dynamic Updates**: Host information updates with table selection changes

#### Read-Only Editors
1. **Security Focus**: Request and response editors are read-only to prevent accidental modifications
2. **Native Integration**: Uses Burp's native editors with `EditorOptions.READ_ONLY` flag
3. **Consistent Experience**: Maintains familiar Burp Suite editor interface and functionality

## Use Cases and Scenarios

### Manual Security Testing

**Scenario**: Penetration tester exploring a web application
- **Problem**: Repetitive AJAX calls and background requests create noise
- **Solution**: UniReq filters duplicates, showing only unique request patterns
- **Benefit**: Tester focuses on new functionality and potential attack vectors

**Enhanced Workflow**:
1. Enable filtering before starting testing session
2. Browse application normally through Burp proxy
3. Monitor UniReq tab to see unique request patterns with responsive table layout
4. Use enhanced statistics to understand request diversity ("Visible: X of Y")
5. Click on requests to inspect details in native Burp editors with target host information
6. Export unique patterns with scope selection and full path feedback for documentation
7. Clear data between testing different application areas

### Automated Scanning

**Scenario**: Running automated security scanners through Burp
- **Problem**: Scanners generate thousands of similar requests
- **Solution**: UniReq identifies truly unique request structures
- **Benefit**: Faster analysis of scanner results and reduced storage

**Enhanced Workflow**:
1. Clear existing data before starting scan
2. Run automated tools with UniReq filtering enabled
3. Review unique request patterns discovered by scanner with sort persistence
4. Use enhanced statistics to understand application request diversity
5. Export unique requests with intelligent scope selection for manual follow-up

### API Testing

**Scenario**: Testing REST APIs with repetitive endpoint calls
- **Problem**: Similar API calls with different parameters create clutter
- **Solution**: UniReq groups similar requests by structure
- **Benefit**: Clear view of API endpoint coverage and unique patterns

**Enhanced Workflow**:
1. Configure API testing tools to use Burp proxy
2. Monitor UniReq for new API endpoints and methods with responsive table display
3. Analyze request/response patterns for each unique endpoint
4. Use sort functionality to organize requests by method, host, or status
5. Export API coverage documentation with scope selection and full file paths

### Traffic Analysis

**Scenario**: Analyzing web application behavior and patterns
- **Problem**: Need to understand application request diversity
- **Solution**: UniReq provides enhanced statistics and unique request catalog
- **Benefit**: Insights into application architecture and user flows

**Enhanced Workflow**:
1. Enable passive monitoring during normal application use
2. Review enhanced statistics to understand request patterns and filtering effectiveness
3. Analyze unique requests to map application functionality with target information
4. Use responsive table layout for efficient data review
5. Export analysis data with scope selection for reporting or further analysis

## Conclusion

UniReq addresses a fundamental challenge in web application security testing: **managing the overwhelming volume of HTTP traffic to focus on what matters**. By providing intelligent deduplication with a familiar interface enhanced by modern UI polish, it significantly improves the efficiency and effectiveness of security testing workflows while maintaining the flexibility and power that Burp Suite users expect.

The extension serves as both a practical tool for immediate use and a foundation for more advanced traffic analysis and security testing automation. Its clean, modular design prioritizes reliability, performance, and user experience while providing the extensibility needed for future enhancements and integrations.

### Production Achievement (v1.0.0 - BApp Store Ready)

The **comprehensive development and polish cycle** has transformed UniReq into a production-ready tool with:

- **Professional Filter UI**: Replaced cryptic icons with labeled checkboxes `[☐ Regex] [☐ Case] [☐ Invert]`
- **Advanced Filtering System**: Modal dialog with MIME type parsing, Burp scope integration, and extension filters
- **Visual Polish**: Forest green/amber active states, GridBagLayout, optimized spacing, and logical grouping
- **JAR Optimization**: Reduced from 1.6MB to 121KB (93% reduction) suitable for BApp Store submission
- **Fixed Export Mapping**: Export operations correctly map filtered/sorted data eliminating wrong exports
- **Synchronized Statistics**: "Visible: X of Y" format provides clear filtering context with accurate counts
- **Enhanced User Experience**: Professional layout, visual feedback, and smart state management
- **Robust Architecture**: 6,635+ lines of well-documented, modular code with comprehensive error handling
- **Memory Efficiency**: Intelligent storage management with FIFO eviction and content truncation
- **Security Focus**: Proper input validation, safe export formats, and sensitive data protection

These comprehensive improvements, combined with the solid architectural foundation, make UniReq a **mature and professional tool** ready for production deployment in security testing environments. The enhanced user experience features ensure smooth workflow integration while the responsive design adapts to different usage patterns and screen sizes.

**Production Status**: ✅ Ready for BApp Store submission with comprehensive feature set, polished UI, and robust architecture  
**Current Version**: 1.0.0 (BApp Store Ready - Professional UI Polish Complete)  
**JAR Size**: 121KB (optimized from 1.6MB - 93% reduction)  
**Total Codebase**: 6,635+ lines across 21 Java files  
**Last Updated**: January 2024  
**Quality Assurance**: All major bugs fixed, UI polish complete, advanced filtering implemented, thread-safe operations verified 