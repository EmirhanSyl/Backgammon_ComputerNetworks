package com.blackflower.backgammon_computernetworks.controller;

import com.blackflower.backgammon_computernetworks.model.Dice;
import com.blackflower.backgammon_computernetworks.model.GameState;
import com.blackflower.backgammon_computernetworks.model.Move;

/**
 *
 * @author emirs
 */
public class MovePhaseController implements GameStateController {

    private int selectedPoint = -1;   // GUI’de seçili pulun bulunduğu nokta
    public int getSelectedPoint() { return selectedPoint; }
    public java.util.Set<Integer> getLegalTargets(GameContext ctx) {   // 3. madde için
        java.util.Set<Integer> targets = new java.util.HashSet<>();
        if (selectedPoint == -1) return targets;
   
        GameState st = ctx.state();
        Dice[] dice  = st.getDice();     boolean[] used = st.getDiceUsed();
        for (int i = 0; i < used.length; i++) {
            if (used[i]) continue;
            int die = dice[i % 2].get();
            int dir = st.getCurrentTurn().direction;
            int to  = selectedPoint + die * dir;
            if (st.canBearOff(st.getCurrentTurn()) &&   // bearing-off
                ((dir > 0 && to > 24) || (dir < 0 && to < 1))) to = 25;
            if (ctx.validator().isLegal(st, new Move(selectedPoint, to), die))
                targets.add(to);
        }
        return targets;
    }
    
    @Override
    public void enter(GameContext ctx) {
        ctx.state().rollDice();
    }

    @Override
    public void onPointSelected(GameContext ctx, int point) {
        GameState st = ctx.state();

        if (selectedPoint == -1) {
            // seçme
            if (st.getPoint(point).isEmpty()) return;
            if (st.getPoint(point).peek().color() != st.getCurrentTurn()) return;
            selectedPoint = point;
            return;
        }

        // Hedef nokta
        int dieIdx = matchDie(st, selectedPoint, point);
        if (dieIdx == -1) { selectedPoint = -1; return; }   // uygun zar yok

        Move mv = new Move(selectedPoint, point);
        if (ctx.validator().isLegal(st, mv, st.getDice()[dieIdx].get())) {
            ctx.applyMove(mv, dieIdx);
            if (st.allDiceUsed()) {
                ctx.endTurn();
            }
        }
        selectedPoint = -1;
    }

    /** from → to mesafesini hangi zar karşılıyor? */
    private int matchDie(GameState st, int from, int to) {
        int dist = Math.abs(from - to);
        Dice[] dice = st.getDice();
        boolean[] used = st.getDiceUsed();
        for (int i = 0; i < used.length; i++) {
            if (!used[i] && dice[i % 2].get() == dist) return i;
        }
        return -1;
    }
}
