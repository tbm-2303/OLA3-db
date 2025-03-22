package com.dk_db;

import java.sql.*;

public class OptimisticConcurrency {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ola3";
    private static final String USER = "root";
    private static final String PASSWORD = "KT&F&(D5^._;cfG";

    public static void main(String[] args) throws SQLException {
        // used for optimistic control
        //int tournamentId = 1;

        //Thread admin1 = new Thread(() -> updateTournamentStartDate(tournamentId, "2025-04-10 16:00:00"));
        //Thread admin2 = new Thread(() -> updateTournamentStartDate(tournamentId, "2025-04-10 17:00:00"));

        //admin1.start();
        //admin2.start();

        // used for pessimistic control
        //Thread admin1 = new Thread(() -> updateMatchResult2(2, 4), "Admin-1");
        //Thread admin2 = new Thread(() -> updateMatchResult2(2, 3), "Admin-2");

        //admin1.start();
        //admin2.start();

        //3. transaction
        try {
            registerPlayerForTournament(2,3); // Example
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }



    }
    // pessimistic locking
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

    //used for examples in readme.md to show the pessimistic locking
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

    // optimistic concurrency control
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


}
