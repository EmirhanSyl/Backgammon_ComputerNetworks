package com.blackflower.backgammon_computernetworks.controller;

import com.blackflower.backgammon_computernetworks.model.PlayerColor;

/**
 *
 * @author emirs
 */
public final class OpeningRollController implements GameStateController {

    @Override
    public void enter(GameContext ctx) {
        // Her iki oyuncu tek zar atar – pratikte iki zarı birden atıp büyük başlatıyoruz
        ctx.state().rollDice();
        int d1 = ctx.state().getDice()[0].get();
        int d2 = ctx.state().getDice()[1].get();

        if (d1 == d2) { enter(ctx); return; }   // eşitlik: yeniden at
        PlayerColor starter = d1 > d2 ? PlayerColor.WHITE : PlayerColor.BLACK;
        ctx.state().setCurrentTurn(starter);
        ctx.setStateController(new MovePhaseController());
        ctx.getStateController().enter(ctx);
    }
}