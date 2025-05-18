package com.blackflower.backgammon_computernetworks.server;

import com.blackflower.backgammon_computernetworks.controller.GameContext;
import com.blackflower.backgammon_computernetworks.model.*;
import com.blackflower.backgammon_computernetworks.ui.BoardPanel;
import javax.swing.*;
import java.awt.*;
/**
 *
 * @author emirs
 */
public final class GameScreen extends JFrame {

    public final GameState state;
    private final ClientNetwork net;
    public final BoardPanel board;

    public GameScreen(ClientNetwork net, PlayerColor myColor){
        this.net = net;
        this.state = new GameState();          // boş başlatılacak
        GameContext ctx = new GameContext(state,new StandardMoveValidator());
        ctx.setStateController(new OnlineMoveController(net,state));

        board = new BoardPanel(ctx);

        setTitle("Backgammon – "+myColor);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        add(board, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /* ---------------- Mesaj İşle ---------------- */
    public void onMessage(LegacyMessage m){
        switch(m.type()){
            case "STATE_UPDATE" -> {
                StateCodec.apply(m.get("state"), state);
                board.repaint();
            }
            case "GAME_OVER" -> {
                JOptionPane.showMessageDialog(this,
                        "Kazanan: "+m.get("winnerColor"));
            }
            case "ILLEGAL_MOVE" -> JOptionPane.showMessageDialog(this,
                        "Geçersiz hamle: "+m.get("reason"));
            case "ERROR" -> JOptionPane.showMessageDialog(this, m.get("message"));
        }
    }
}