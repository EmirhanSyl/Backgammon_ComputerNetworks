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

    final GameState state = new GameState();
    private final ClientNetwork net;
    final BoardPanel board;

    public GameScreen(ClientNetwork net, PlayerColor myColor){
        this.net = net;
        GameContext ctx = new GameContext(state,new StandardMoveValidator());
        ctx.setStateController(new OnlineMoveController(net,state, myColor));

        board = new BoardPanel(ctx);

        setTitle("Backgammon – "+myColor);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        add(board, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /* -------------- Sunucudan gelen her mesaj -------------- */
    public void onMessage(LegacyMessage m){
        System.out.println("Message Recieved: " + m.toString());
        switch(m.type()){
            case "START","STATE_UPDATE" -> {
                if(m.has("state"))
                    StateCodec.apply(m.get("state"), state);

                if (m.has("dice")) {                  // sadece zar değerini ayarla
                    String[] d = m.get("dice").split(",");
                    state.getDice()[0].set(Integer.parseInt(d[0]));
                    state.getDice()[1].set(Integer.parseInt(d[1]));
                }
                board.repaint();
            }
            case "ILLEGAL_MOVE" -> JOptionPane.showMessageDialog(this,
                    "Geçersiz hamle: "+m.get("reason"));
            case "GAME_OVER" -> JOptionPane.showMessageDialog(this,
                    "Kazanan: "+m.get("winnerColor"));
            case "ERROR" -> JOptionPane.showMessageDialog(this,
                    m.get("message"),"Sunucu",JOptionPane.ERROR_MESSAGE);
        }
    }
}