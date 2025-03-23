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
**Test Scenario:** [Describe how OCC was tested]

| **Metric** | **Value** |
|-----------|----------|
| Execution Time (ms) | [Your Value] |
| Number of successful transactions | [Your Value] |
| Number of retries due to version mismatch | [Your Value] |

**Observations:**
- [Summarize key findings related to OCC]

---

### **2️⃣ Pessimistic Concurrency Control (PCC) Results**
**Test Scenario:** [Describe how PCC was tested]

| **Metric** | **Value** |
|-----------|----------|
| Execution Time (ms) | [Your Value] |
| Number of successful transactions | [Your Value] |
| Number of transactions that had to wait due to locks | [Your Value] |

**Observations:**
- [Summarize key findings related to PCC]

---

## **📌 Comparison Table**
| **Metric**               | **Optimistic CC** | **Pessimistic CC** |
|--------------------------|------------------|------------------|
| **Execution Time**       | [Your Value] | [Your Value] |
| **Transaction Failures** | [Your Value] | [Your Value] |
| **Lock Contention**      | [Your Value] | [Your Value] |
| **Best Use Case**       | [Your Value] | [Your Value] |

---

## **Performance Comparison Chart**
_You *may* want to visualize your finding by including a  chart that illustrates the differences in execution time, successful transactions, and transactions with delays for OCC vs. PCC._

---

## **📌 Conclusion & Recommendations**
### **Key Findings:**
- [Summarize overall findings and comparison of OCC vs. PCC]

### **Final Recommendations:**
- [Provide recommendations based on the test results]