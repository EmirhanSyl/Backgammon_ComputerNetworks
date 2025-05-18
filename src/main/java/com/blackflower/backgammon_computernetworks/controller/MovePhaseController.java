package com.blackflower.backgammon_computernetworks.controller;

import com.blackflower.backgammon_computernetworks.model.Dice;
import com.blackflower.backgammon_computernetworks.model.GameState;
import com.blackflower.backgammon_computernetworks.model.Move;
import com.blackflower.backgammon_computernetworks.model.PlayerColor;
import com.blackflower.backgammon_computernetworks.model.Point;
import javax.swing.JOptionPane;

/**
 *
 * @author emirs
 */
public class MovePhaseController implements GameStateController {

    private int selectedPoint = -1;   // GUI’de seçili pulun bulunduğu nokta
    public int getSelectedPoint() { return selectedPoint; }
    public java.util.Set<Integer> getLegalTargets(GameContext ctx) {
    java.util.Set<Integer> targets = new java.util.HashSet<>();
    if (selectedPoint == -1) return targets;

    GameState   st      = ctx.state();
    Dice[]      dice    = st.getDice();
    boolean[]   used    = st.getDiceUsed();
    PlayerColor pc      = st.getCurrentTurn();
    int         dir     = pc.direction;

    for (int i = 0; i < used.length; i++) {
        if (used[i]) continue;
        int die = dice[i % 2].get();

        int to;
        if (selectedPoint == 0) {                
            to = (dir > 0) ? 25 - die : die;                    
        } else {
            to = selectedPoint + die * dir;

            // Evdeyken zar mesafesini aşma → bearing-off
            if (st.canBearOff(pc) &&
                ((dir > 0 && to > 24) || (dir < 0 && to < 1)))
                to = 25;
        }
        if (to == 25) {                                // bearing-off
            if (ctx.validator().isLegal(st, new Move(selectedPoint, 25), die))
                targets.add(25);
            continue;
        }
        if (to < 1 || to > 24) continue;
        if (ctx.validator().isLegal(st, new Move(selectedPoint, to), die))
            targets.add(to);
    }
    return targets;
}
    
    @Override
    public void enter(GameContext ctx) {
        ctx.state().rollDice();
        skipIfStuck(ctx);
    }

    @Override
    public void onPointSelected(GameContext ctx, int point) {
        GameState st = ctx.state();

        if (selectedPoint == -1) {
            // ► BAR (point==0) ◄
            if (point == 0) {
                if (st.checkersOnBar(st.getCurrentTurn()) == 0) return;
                selectedPoint = 0;
                return;
            }
            
            if (st.getPoint(point).isEmpty()) return;
            if (st.getPoint(point).peek().color() != st.getCurrentTurn()) return;
            selectedPoint = point;
            return;
        }

        // Hedef nokta
        int dieIdx = matchDie(st, selectedPoint, point);
        if (dieIdx == -1) { selectedPoint = -1; return; }   // uygun zar yok

        Move mv = new Move(selectedPoint, point);
        if (ctx.validator().isLegal(st, mv, st.getDice()[dieIdx % 2].get())) {
            ctx.applyMove(mv, dieIdx);
            // Oyun bitti mi?
            if (st.allBorneOff(st.getCurrentTurn())) {
                JOptionPane.showMessageDialog(null,
                    (st.getCurrentTurn() == PlayerColor.WHITE ? "Beyaz" : "Siyah") +
                    " 15 taşı da topladı — Oyun bitti!",
                    "Tebrikler", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);          // basit son; ileride yeniden başlatma ekranı ekleyebilirsiniz
            }
            if (st.allDiceUsed()) {
                ctx.endTurn();
                skipIfStuck(ctx);
            }else{
                skipIfStuck(ctx);
            }
        }
        selectedPoint = -1;
    }

    /**
     * from → to mesafesini hangi zar karşılıyor?
     */
    private int matchDie(GameState st, int from, int to) {
        Dice[] dice = st.getDice();        // iki fiziksel zar
        boolean[] used = st.getDiceUsed();    // duble'de 4 hamle takibi
        PlayerColor c = st.getCurrentTurn();
        int dir = c.direction;         // WHITE = +1, BLACK = -1

        for (int i = 0; i < used.length; i++) {
            if (used[i]) {
                continue;               // bu zar hakkı zaten tüketilmiş
            }
            int die = dice[i % 2].get();         // 0/1 → ilk zar, 2/3 → yine ilk zar (duble)

            /* -------- bar'dan giriş -------- */
            if (from == 0) {
                int entry = (c.direction > 0) ? die : 25 - die;
                if (entry == to) {
                    return i;
                }
                continue;                       // bu zar uymuyorsa diğer zarları dene
            }

            /* -------- normal hamle & bearing-off -------- */
            int expected = from + die * dir;     // yön hesabi
            if (expected < 1 || expected > 24) expected = 25;
            
            // bearing-off sırasında taş, zar mesafesini aşarak dışarı çıkıyorsa
            boolean overshootBearOff = st.canBearOff(c) && to == 25
                    && ((dir > 0 && expected > 24) || (dir < 0 && expected < 1));

            if (overshootBearOff || expected == to) {
                return i;
            }
        }
        return -1;                               // eşleşen zar bulunamadı
    }
    
    private boolean hasAnyLegalMove(GameContext ctx) {
        GameState     st = ctx.state();
        PlayerColor   pc = st.getCurrentTurn();
        Dice[]        dice = st.getDice();
        boolean[]     used = st.getDiceUsed();

        /* BAR’da taş varsa sadece giriş hamleleri denenir */
        if (st.checkersOnBar(pc) > 0) {
            for (int i = 0; i < used.length; i++) {
                if (used[i]) continue;
                int die = dice[i % 2].get();
                System.out.println("Die: " + die);
                int entry = pc.direction > 0 ? die : 25 - die;
                System.out.println("Entry: " + entry);
                if (ctx.validator().isLegal(st, new Move(0, entry), die))
                    return true;
            }
            System.out.println("FAAALSE");
            return false;   // bar’daki taş hiç giremiyor
        }

        /* Tahta üzerindeki tüm pullar için */
        for (int from = 1; from <= 24; from++) {
            Point p = st.getPoint(from);
            if (p.isEmpty() || p.peek().color() != pc) continue;
            for (int i = 0; i < used.length; i++) {
                if (used[i]) continue;
                int die = dice[i % 2].get();
                int to  = from + die * pc.direction;
                if (st.canBearOff(pc) &&
                    ((pc.direction > 0 && to > 24) || (pc.direction < 0 && to < 1)))
                    to = 25;                                   // bearing-off
                if (ctx.validator().isLegal(st, new Move(from, to), die))
                    return true;
            }
        }
        return false;          // hiçbir pul oynayamıyor
    }

    /* -----------  YENİ: Hamle yoksa otomatik pas ----------------------- */
    private void skipIfStuck(GameContext ctx) {
        while (!hasAnyLegalMove(ctx)) {               // art arda çift pas ihtimaline karşı döngü
            System.out.println("Skiped!");
            boolean[] used = ctx.state().getDiceUsed();
            for (int i = 0; i < used.length; i++) used[i] = true;   // kalan zarları tüket
            ctx.endTurn();                           // zar at + sıra değiş
            // Yeni oyuncu da sıkışık olabilir, kontrolü yinele
        }
    }
}
