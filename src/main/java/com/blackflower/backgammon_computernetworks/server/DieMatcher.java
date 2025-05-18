package com.blackflower.backgammon_computernetworks.server;

import com.blackflower.backgammon_computernetworks.model.*;

/**
 *
 * @author emirs
 */
final class DieMatcher {
    private DieMatcher() {}

    /** Hangi zar bu hamleyi karşılıyor?  bulunursa zar DEĞERİNİ (1-6) döndürür; yoksa -1 */
    static int findDieIndex(GameState st, int from, int to) {

        PlayerColor pc  = st.getCurrentTurn();
        Dice[]      dice = st.getDice();
        boolean[]   used = st.getDiceUsed();

        for (int i = 0; i < used.length; i++) {
            if (used[i]) continue;                 // bu zar tüketilmiş
            int die = dice[i % 2].get();

            /* ---------- BAR’dan giriş ---------- */
            if (from == 0) {
                int entry = (pc.direction > 0)      // WHITE yönü +1
                          ? 25 - die               // 25 – die  ⇒ 1-6
                          : die;                   // BLACK ⇒ 24-19
                if (entry == to) return die;       // doğru zar
                continue;
            }

            /* ---------- Normal / bearing-off ---------- */
            int expected = from + die * pc.direction;
            if (expected < 1 || expected > 24) expected = 25;    // taş dışarı çıktı

            boolean overshoot = st.canBearOff(pc) && to == 25 &&
                    ((pc.direction > 0 && expected > 24) ||
                     (pc.direction < 0 && expected < 1));

            if (overshoot || expected == to) return die;
        }
        return -1;                                   // hiçbir zar eşleşmedi
    }
}
