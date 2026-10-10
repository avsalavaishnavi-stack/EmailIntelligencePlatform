SMART EMAIL INTELLIGENCE PLATFORM

1. REQUIREMENTS
- JDK 17 or newer
- VS Code
- Maven (optional; mvnw is not included, use installed Maven)

2. RUN
Open VS Code terminal in backend:
cd backend
mvn spring-boot:run

Then open:
http://localhost:8081

3. REAL EMAIL WITHOUT GMAIL
Put any .eml files into the project-level inbox folder:
EmailIntelligencePlatform/inbox
The Java backend scans this folder every 3 seconds and automatically analyzes new emails.

You can also click Upload .eml in the dashboard.

4. DSA MODULES
Spam Detection        -> HashMap
Categorization        -> Trie
Duplicate Detection   -> Rabin-Karp rolling hash
Priority Prediction   -> Priority Queue / Heap logic
Similar Email Search  -> Dynamic Programming (LCS)
Contact Network        -> Graph + BFS/DFS
Fast Search            -> Trie + Suffix Array + KMP
Recommendation         -> LRU Cache + HashMap

5. DEMO
Click + Demo Email several times. Try the spam, academic, work and personal samples.
Use the search box, click an email for similar mail, use Spam/Duplicates/Priority, and open Contact Network.
