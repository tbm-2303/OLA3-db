package com.dk_db;

import java.sql.*;

public class OptimisticConcurrency {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ola3";
    private static final String USER = "root";
    private static final String PASSWORD = "KT&F&(D5^._;cfG";

    public static void main(String[] args) throws SQLException {
        /* 1
        //1. used for optimistic control

        int tournamentId = 1;

        Thread admin1 = new Thread(() -> updateTournamentStartDate(tournamentId, "2025-04-10 16:00:00"));
        Thread admin2 = new Thread(() -> updateTournamentStartDate(tournamentId, "2025-04-10 17:00:00"));

        admin1.start();
        admin2.start();
        */

        /* 2
        //2. used for pessimistic control
        Thread admin1 = new Thread(() -> updateMatchResultTest(2, 4), "Admin-1");
        Thread admin2 = new Thread(() -> updateMatchResultTest(2, 3), "Admin-2");

        admin1.start();
        admin2.start();
        */

        /* 3
        //3. Handle Transactions for Tournament Registrations
        try {
            registerPlayerForTournament(2,3); // Example
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
        /*

        /* 4
        //4.Implement a Stored Procedure for Safe Ranking Updates

        int playerID = 1; // Change to test different players

        Thread thread1 = new Thread(() -> updatePlayerRankingWithLock(playerID), "Thread 1");
        Thread thread2 = new Thread(() -> updatePlayerRankingWithLock(playerID), "Thread 2");

        thread1.start();
        thread2.start();

         */

        /* 4 test
        int playerID = 1; // Example player ID

        // Create two threads to simulate concurrent ranking updates
        Thread thread1 = new Thread(() -> updatePlayerRankingWithLockTest(playerID), "Thread-1");
        Thread thread2 = new Thread(() -> updatePlayerRankingWithLockTest(playerID), "Thread-2");

        // Start both threads
        thread1.start();
        thread2.start();

         */

        // Simulating two players trying to register at the same time
        Thread thread1 = new Thread(() -> registerPlayerWithLock(3, 3), "Thread-1");
        Thread thread2 = new Thread(() -> registerPlayerWithLock(3, 4), "Thread-2");

        thread1.start();
        thread2.start();

    }


