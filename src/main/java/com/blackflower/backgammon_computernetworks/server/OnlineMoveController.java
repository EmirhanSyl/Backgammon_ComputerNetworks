package com.blackflower.backgammon_computernetworks.server;   // <-- kendi paketinizi yazın

import com.blackflower.backgammon_computernetworks.controller.*;
import com.blackflower.backgammon_computernetworks.model.*;
import java.util.HashSet;
import java.util.Set;

public final class OnlineMoveController extends GameStateControllerAdapter {

    private final ClientNetwork net;
    private final GameState state;
    private final PlayerColor myColor;
    private int selected = -1;                       // GUI highlight

    public OnlineMoveController(ClientNetwork net, GameState state, PlayerColor myColor) {
        this.net = net;
        this.state = state;
        this.myColor = myColor;
    }

    /* ----------------  GUI tıklamaları ---------------- */
    @Override
    public void onPointSelected(GameContext ctx, int point) {

        /* 1) sıra bende mi? */
        if (state.getCurrentTurn() != myColor) {
            selected = -1;
            return;
        }

        /* 2) ilk tıklama → pul seç */
        if (selected == -1) {
            if (point == 0
                    || // bar
                    (!state.getPoint(point).isEmpty()
                    && state.getPoint(point).peek().color() == myColor)) {
                selected = point;
            }
            return;
        }

        /* 3) ikinci tıklama → hedef belirle */
        int dieIdx = DieMatcher.findDieIndex(state, selected, point);
        if (dieIdx == -1) {
            selected = -1;
            return;
        }

        /* 4) MOVE mesajını zar İNDİSİ ile gönder */
        net.send(new LegacyMessage("MOVE")
                .put("from", selected)
                .put("to", point)
                .put("dieIdx", dieIdx));

        selected = -1;                                // highlight sıfırla
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
                continue;                // bu zar tüketildi
            }
            int die = dice[i % 2].get();

            /* --- hedef koordinatı hesapla --- */
            int to;
            if (selected == 0) {                  // BAR’dan giriş
                to = (dir > 0) ? 25 - die : die;
            } else {
                to = selected + die * dir;
                if (state.canBearOff(myColor)
                        && ((dir > 0 && to > 24) || (dir < 0 && to < 1))) {
                    to = 25;                      // bearing-off aşımı
                }
            }

            /* --- BEARING-OFF --- */
            if (to == 25) {
                targets.add(25);
                continue;
            }

            /* --- Tahta aralığı dışında ise atla --- */
            if (to < 1 || to > 24) {
                continue;
            }

            /* --- Blok kontrolü: rakip ≥2 pul tutuyorsa inemezsin --- */
            Point dest = state.getPoint(to);
            if (dest.size() >= 2 && dest.peek().color() != myColor) {
                continue;
            }

            /* --- Geçerli nokta --- */
            targets.add(to);
        }
        return targets;
    }

    public int getSelectedPoint() {        // <-- eklendi
        return selected;                   // (getSelected() ile aynı)
    }
}
