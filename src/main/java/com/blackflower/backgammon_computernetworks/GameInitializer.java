package com.blackflower.backgammon_computernetworks;

import com.blackflower.backgammon_computernetworks.model.GameState;
import com.blackflower.backgammon_computernetworks.model.PlayerColor;

/**
 *
 * @author emirs
 */
public class GameInitializer {
    private GameInitializer() { }

    /** Klasik tavla açılış dizilimini oluşturur. */
    public static GameState defaultSetup() {
        GameState state = new GameState();
        // 15’er pulun standart yerleşimi
        state.placeCheckers(1,  2, PlayerColor.WHITE);
        state.placeCheckers(12, 5, PlayerColor.WHITE);
        state.placeCheckers(17, 3, PlayerColor.WHITE);
        state.placeCheckers(19, 5, PlayerColor.WHITE);

        state.placeCheckers(24, 2, PlayerColor.BLACK);
        state.placeCheckers(13, 5, PlayerColor.BLACK);
        state.placeCheckers(8,  3, PlayerColor.BLACK);
        state.placeCheckers(6,  5, PlayerColor.BLACK);

        state.setCurrentTurn(PlayerColor.WHITE);
        return state;
    }
}
