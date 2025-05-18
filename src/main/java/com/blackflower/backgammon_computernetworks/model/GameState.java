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
        diceUsed = new boolean[isDouble() ? 4 : 2];
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

    public void markDieUsed(int dieIndex) {
        diceUsed[dieIndex] = true;
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
        int start = c.homeStart;
        int end = start + 5 * c.direction;
        for (int i = start; c.direction > 0 ? i <= end : i >= end; i += c.direction) {
            if (board.get(i).size() > 0 && board.get(i).peek().color() != c) {
                return false;
            }
        }
        // evde olmayan pul var mı?
        int oppositeDir = c.direction > 0 ? -1 : 1;
        for (int i = start + 6 * oppositeDir; (oppositeDir > 0 ? i <= 24 : i >= 1); i += oppositeDir) {
            if (board.get(i).size() > 0 && board.get(i).peek().color() == c) {
                return false;
            }
        }
        return true;
    }

    /* ---------- Getters / setters --------------------------------------- */
    public PlayerColor getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(PlayerColor c) {
        currentTurn = c;
    }
}
