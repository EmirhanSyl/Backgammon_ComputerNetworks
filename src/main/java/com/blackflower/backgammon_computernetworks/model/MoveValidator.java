package com.blackflower.backgammon_computernetworks.model;

/**
 *
 * @author emirs
 */
public interface MoveValidator {
    boolean isLegal(GameState state, Move move, int dieValue);
}
