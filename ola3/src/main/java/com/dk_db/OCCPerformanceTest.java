package com.dk_db;

import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class OCCPerformanceTest {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ola3";
    private static final String USER = "root";
    private static final String PASSWORD = "KT&F&(D5^._;cfG";

    private static final int THREAD_COUNT = 10;
    private static final AtomicInteger successfulTransactions = new AtomicInteger(0);
    private static final AtomicInteger retriesDueToOCC = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        long startTime = System.currentTimeMillis(); // Start measuring time

        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadId = i + 1;
            executor.submit(() -> {
                boolean success = false;
                System.out.println("Thread " + threadId + " started...");
                while (!success) {
                    success = updateTournamentOptimisticConcurrency(1, 4, threadId);
                }
                System.out.println("Thread " + threadId + " finished.");
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis(); // End measuring time

        // Display results
        System.out.println("Execution Time (ms): " + (endTime - startTime));
        System.out.println("Number of successful transactions: " + successfulTransactions.get());
        System.out.println("Number of retries due to version mismatch: " + retriesDueToOCC.get());
    }

    public static boolean updateTournamentOptimisticConcurrency(int tournamentId, int playerId, int threadId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);
            System.out.println("Thread " + threadId + " is attempting to register player...");

            // Step 1: Get tournament info
            String selectSQL = "SELECT max_players, (SELECT COUNT(*) FROM Tournament_Registrations WHERE tournament_id = ?) AS current_players, version FROM Tournaments WHERE tournament_id = ?";
            int maxPlayers, currentPlayers, currentVersion;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSQL)) {
                selectStmt.setInt(1, tournamentId);
                selectStmt.setInt(2, tournamentId);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    System.out.println("Thread " + threadId + ": Tournament not found.");
                    return true; // Tournament not found, exit without retry
                }

                maxPlayers = rs.getInt("max_players");
                currentPlayers = rs.getInt("current_players");
                currentVersion = rs.getInt("version");

                if (currentPlayers >= maxPlayers) {
                    System.out.println("Thread " + threadId + ": Tournament is full. Exiting...");
                    conn.rollback();
                    return true; // Tournament full, exit without retry
                }
            }

            // Step 2: OCC Check - Attempt to update tournament version FIRST
            String updateSQL = "UPDATE Tournaments SET version = version + 1 WHERE tournament_id = ? AND version = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                updateStmt.setInt(1, tournamentId);
                updateStmt.setInt(2, currentVersion);

                int rowsUpdated = updateStmt.executeUpdate();
                if (rowsUpdated == 0) {
                    System.out.println("Thread " + threadId + ": OCC failed (version mismatch). Retrying...");
                    conn.rollback();
                    retriesDueToOCC.incrementAndGet(); // Increment OCC retry count
                    return false; // OCC failed, retry
                }
            }

            // Step 3: Check if player is already registered
            String checkPlayerSQL = "SELECT 1 FROM Tournament_Registrations WHERE tournament_id = ? AND player_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkPlayerSQL)) {
                checkStmt.setInt(1, tournamentId);
                checkStmt.setInt(2, playerId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    System.out.println("Thread " + threadId + ": Player already registered. Exiting...");
                    conn.rollback();
                    return true; // Player already registered, exit without retry
                }
            }

            // Step 4: Insert player AFTER the OCC check
            String insertSQL = "INSERT INTO Tournament_Registrations (tournament_id, player_id) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                insertStmt.setInt(1, tournamentId);
                insertStmt.setInt(2, playerId);
                insertStmt.executeUpdate();
            }

            conn.commit();
            successfulTransactions.incrementAndGet(); // Increment success count
            System.out.println("Thread " + threadId + ": Successfully registered player!");
            return true;
        } catch (SQLException e) {
            System.out.println("Thread " + threadId + ": SQL Exception occurred. Retrying...");
            return false; // Retry on failure
        }
    }
}
