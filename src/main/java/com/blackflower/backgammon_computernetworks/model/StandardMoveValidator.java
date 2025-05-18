package com.blackflower.backgammon_computernetworks.model;

/**
 *
 * @author emirs
 */
public final class StandardMoveValidator implements MoveValidator {

    @Override
    public boolean isLegal(GameState st, Move mv, int die) {
        PlayerColor c = st.getCurrentTurn();

        /* -- Bar’dan giriş zorunluluğu -- */
        if (st.checkersOnBar(c) > 0 && mv.from() != 0) return false;
        if (mv.from() == 0) {
            int entry = c == PlayerColor.WHITE ? 25 - die : die;
            return mv.to() == entry && canLand(st, c, mv.to());
        }

        /* -- Yön ve mesafe -- */
        int expectedTo = mv.from() + die * c.direction;
        if (mv.to() != expectedTo && !(st.canBearOff(c) && mv.to() == 25)) return false;

        return canLand(st, c, mv.to());
    }

    private boolean canLand(GameState st, PlayerColor c, int point) {
        if (point == 25) return true;   // bear-off
        Point dest = st.getPoint(point);
        return dest.size() < 2 || dest.peek().color() == c;
    }
}