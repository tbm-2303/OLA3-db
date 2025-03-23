package com.dk_db;

import java.sql.*;
import java.util.concurrent.*;

public class PCCPerformanceTest {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ola3";
    private static final String USER = "root";
    private static final String PASSWORD = "KT&F&(D5^._;cfG";

    private static final int THREAD_COUNT = 10;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        long startTime = System.currentTimeMillis(); // Start measuring time

        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadId = i + 1; // Assign an ID to each thread for clarity
            executor.submit(() -> updateMatchResultPessimistically(1, 2, threadId));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis(); // End measuring time

        // Display results
        System.out.println("\nFinal Metrics:");
        System.out.println("Execution Time (ms): " + (endTime - startTime));
    }

    public static void updateMatchResultPessimistically(int matchId, int winnerId, int threadId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);

            System.out.println("Thread " + threadId + " attempting to update match " + matchId);

            // Step 1: Attempt to lock the row
            String selectSQL = "SELECT match_id FROM Matches WHERE match_id = ? FOR UPDATE";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSQL)) {
                selectStmt.setInt(1, matchId);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    System.out.println("Thread " + threadId + " failed: Match not found.");
                    return;
                }

                System.out.println("Thread " + threadId + " acquired lock on match " + matchId);
            }

            // Step 2: Validate winnerId is one of the players in the match
            String validateSQL = "SELECT player1_id, player2_id FROM Matches WHERE match_id = ?";
            try (PreparedStatement validateStmt = conn.prepareStatement(validateSQL)) {
                validateStmt.setInt(1, matchId);
                ResultSet rs = validateStmt.executeQuery();
                if (rs.next()) {
                    int player1 = rs.getInt("player1_id");
                    int player2 = rs.getInt("player2_id");
                    if (winnerId != player1 && winnerId != player2) {
                        System.out.println("Thread " + threadId + " failed: Winner ID not valid.");
                        conn.rollback();
                        return;
                    }
                }
            }

            // Step 3: Update match result
            String updateSQL = "UPDATE Matches SET winner_id = ? WHERE match_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                updateStmt.setInt(1, winnerId);
                updateStmt.setInt(2, matchId);
                updateStmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Thread " + threadId + " successfully updated match " + matchId);
        } catch (SQLException e) {
            System.out.println("Thread " + threadId + " encountered an error: " + e.getMessage());
        }
    }
}
