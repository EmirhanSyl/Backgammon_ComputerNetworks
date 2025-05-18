package com.blackflower.backgammon_computernetworks.controller;

import com.blackflower.backgammon_computernetworks.model.GameState;
import com.blackflower.backgammon_computernetworks.model.Move;
import com.blackflower.backgammon_computernetworks.model.MoveValidator;
import com.blackflower.backgammon_computernetworks.model.PlayerColor;
import com.blackflower.backgammon_computernetworks.server.OnlineMoveController;

/**
 *
 * @author emirs
 */
public final class GameContext {

    private final GameState state;
    private final MoveValidator validator;
    private GameStateController stateController;

    public GameContext(GameState state, MoveValidator validator) {
        this.state = state;
        this.validator = validator;
    }
    
    public OnlineMoveController asOnline() {                   // <-- eklendi
        return (stateController instanceof OnlineMoveController omc) ? omc : null;
    }

    /* ---------- State machine bağlantıları ------------------------------ */
    public void setStateController(GameStateController sc) {
        this.stateController = sc;
    }

    public GameStateController getStateController() {
        return stateController;
    }

    /* ---------- Facade benzeri yardımcılar ------------------------------ */
    public GameState state() {
        return state;
    }

    public MoveValidator validator() {
        return validator;
    }

    public void applyMove(Move mv, int dieIndex) {
        // model güncelle
        state.moveChecker(mv.from(), mv.to());
        state.markDieUsed(dieIndex);
    }

    public void endTurn() {
        state.setCurrentTurn(
                state.getCurrentTurn() == PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE);
        state.rollDice();
    }
    
    public MovePhaseController asMovePhase() {
        return stateController instanceof MovePhaseController m ? m : null;
    }
}
