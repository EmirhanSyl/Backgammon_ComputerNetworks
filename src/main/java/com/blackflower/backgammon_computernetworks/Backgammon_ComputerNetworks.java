package com.blackflower.backgammon_computernetworks;

import com.blackflower.backgammon_computernetworks.controller.OpeningRollController;
import com.blackflower.backgammon_computernetworks.model.GameState;
import com.blackflower.backgammon_computernetworks.model.StandardMoveValidator;
import com.blackflower.backgammon_computernetworks.controller.GameContext;
import com.blackflower.backgammon_computernetworks.ui.BoardPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 *
 * @author emirs
 */
public class Backgammon_ComputerNetworks {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameState state = GameInitializer.defaultSetup();
            GameContext context = new GameContext(state, new StandardMoveValidator());
            context.setStateController(new OpeningRollController());

            JFrame frame = new JFrame("Backgammon");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new BoardPanel(context));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            context.getStateController().enter(context);
        });
    }
}
