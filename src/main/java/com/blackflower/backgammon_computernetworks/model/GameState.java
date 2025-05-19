package com.blackflower.backgammon_computernetworks.model;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author emirs
 */
public final class GameState {

    private final Map<Integer, Point> board = new HashMap<>();
    private final Dice[] dice = {new Dice(), new Dice()};
    private boolean[] diceUsed = new boolean[4];   // duble durumunda 4 hamle
    private PlayerColor currentTurn;
    private int barWhite = 0, barBlack = 0;
    
    private int borneOffWhite = 0, borneOffBlack = 0;     // topladıklarımız

    /* ---------- Bearing-off sayaçları ------------------------------- */
    public void bearOff(Checker ch) {
        if (ch.color() == PlayerColor.WHITE) borneOffWhite++;
        else borneOffBlack++;
    }
    public int borneOff(PlayerColor c) {
        return c == PlayerColor.WHITE ? borneOffWhite : borneOffBlack;
    }
    public boolean allBorneOff(PlayerColor c) {           // 15 pulu topladı mı?
        return borneOff(c) >= 15;
    }

    public GameState() {
        // 0 = bar, 1-24 = gerçek noktalar, 25 = bear-off
        for (int i = 0; i <= 25; i++) {
            board.put(i, new Point(i));
        }
    }

    /* ---------- Setup --------------------------------------------------- */
    public void placeCheckers(int point, int count, PlayerColor color) {
        Point p = board.get(point);
        for (int i = 0; i < count; i++) {
            p.push(new Checker(color));
        }
    }

    /* ---------- Turn management ----------------------------------------- */
    public void rollDice() {
        dice[0].roll();
        dice[1].roll();
        // duble ise 4 hamle hakkı
        diceUsed = new boolean[ isDouble() ? 4 : 2 ];
    }

    public boolean isDouble() {
        return dice[0].get() == dice[1].get();
    }

    public Dice[] getDice() {
        return dice;
    }

    public boolean[] getDiceUsed() {
        return diceUsed;
    }
    
    public void setDiceUsed(boolean[] newArr) {
        this.diceUsed = newArr;
    }

    public void markDieUsed(int dieVal) {
        System.out.println("Dice length: " + diceUsed.length);
        for (int i = 0; i < diceUsed.length; i++) {
            if (!diceUsed[i] && dice[i % 2].get() == dieVal) {
                diceUsed[i] = true;
                System.out.println("Used Dice: " + dice[i % 2].get());
                if (isDouble()) break;
            }
        }
    }

    public boolean allDiceUsed() {
        for (boolean d : diceUsed) {
            if (!d) {
                return false;
            }
        }
        return true;
    }

    /* ---------- Checker access ------------------------------------------ */
    public Point getPoint(int idx) {
        return board.get(idx);
    }

    public int checkersOnBar(PlayerColor c) {
        return c == PlayerColor.WHITE ? barWhite : barBlack;
    }

    public void sendToBar(Checker ch) {
        if (ch.color() == PlayerColor.WHITE) {
            barWhite++;
        } else {
            barBlack++;
        }
    }

    public void enterFromBar(PlayerColor c) {
        if (c == PlayerColor.WHITE) {
            barWhite--;
        } else {
            barBlack--;
        }
    }

    /* ---------- Moves & bearing off ------------------------------------- */
    public void moveChecker(int from, int to) {
        Checker ch;
        if (from == 0) {             // bar
            PlayerColor c = currentTurn;
            enterFromBar(c);
            ch = new Checker(c);
        } else {
            ch = board.get(from).pop();
        }

        if (to == 25) {              // bear-off
            bearOff(ch);
            return;                  // checker oyundan çıktı
        }

        Point dest = board.get(to);
        if (dest.size() == 1 && dest.peek().color() != ch.color()) {
            // vurma
            Checker hit = dest.pop();
            sendToBar(hit);
        }
        dest.push(ch);
    }

    public boolean canBearOff(PlayerColor c) {
        // Bar’da pulu varsa toplama yapamaz
        if (checkersOnBar(c) > 0) {
            return false;
        }

        for (int idx = 1; idx <= 24; idx++) {
            Point p = board.get(idx);
            if (p.isEmpty() || p.peek().color() != c) {
                continue;
            }

            // WHITE için ev 19-24, BLACK için 1-6
            if (c.direction > 0) {          // WHITE
                if (idx < 19) {
                    return false; // ev dışı
                }
            } else {                        // BLACK
                if (idx > 6) {
                    return false; // ev dışı
                }
            }
        }
        return true;    // tüm pullar evde
    }
    
    public void resetBorneOff() {
        borneOffWhite = borneOffBlack = 0;
    }

    /* ---------- Getters / setters --------------------------------------- */
    public PlayerColor getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(PlayerColor c) {
        currentTurn = c;
    }
}
