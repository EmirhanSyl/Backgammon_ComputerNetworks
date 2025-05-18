package com.blackflower.backgammon_computernetworks.controller;

/**
 *
 * @author emirs
 */
public interface GameStateController {
    default void enter(GameContext ctx) { }
    default void onPointSelected(GameContext ctx, int point) { }
    default void onDiceClicked(GameContext ctx) { }
}