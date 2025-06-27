# UniReq – HTTP Request Deduplicator for Burp Suite

**UniReq** is a Burp Suite extension that helps security professionals efficiently deduplicate and analyze HTTP traffic. It provides high-performance filtering, smart grouping, and one-click exports — ideal for bug bounty, pentests, and large-scale recon.

---

## 🚀 Features

- ✅ **Deduplication of HTTP Requests**
- 🎯 **Host, Method, Status & MIME Type Filtering**
- 🧠 **Regex, Case-Sensitive, and Invert Toggles**
- 🎛️ **Advanced Filters** for deeper control
- 📤 **Export to JSON** with one click
- 🛡️ **Burp Scope Integration**
- ⚡ **Optimized for speed** – <150KB JAR, no runtime dependencies

---

## 🖥️ How to Use

1. **Download the JAR:**  
   👉 [Download UniReq v1.0.0](https://github.com/Johnfire45/UniReq/releases/tag/v1.0.0)

2. **Install in Burp:**
   - Open **Burp Suite**
   - Go to `Extensions → Add`
   - Select the downloaded `unireq-deduplicator-1.0.0.jar`

3. **Start Using:**
   - Go to the **UniReq tab**
   - View deduplicated requests in real-time
   - Apply filters (inline or advanced)
   - Export visible results as JSON

---

## 📸 UI Preview

![UniReq Screenshot](docs/unireq_ui.png)

---

## 📦 Release Info

- **Latest Version**: `v1.0.0`
- **Built With**: [Burp Montoya API](https://portswigger.net/burp/extender/api)
- **Compatible With**: Burp Suite Pro v2023.12 and above
- **License**: MIT

---

## 🔒 Security Notes

- No external network calls
- Fully offline-capable
- Minimal attack surface (no reflection, no eval, no unsafe deserialization)

---

## 🛠️ For Developers

If you're a developer interested in contributing or extending UniReq:

```bash
# Clone and build
git clone https://github.com/Johnfire45/UniReq.git
cd UniReq
mvn clean package