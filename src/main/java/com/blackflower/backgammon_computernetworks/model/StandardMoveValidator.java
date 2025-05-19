package com.blackflower.backgammon_computernetworks.model;

/**
 *
 * @author emirs
 */
public final class StandardMoveValidator implements MoveValidator {


    
    @Override
    public boolean isLegal(GameState st, Move mv, int die) {
        PlayerColor c = st.getCurrentTurn();
        System.out.println("Color: " + c.toString());

        if (st.checkersOnBar(c) > 0 && mv.from() != 0)
            return false;

        if (mv.from() == 0) {
            int entry = (c.direction > 0) ? die : 25 - die;
            return mv.to() == entry && canLand(st, c, entry);
        }

        int expected = mv.from() + die * c.direction;

        /* 2-a) BEARING-OFF BLOĞU ------------------------- */
        if (st.canBearOff(c)) {
            // Hamle doğrudan dışarı toplama (to == 25)
            if (mv.to() == 25) {
                // A) Tam zar → expected tam 25 olmalı
                if (expected == 25) return true;

                boolean hasCheckerFurther = false;
                int step = c.direction;                 // +1 ya da -1
                for (int p = mv.from() - step;
                     (step > 0 ? p >= c.homeStart : p <= c.homeStart);
                     p -= step)
                {
                    Point pt = st.getPoint(p);
                    if (!pt.isEmpty() && pt.peek().color() == c) {
                        hasCheckerFurther = true;
                        break;
                    }
                }
                if (!hasCheckerFurther &&                // arkada pul yok
                    ((c.direction > 0 && expected > 25) ||
                     (c.direction < 0 && expected < 25)))
                    return true;                         // büyük zarla topla

                return false;
            }
        }
        /* 2-b) NORMAL KARAYA İNİŞ ----------------------- */
        System.out.println("Expected: " + expected + " To: " + mv.to());
        if (mv.to() != expected) return false;           // yanlış mesafe
        return canLand(st, c, mv.to());
    }

    private boolean canLand(GameState st, PlayerColor c, int point) {
        // bearing-off
        if (point == 25) {
            return true;
        }

        // tahta dışı veya bar (0) => inilemez
        if (point < 1 || point > 24) {
            return false;
        }

        Point dest = st.getPoint(point);          // artık null olamaz
        return dest.size() < 2 || dest.peek().color() == c;
    }
}