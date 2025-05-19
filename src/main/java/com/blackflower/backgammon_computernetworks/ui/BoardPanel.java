package com.blackflower.backgammon_computernetworks.ui;

/**
 *
 * @author emirs
 */
import com.blackflower.backgammon_computernetworks.controller.GameContext;
import com.blackflower.backgammon_computernetworks.controller.MovePhaseController;
import com.blackflower.backgammon_computernetworks.model.Checker;
import com.blackflower.backgammon_computernetworks.model.Dice;
import com.blackflower.backgammon_computernetworks.model.GameState;
import com.blackflower.backgammon_computernetworks.model.PlayerColor;
import com.blackflower.backgammon_computernetworks.server.OnlineMoveController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class BoardPanel extends JPanel {

    private static final int TRI_W   = 60;
    private static final int TRI_H   = 250;
    private static final int BAR_W   = 40;
    private static final int PADDING = 20;
    private static final int CHECKER_R = 25;
    private static final Color HIGHLIGHT = new Color(255, 215, 0, 160); // altın yarı saydam
    private static final Color TARGET_DOT = new Color(50, 220, 255, 180);
    private final GameContext ctx;
    private static final int BEAR_W = TRI_W * 1;   // üçgen kadar geniş

    private static final Rectangle BEAR_OFF_WHITE
            = new Rectangle(PADDING + TRI_W * 12 + BAR_W,
                    PADDING,
                    BEAR_W, TRI_H);

    private static final Rectangle BEAR_OFF_BLACK
            = new Rectangle(BEAR_OFF_WHITE.x,
                    PADDING + TRI_H,
                    BEAR_W, TRI_H);

    public BoardPanel(GameContext ctx) {
        this.ctx = ctx;
        int width  = (TRI_W * 6) * 2 + BAR_W + PADDING * 2;
        int height = TRI_H * 2 + PADDING * 2;
        setPreferredSize(new Dimension(width, height));
        addMouseListener(new BoardMouse());
    }

    /* ---------------- Swing painting ------------------------------------ */

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        drawBoardBackground(g2);
        drawCheckers(g2);
        drawDice(g2);

        drawSelection(g2);
        drawPossibleTargets(g2);
        drawBearOffCounters(g2);
        
        g2.dispose();
    }

    private void drawBoardBackground(Graphics2D g) {
        g.setColor(new Color(181, 136, 99));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Üçgenler
        for (int i = 0; i < 24; i++) {
            int col = (i / 6) % 2 == 0 ? i % 2 : (i + 1) % 2;
            g.setColor(col == 0 ? Color.DARK_GRAY : Color.LIGHT_GRAY);
            Polygon p = trianglePolygon(i);
            g.fillPolygon(p);
        }

        // Bar
        g.setColor(new Color(90, 60, 40));
        int barX = PADDING + TRI_W * 6;
        g.fillRect(barX, PADDING, BAR_W, TRI_H * 2);

        // Bear-off sütun zemini
        g.setColor(new Color(120, 70, 40));
        g.fillRect(BEAR_OFF_WHITE.x, BEAR_OFF_WHITE.y,
                BEAR_W, TRI_H * 2);

        // Küçük etiket
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("OFF", BEAR_OFF_WHITE.x + 6, BEAR_OFF_WHITE.y + 16);

    }

    private Polygon trianglePolygon(int pointIdx) {
        int xIndex = pointIdx < 12 ? 11 - pointIdx : pointIdx - 12;
        boolean bottom = pointIdx < 12;
        int x = PADDING + (xIndex % 6) * TRI_W + (xIndex >= 6 ? BAR_W + TRI_W * 6 : 0);
        int baseY = bottom ? PADDING + TRI_H * 2 : PADDING;
        int tipY  = bottom ? baseY - TRI_H : baseY + TRI_H;

        Polygon p = new Polygon();
        p.addPoint(x, baseY);
        p.addPoint(x + TRI_W, baseY);
        p.addPoint(x + TRI_W / 2, tipY);
        return p;
    }

    private void drawCheckers(Graphics2D g) {
        GameState st = ctx.state();
        for (int idx = 1; idx <= 24; idx++) {
            com.blackflower.backgammon_computernetworks.model.Point pt = st.getPoint(idx);
            int count = pt.size();
            if (count == 0) continue;

            int xTile = (idx <= 12 ? 12 - idx : idx - 13);
            int colX  = PADDING + (xTile % 6) * TRI_W +
                        (xTile >= 6 ? BAR_W + TRI_W * 6 : 0) + TRI_W / 2 - CHECKER_R;
            boolean bottom = idx <= 12;
            int startY = bottom ? (PADDING + TRI_H * 2 - CHECKER_R * 2) : PADDING;

            int dy = bottom ? -CHECKER_R * 2 : CHECKER_R * 2;
            int y = startY;
            for (int i = 0; i < count; i++) {
                Checker ch = pt.peek();      // renk için üstü yeterli
                g.setColor(ch.color() == PlayerColor.WHITE ? Color.WHITE : Color.BLACK);
                g.fillOval(colX, y, CHECKER_R * 2, CHECKER_R * 2);
                g.setColor(Color.GRAY);
                g.drawOval(colX, y, CHECKER_R * 2, CHECKER_R * 2);
                y += dy;
            }
        }
        // Bar’daki pullar; burada üst üste birer daire çiziyoruz
        int barX = PADDING + TRI_W * 6 + BAR_W / 2 - CHECKER_R;
        int yWhite = PADDING + TRI_H - CHECKER_R * 2;
        for (int i = 0; i < st.checkersOnBar(PlayerColor.WHITE); i++, yWhite -= CHECKER_R * 2) {
            g.setColor(Color.WHITE); g.fillOval(barX, yWhite, CHECKER_R * 2, CHECKER_R * 2);
            g.setColor(Color.GRAY);  g.drawOval(barX, yWhite, CHECKER_R * 2, CHECKER_R * 2);
        }
        int yBlack = PADDING + TRI_H;
        for (int i = 0; i < st.checkersOnBar(PlayerColor.BLACK); i++, yBlack += CHECKER_R * 2) {
            g.setColor(Color.BLACK); g.fillOval(barX, yBlack, CHECKER_R * 2, CHECKER_R * 2);
            g.setColor(Color.GRAY);  g.drawOval(barX, yBlack, CHECKER_R * 2, CHECKER_R * 2);
        }
    }

    private void drawDice(Graphics2D g) {
        Dice[] dice = ctx.state().getDice();
        int x = getWidth() / 2 - 30;
        int y = getHeight() / 2 - 30;
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, 60, 30, 8, 8);
        g.fillRoundRect(x, y + 35, 60, 30, 8, 8);
        g.setColor(Color.BLACK);
        g.drawString(dice[0].toString(), x + 25, y + 22);
        g.drawString(dice[1].toString(), x + 25, y + 57);
    }

    /* ---------------- Mouse --------------------------------------------- */

    private final class BoardMouse extends MouseAdapter {
        @Override public void mousePressed(MouseEvent e) {
            int point = screenToPoint(e.getX(), e.getY());
            if (point != -1) {
                ctx.getStateController().onPointSelected(ctx, point);
                repaint();
            }
        }
    }

    /** Ekran koordinatını 1-24 noktasına dönüştürür; bar ve boşluk -1. */
    private int screenToPoint(int x, int y) {
        int relX = x - PADDING;
        int relY = y - PADDING;
        int barLeft = TRI_W * 6;
        
        if (relX < 0 || relY < 0 || relX > TRI_W * 12 + BAR_W || relY > TRI_H * 2) return -1;
        // ► BAR ◄
        if (relX >= barLeft && relX <= barLeft + BAR_W) return 0;
   
        /* bear-off bölgeleri */
        int bearLeft = PADDING + TRI_W * 12 + BAR_W;
        if (x >= bearLeft && x <= bearLeft + BEAR_W) {
            return 25;
        }

        boolean bottom = relY > TRI_H;
        int col = relX < TRI_W * 6 ? (relX / TRI_W) : ((relX - BAR_W) / TRI_W);
        int idx = bottom ? 12 - col : 13 + col;
        return idx;
    }
    
    private void drawSelection(Graphics2D g) {
        OnlineMoveController mp = ctx.asOnline(); 
        if (mp == null) return;
        int sel = mp.getSelectedPoint();
        if (sel == -1 || sel == 25) return;
        if (sel == 0) {                                        // BAR
            Rectangle r = checkerBoundsBar(ctx.state().getCurrentTurn());
            g.setStroke(new BasicStroke(4));
            g.setColor(HIGHLIGHT);
            g.drawOval(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
            return;
        }

        com.blackflower.backgammon_computernetworks.model.Point p = ctx.state().getPoint(sel);
        if (p.isEmpty()) return;
        
        // seçili pulun ekran koordinatı (üstteki)
        Rectangle r = checkerBounds(sel, p.size() - 1);
        g.setStroke(new BasicStroke(4));
        g.setColor(HIGHLIGHT);
        g.drawOval(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
    }
    
    private void drawBearOffCounters(Graphics2D g) {
        GameState st = ctx.state();
        g.setFont(new Font("SansSerif", Font.BOLD, 16));

        // Beyaz (sağ üst)
        String w = "♔ " + st.borneOff(PlayerColor.WHITE);
        int wX = getWidth() - PADDING - g.getFontMetrics().stringWidth(w);
        int wY = PADDING + 16;
        g.setColor(Color.WHITE);
        g.drawString(w, wX, wY);

        // Siyah (sol alt)
        String b = "♚ " + st.borneOff(PlayerColor.BLACK);
        int bX = PADDING;
        int bY = getHeight() - PADDING;
        g.setColor(Color.BLACK);
        g.drawString(b, bX, bY);
    }
    
    private void drawPossibleTargets(Graphics2D g) {
        OnlineMoveController mp = ctx.asOnline(); 
        if (mp == null) return;
        for (int tgt : mp.getLegalTargets()) {
            if (tgt == 25) {
                continue; // bear-off işareti şimdilik yok
            }
            if (tgt == 25) {
                Rectangle b = (mp.myColor == PlayerColor.WHITE)
                        ? BEAR_OFF_WHITE : BEAR_OFF_BLACK;
                int cx = b.x + b.width / 2, cy = b.y + b.height / 2;
                g.setColor(TARGET_DOT);
                g.fillOval(cx - 14, cy - 14, 28, 28);   // büyük hedef
                continue;
            }
            Rectangle r = checkerBounds(tgt, ctx.state().getPoint(tgt).size());
            int cx = r.x + r.width / 2 - 6;
            int cy = r.y + r.height / 2 - 6;
            g.setColor(TARGET_DOT);
            g.fillOval(cx, cy, 12, 12);
        }
    }
    
    /** Seçilen nokta & seviye için pulun “piksel” koordinatlarını verir. */
    private Rectangle checkerBounds(int pointIdx, int stackIndex) {
        if (pointIdx == 0) return checkerBoundsBar(ctx.state().getCurrentTurn());
        int xTile = (pointIdx <= 12 ? 12 - pointIdx : pointIdx - 13);
        int colX  = PADDING + (xTile % 6) * TRI_W +
                    (xTile >= 6 ? BAR_W + TRI_W * 6 : 0) + TRI_W / 2 - CHECKER_R;
        boolean bottom = pointIdx <= 12;
        int startY = bottom ? (PADDING + TRI_H * 2 - CHECKER_R * 2)
                            : PADDING;
        int dy = bottom ? -CHECKER_R * 2 : CHECKER_R * 2;
        int y = startY + dy * stackIndex;
        return new Rectangle(colX, y, CHECKER_R * 2, CHECKER_R * 2);
    }
    
    private Rectangle checkerBoundsBar(PlayerColor color) {
        int barX = PADDING + TRI_W * 6 + BAR_W / 2 - CHECKER_R;
        int idx  = ctx.state().checkersOnBar(color) - 1;
        if (idx < 0) idx = 0;
        int y = color == PlayerColor.WHITE
                ? PADDING + TRI_H - CHECKER_R * 2 - idx * CHECKER_R * 2   // üst
                : PADDING + TRI_H + idx * CHECKER_R * 2;                  // alt
        return new Rectangle(barX, y, CHECKER_R * 2, CHECKER_R * 2);
    }
}