package com.blackflower.backgammon_computernetworks.server;   // <-- kendi paketinizi yazın

import com.blackflower.backgammon_computernetworks.controller.*;
import com.blackflower.backgammon_computernetworks.model.*;
import java.util.HashSet;
import java.util.Set;

public final class OnlineMoveController extends GameStateControllerAdapter {

    private final ClientNetwork net;
    private final GameState state;
    public final PlayerColor myColor;
    private int selected = -1;                       // GUI highlight

    public OnlineMoveController(ClientNetwork net, GameState state, PlayerColor myColor) {
        this.net = net;
        this.state = state;
        this.myColor = myColor;
    }

    /* ----------------  GUI tıklamaları ---------------- */
    @Override
    public void onPointSelected(GameContext ctx, int point) {

        if (state.getCurrentTurn() != myColor) {
            selected = -1;
            return;
        }

        /* ----------- İLK TIKLAMA → pul seç ---------------- */
        if (selected == -1) {

            /* BAR ancak bar’da pul varsa seçilebilir */
            if (point == 0 && state.checkersOnBar(myColor) > 0) {
                selected = 0;                // ✔ bar’daki pulu seçtik
                return;
            }

            /* Tahta üzerindeki kendi pul */
            if (point != 0
                    && !state.getPoint(point).isEmpty()
                    && state.getPoint(point).peek().color() == myColor) {
                selected = point;
            }
            return;
        }

        /* ----------- İKİNCİ TIKLAMA → hedef ---------------- */
        int die = DieMatcher.findDieIndex(state, selected, point);
        if (die == -1) {
            selected = -1;
            return;
        }

        net.send(new LegacyMessage("MOVE")
                .put("from", selected)
                .put("to", point)
                .put("die", die));

        selected = -1;
    }

    /* ----------------  BoardPanel için ---------------- */
    public int getSelected() {
        return selected;
    }

    /**
     * Mavi hedef noktalarını üretir (MovePhaseController’dan alındı).
     */
    public java.util.Set<Integer> getLegalTargets() {
        java.util.Set<Integer> targets = new java.util.HashSet<>();
        if (selected == -1) {
            return targets;
        }

        Dice[] dice = state.getDice();
        boolean[] used = state.getDiceUsed();
        int dir = myColor.direction;

        for (int i = 0; i < used.length; i++) {
            if (used[i]) {
                continue;                  // bu zar tüketildi
            }
            int die = dice[i % 2].get();

            /* ---------- Hedef karesi ---------- */
            int to;
            if (selected == 0) {                    // BAR’dan giriş
                to = (dir > 0) ? die // Beyaz  → 1-6
                        : 25 - die; // Siyah  → 24-19
            } else {
                to = selected + die * dir;
                /* bearing-off aşımı */
                if (state.canBearOff(myColor)
                        && ((dir > 0 && to > 24) || (dir < 0 && to < 1))) {
                    to = 25;
                }
            }

            /* ---------- BEARING-OFF ---------- */
            if (to == 25) {
                targets.add(25);
                continue;
            }

            /* ---------- Tahta sınırı ---------- */
            if (to < 1 || to > 24) {
                continue;
            }

            /* ---------- Blok kontrolü ---------- */
            Point dest = state.getPoint(to);
            if (dest.size() >= 2 && dest.peek().color() != myColor) {
                continue;
            }

            targets.add(to);
        }
        return targets;
    }

    public int getSelectedPoint() {        // <-- eklendi
        return selected;                   // (getSelected() ile aynı)
    }
}
