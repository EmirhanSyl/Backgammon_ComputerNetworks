package com.blackflower.backgammon_computernetworks.server;

import com.blackflower.backgammon_computernetworks.model.*;

/**
 *
 * @author emirs
 */
final class DieMatcher {

    private DieMatcher() {
    }

    /**
     * Hangi zar bu hamleyi karşılıyor? bulunursa zar DEĞERİNİ (1-6) döndürür;
     * yoksa -1
     */
    static int findDieIndex(GameState st, int from, int to) {

        PlayerColor pc = st.getCurrentTurn();
        Dice[] dice = st.getDice();
        boolean[] used = st.getDiceUsed();
        int dir = pc.direction;

        for (int i = 0; i < used.length; i++) {
            if (used[i]) {
                continue;                     // bu zar tüketilmiş
            }
            int die = dice[i % 2].get();               // fiziksel değer

            /* ---- BAR'dan giriş ---- */
            if (from == 0) {
                int entry = (dir > 0) ? 25 - die : die;
                if (entry == to) {
                    return i;
                }
                continue;
            }

            /* ---- Normal / bearing-off ---- */
            int expected = from + die * dir;
            if (expected < 1 || expected > 24) {
                expected = 25;   // aşım
            }
            boolean overshoot = st.canBearOff(pc) && to == 25
                    && ((dir > 0 && expected > 24)
                    || (dir < 0 && expected < 1));

            if (overshoot || expected == to) {
                return i;
            }
        }
        return -1;                                              // eşleşme yok
    }
}
