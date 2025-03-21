package com.dk_db;

import java.sql.*;

public class OptimisticConcurrency {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ola3";
    private static final String USER = "root";
    private static final String PASSWORD = "KT&F&(D5^._;cfG";

    public static void main(String[] args) {
        int tournamentId = 1;

        Thread admin1 = new Thread(() -> updateTournamentStartDate(tournamentId, "2025-04-10 16:00:00"));
        Thread admin2 = new Thread(() -> updateTournamentStartDate(tournamentId, "2025-04-10 17:00:00"));

        admin1.start();
        admin2.start();
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
