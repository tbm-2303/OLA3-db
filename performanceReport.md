# **Performance Analysis Report: Optimistic vs. Pessimistic Concurrency Control**

## **📝 Student Names: [Timothy Busk Mortensen]**

---

## **📌 Introduction**
### **Objective:**
This report analyzes and compares the performance of **Optimistic Concurrency Control (OCC) vs. Pessimistic Concurrency Control (PCC)** when handling concurrent transactions in an Esports Tournament database.

### **Scenario Overview:**
- **OCC is tested** by simulating multiple players registering for the same tournament concurrently.
- **PCC is tested** by simulating multiple administrators updating the same match result simultaneously.

---

## **📌 Experiment Setup**
### **Database Schema Used:**
```sql
CREATE TABLE Players (
    player_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    ranking INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Tournaments (
    tournament_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    game VARCHAR(50) NOT NULL,
    max_players INT NOT NULL,
    start_date DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 1
);

CREATE TABLE Tournament_Registrations (
    registration_id INT PRIMARY KEY AUTO_INCREMENT,
    tournament_id INT NOT NULL,
    player_id INT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tournament_id) REFERENCES Tournaments(tournament_id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES Players(player_id) ON DELETE CASCADE
);

CREATE TABLE Matches (
    match_id INT PRIMARY KEY AUTO_INCREMENT,
    tournament_id INT NOT NULL,
    player1_id INT NOT NULL,
    player2_id INT NOT NULL,
    winner_id INT NULL,
    match_date DATETIME NOT NULL,
    FOREIGN KEY (tournament_id) REFERENCES Tournaments(tournament_id) ON DELETE CASCADE,
    FOREIGN KEY (player1_id) REFERENCES Players(player_id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES Players(player_id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES Players(player_id) ON DELETE SET NULL
);
```

### **Concurrency Control Techniques Implemented:**
- **Optimistic Concurrency Control (OCC)** using a **version column** in the `Tournaments` table.
- **Pessimistic Concurrency Control (PCC)** using `SELECT ... FOR UPDATE` when updating `Matches`.

### **Test Parameters:**
| Parameter        | Value |
|-----------------|-------|
| **Number of concurrent transactions** | [10 from ExecutorService] |
| **Database** | [MySQL 8.0] |
| **Java Version** | [OpenJDK 23.0.2 ] |
| **IDE** | [IntelliJ IDEA 2024.3.2] |
| **Java Thread Pool Size** | [10 (from Executors.newFixedThreadPool(10))] |

---

## **📌 Results & Observations**

### **1️⃣ Optimistic Concurrency Control (OCC) Results**
**Test Scenario:** 
- Simulated 10 concurrent threads attempting to register the same player into the same tournament. 
- To create multiple threads that run transactions concurrently, I used the same approach as in the recommended note; https://github.com/Tine-m/final-assignment/blob/main/performance-test.md.
- I used Version based OCC on registering players in tournaments:
    - Reads the current version of the tournament.
    - Attempts to insert the player into the Tournament_Registrations table.
    - Commits only if the tournament version has not changed, ensuring only one transaction succeeds.
    - If a version mismatch occurs, the transaction rolls back and retries.

| **Metric** | **Value** |
|-----------|----------|
| Execution Time (ms) | [1130] |
| Number of successful transactions | [1] |
| Number of retries due to version mismatch | [9] |

**Observations:**
- Only one thread registered a player. All the subsequent transactions first failed due to version mismatch(OCC). The 9 failing threads all retried. On the the retry they all failed due to the player already being registered in that tournament. This will also be reflected in the console but with 10 threads its to big to include screenshot.

---

### **2️⃣ Pessimistic Concurrency Control (PCC) Results**
**Test Scenario:** 
- To evaluate Pessimistic Concurrency Control (PCC), I simulated multiple concurrent threads attempting to update the same match result in a database.

| **Metric** | **Value** |
|-----------|----------|
| Execution Time (ms) | [1173] |
| Number of successful transactions | [10] |
| Number of transactions that had to wait due to locks | [9] |

**Observations:**
- All transactions eventually succeeded.
- All other transactions had to wait for the lock before proceeding, except the first. 
- High load testing makes the execution time higher since transactions must wait on each other. 

---

## **📌 Comparison Table**
| **Metric**               | **Optimistic CC** | **Pessimistic CC** |
|--------------------------|------------------|------------------|
| **Execution Time**       | [1130] | [1173] |
| **Transaction Failures** | [9 (due to OCC retries)] | [0 (all transactions eventually succeed after waiting)] |
| **Lock Contention**      | [Low (only one update wins, others fail and retry)] | [High (all other threads wait for lock to be released)] |
| **Best Use Case**       | [When conflicts are rare] | [When conflicts are frequent] |

---

## **📌 Conclusion & Recommendations**
### **Key Findings:**
- OCC is best when you have few conflict since failed transactions must retry(we could also have OCC where a transaction dont retry but gives an error etc.). When you have few conflicts, OCC is preferable since we dont have to lock any record. this allows multiple transactions to run concurrently, in case of conflict one will fail at commit level. PCC is best for scenarios where you expect many conflict to occur. In this high load scenario each transaction locks the row such that subsequent transactions must wait until the key is available. This is done exactly to prevent those conflict from arising; each transaction instead runs in sequence and no conflict can happen. 
### **Final Recommendations:**
- For Low Contention Scenarios: Use Optimistic Concurrency Control (OCC) to maximize performance and allow higher concurrency. Since OCC avoids locking, it is ideal for read-heavy workloads(conflicts detected at update time) or cases where conflicts are rare.

- For High Contention Scenarios: Use Pessimistic Concurrency Control (PCC) to prevent frequent conflicts and ensure data consistency. PCC is suitable for write-heavy workloads where transactions must not fail due to version mismatches.