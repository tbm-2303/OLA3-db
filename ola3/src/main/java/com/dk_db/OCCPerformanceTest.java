package com.dk_db;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OCCPerformanceTest {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ola3";
    private static final String USER = "root";
    private static final String PASSWORD = "KT&F&(D5^._;cfG";

    public static void main(String[] args) throws InterruptedException {


        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                boolean success = false;
                while (!success) {
                    success = updateTournamentOptimisticConcurrency(1, 2);
                }
            });
        }
        executor.shutdown();

    }




    public static boolean updateTournamentOptimisticConcurrency(int tournamentId, int playerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);

            String selectSQL = "SELECT version FROM Tournaments WHERE tournament_id = ?";
            int currentVersion;

            // Step 1: Read the current version of the tournament
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSQL)) {
                selectStmt.setInt(1, tournamentId);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    System.out.println("Tournament not found.");
                    return true;// Tournament not found, exit
                }
                currentVersion = rs.getInt("version");
            }

            // Step 2: Insert player into Tournament_Registrations
            String insertSQL = "INSERT INTO Tournament_Registrations (tournament_id, player_id) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                insertStmt.setInt(1, tournamentId);
                insertStmt.setInt(2, playerId);
                insertStmt.executeUpdate();
            }

            // Step 3: Attempt to update tournament version (OCC check)
            String updateSQL = "UPDATE Tournaments SET version = version + 1 WHERE tournament_id = ? AND version = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                updateStmt.setInt(1, tournamentId);
                updateStmt.setInt(2, currentVersion);

                int rowsUpdated = updateStmt.executeUpdate();
                if (rowsUpdated == 0) {
                    conn.rollback();
                    return false; // OCC failed, retry
                }

                conn.commit();
                return true; // Success
            }
        } catch (SQLException e) {
            return false; // Retry on failure
        }
    }

}
