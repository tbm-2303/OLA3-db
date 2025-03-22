DELIMITER $$

CREATE PROCEDURE UpdateRanking(IN playerID INT)
BEGIN
    START TRANSACTION;
    
    -- Pessimistic Locking: Lock the player's row to prevent concurrent updates
    SELECT ranking FROM Players WHERE player_id = playerID FOR UPDATE;
    
    -- Increase the player's ranking
    UPDATE Players SET ranking = ranking + 10 WHERE player_id = playerID;

    COMMIT;
END $$

DELIMITER ;