# UniReq - Project Context and Purpose

## Project Goal

UniReq is a Burp Suite extension designed to **eliminate noise from duplicate HTTP requests** during security testing and web application analysis. The extension provides an intelligent filtering system that identifies and manages duplicate requests while offering a comprehensive HTTP History-style interface for inspecting unique traffic.

### Core Objectives

1. **Noise Reduction**: Filter out repetitive HTTP requests that don't add value to security analysis
2. **Efficiency Improvement**: Help security testers focus on unique request patterns and behaviors
3. **Traffic Analysis**: Provide clear visibility into request diversity and duplication patterns
4. **Memory Management**: Maintain performance with intelligent storage limits and cleanup
5. **User Experience**: Offer a familiar HTTP History-style interface integrated with Burp Suite

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

## Future Enhancements

### Planned Features
- **Configurable Algorithms**: Support for different fingerprinting methods
- **Export Functionality**: Save unique requests for external analysis
- **Advanced Filtering**: Custom rules based on headers, content, or patterns
- **Integration APIs**: Programmatic access for other tools and scripts

### Potential Integrations
- **Scanner Integration**: Feed unique requests to active scanners
- **Reporting Tools**: Export data to security reporting platforms
- **CI/CD Pipelines**: Automated request pattern validation
- **Threat Intelligence**: Compare patterns against known attack signatures

## Conclusion

UniReq addresses a fundamental challenge in web application security testing: **managing the overwhelming volume of HTTP traffic to focus on what matters**. By providing intelligent deduplication with a familiar interface, it enhances the efficiency and effectiveness of security testing workflows while maintaining the flexibility and power that Burp Suite users expect.

The extension serves as both a practical tool for immediate use and a foundation for more advanced traffic analysis and security testing automation. Its design prioritizes reliability, performance, and user experience while providing the extensibility needed for future enhancements and integrations. 