    // 1. optimistic concurrency control
    public static void updateTournamentStartDate(int tournamentId, String newStartDate) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);

            String selectSQL = "SELECT version FROM Tournaments WHERE tournament_id = ?";
            int currentVersion;

            // Step 1: Read the version
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSQL)) {
                selectStmt.setInt(1, tournamentId);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    System.out.println(Thread.currentThread().getName() + " - Tournament not found.");
                    return;
                }
                currentVersion = rs.getInt("version");
            }

            // Simulated delay
            Thread.sleep(2000);

            // Step 2: Attempt to update
            String updateSQL = "UPDATE Tournaments SET start_date = ?, version = version + 1 WHERE tournament_id = ? AND version = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                updateStmt.setString(1, newStartDate);
                updateStmt.setInt(2, tournamentId);
                updateStmt.setInt(3, currentVersion);

                int rowsUpdated = updateStmt.executeUpdate();

                if (rowsUpdated == 0) {
                    System.out.println(Thread.currentThread().getName() + " - Update failed: another admin modified the tournament.");
                    conn.rollback();
                } else {
                    conn.commit();
                    System.out.println(Thread.currentThread().getName() + " - Tournament updated successfully!");
                }
            }
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 2. pessimistic locking
    public static void updateMatchResult(int matchId, int winnerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false); // Begin transaction

            // Step 1: Lock the match row
            String lockSQL = "SELECT match_id FROM Matches WHERE match_id = ? FOR UPDATE";
            try (PreparedStatement lockStmt = conn.prepareStatement(lockSQL)) {
                lockStmt.setInt(1, matchId);
                lockStmt.executeQuery(); // Locks the row
            }

            // Simulate some processing delay
            Thread.sleep(5000);

            // Step 2: Update the winner
            String updateSQL = "UPDATE Matches SET winner_id = ? WHERE match_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                updateStmt.setInt(1, winnerId);
                updateStmt.setInt(2, matchId);
                updateStmt.executeUpdate();
            }
            conn.commit(); // Commit transaction
        }
        catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 2. used for examples in readme.md to show the pessimistic locking
    public static void updateMatchResultTest(int matchId, int winnerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false); // Begin transaction

            long startTime = System.currentTimeMillis(); // Log start time
            System.out.println(Thread.currentThread().getName() + " - Trying to acquire lock...");

            // Step 1: Lock the match row
            String lockSQL = "SELECT match_id FROM Matches WHERE match_id = ? FOR UPDATE";
            try (PreparedStatement lockStmt = conn.prepareStatement(lockSQL)) {
                lockStmt.setInt(1, matchId);
                lockStmt.executeQuery(); // This will block if another transaction has the lock
            }

            long lockTime = System.currentTimeMillis() - startTime; // Calculate wait time
            System.out.println(Thread.currentThread().getName() + " - Lock acquired after " + lockTime + "ms!");

            // Simulate processing delay
            Thread.sleep(5000); // Simulate a delay before committing

            // Step 2: Update the winner
            String updateSQL = "UPDATE Matches SET winner_id = ? WHERE match_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                updateStmt.setInt(1, winnerId);
                updateStmt.setInt(2, matchId);
                updateStmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
            System.out.println(Thread.currentThread().getName() + " - Match updated successfully!");

        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    //3. Handle Transactions for Tournament Registrations
    public static void registerPlayerForTournament(int tournamentId, int playerId) throws SQLException {
        String maxPlayersQuery = "SELECT max_players FROM Tournaments WHERE tournament_id = ?";
        String countPlayersQuery = "SELECT COUNT(*) FROM Tournament_Registrations WHERE tournament_id = ?";
        String insertQuery = "INSERT INTO Tournament_Registrations (tournament_id, player_id) VALUES (?, ?)";
        String updateRankingQuery = "UPDATE Players SET ranking = ranking + 1 WHERE player_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false); // Start transaction

            int maxPlayers;
            int currentPlayers;

            // Step 1: Get max players for the tournament
            try (PreparedStatement maxPlayersStmt = conn.prepareStatement(maxPlayersQuery)) {
                maxPlayersStmt.setInt(1, tournamentId);
                ResultSet rs = maxPlayersStmt.executeQuery();
                if (rs.next()) {
                    maxPlayers = rs.getInt("max_players");
                } else {
                    conn.rollback();
                    throw new SQLException("Tournament not found.");
                }
            }

            // Step 2: Get current player count
            try (PreparedStatement countPlayersStmt = conn.prepareStatement(countPlayersQuery)) {
                countPlayersStmt.setInt(1, tournamentId);
                ResultSet rs = countPlayersStmt.executeQuery();
                if (rs.next()) {
                    currentPlayers = rs.getInt(1);
                } else {
                    conn.rollback();
                    throw new SQLException("Failed to retrieve player count.");
                }
            }

            // Step 3: Check if the tournament is full
            if (currentPlayers >= maxPlayers) {
                conn.rollback();
                throw new SQLException("Tournament is full. Registration failed.");
            }

            // Step 4: Insert registration and update ranking
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                 PreparedStatement updateRankingStmt = conn.prepareStatement(updateRankingQuery)) {
                // insert
                insertStmt.setInt(1, tournamentId);
                insertStmt.setInt(2, playerId);
                insertStmt.executeUpdate();
                // update
                updateRankingStmt.setInt(1, playerId);
                updateRankingStmt.executeUpdate();

                conn.commit();
            }
            catch (SQLException e) {
                conn.rollback();
            }
        }
        catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    //4. Implement a Stored Procedure for Safe Ranking Updates
    public static void updatePlayerRankingWithLock(int playerID) {

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false); // Begin transaction

            CallableStatement stmt = conn.prepareCall("CALL UpdateRanking(?)");
            stmt.setInt(1, playerID);
            stmt.execute();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 4. Implement a Stored Procedure for Safe Ranking Updates
    public static void updatePlayerRankingWithLockTest(int playerID) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false); // Begin transaction

            System.out.println(Thread.currentThread().getName() + " - Trying to update ranking...");

            // Simulate some processing before calling the procedure (to stagger execution)
            if (Thread.currentThread().getName().equals("Thread-1")) {
                Thread.sleep(2000); // Delay Thread 1 so Thread 2 gets the lock first
            }

            long startTime = System.currentTimeMillis();
            CallableStatement stmt = conn.prepareCall("CALL UpdateRanking(?)");
            stmt.setInt(1, playerID);
            stmt.execute(); // This will block if another thread has the lock inside MySQL
            long elapsedTime = System.currentTimeMillis() - startTime;

            System.out.println(Thread.currentThread().getName() + " - Ranking updated successfully after waiting " + elapsedTime + "ms!");

        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    //5.
    public static void registerPlayerWithLock(int tournamentId, int playerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false); // Begin transaction

            System.out.println(Thread.currentThread().getName() + " - Trying to register...");

            // Step 1: Lock the tournament row
            String lockQuery = "SELECT max_players FROM Tournaments WHERE tournament_id = ? FOR UPDATE";
            int maxPlayers;

            try (PreparedStatement lockStmt = conn.prepareStatement(lockQuery)) {
                lockStmt.setInt(1, tournamentId);
                ResultSet rs = lockStmt.executeQuery();
                if (rs.next()) {
                    maxPlayers = rs.getInt("max_players");
                } else {
                    conn.rollback();
                    System.out.println(Thread.currentThread().getName() + " - Tournament not found.");
                    return;
                }
            }

            // Step 2: Count current registrations
            String countQuery = "SELECT COUNT(*) FROM Tournament_Registrations WHERE tournament_id = ?";
            int currentPlayers;

            try (PreparedStatement countStmt = conn.prepareStatement(countQuery)) {
                countStmt.setInt(1, tournamentId);
                ResultSet rs = countStmt.executeQuery();
                if (rs.next()) {
                    currentPlayers = rs.getInt(1);
                } else {
                    conn.rollback();
                    System.out.println(Thread.currentThread().getName() + " - Failed to count players.");
                    return;
                }
            }

            // Step 3: Check if registration is possible
            if (currentPlayers >= maxPlayers) {
                conn.rollback();
                System.out.println(Thread.currentThread().getName() + " - Tournament is full. Registration failed.");
                return;
            }

            // Step 4: Register the player
            String insertQuery = "INSERT INTO Tournament_Registrations (tournament_id, player_id) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setInt(1, tournamentId);
                insertStmt.setInt(2, playerId);
                insertStmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
            System.out.println(Thread.currentThread().getName() + " - Registration successful!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}
