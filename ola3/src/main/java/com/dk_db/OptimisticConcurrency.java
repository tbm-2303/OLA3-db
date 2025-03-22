package com.dk_db;

import java.sql.*;

public class OptimisticConcurrency {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ola3";
    private static final String USER = "root";
    private static final String PASSWORD = "KT&F&(D5^._;cfG";

    public static void main(String[] args) {
        // used for optimistic control
        //int tournamentId = 1;

        //Thread admin1 = new Thread(() -> updateTournamentStartDate(tournamentId, "2025-04-10 16:00:00"));
        //Thread admin2 = new Thread(() -> updateTournamentStartDate(tournamentId, "2025-04-10 17:00:00"));

        //admin1.start();
        //admin2.start();

        // used for pessimistic control
        Thread admin1 = new Thread(() -> updateMatchResult2(2, 4), "Admin-1");
        Thread admin2 = new Thread(() -> updateMatchResult2(2, 3), "Admin-2");

        admin1.start();
        admin2.start();


    }

    public static void updateMatchResult(int matchId, int winnerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false); // Begin transaction

            // Step 1: Lock the match row
            String lockSQL = "SELECT match_id FROM Matches WHERE match_id = ? FOR UPDATE";
            try (PreparedStatement lockStmt = conn.prepareStatement(lockSQL)) {
                lockStmt.setInt(1, matchId);
                lockStmt.executeQuery(); // Locks the row
            }

            System.out.println(Thread.currentThread().getName() + " - Lock acquired, processing update...");

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
            System.out.println(Thread.currentThread().getName() + " - Match updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateMatchResult2(int matchId, int winnerId) {
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
}
