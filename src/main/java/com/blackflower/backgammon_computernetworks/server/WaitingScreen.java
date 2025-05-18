package com.blackflower.backgammon_computernetworks.server;

/**
 *
 * @author emirs
 */
import com.blackflower.backgammon_computernetworks.model.PlayerColor;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * "Rakip bekleniyor..." ekranı.
 * 1. WELCOME geldiğinde renk atanır.
 * 2. START geldiğinde GameScreen açılır, kendisi dispose edilir.
 */
public final class WaitingScreen extends JFrame implements Consumer<LegacyMessage> {

    /* ---------- UI ---------- */
    private final JLabel info = new JLabel("Rakip bekleniyor...", SwingConstants.CENTER);

    /* ---------- Ağ ---------- */
    private final ClientNetwork net;
    private PlayerColor myColor;

    public WaitingScreen(ClientNetwork net) {
        this.net = net;

        setTitle("Backgammon – Bağlandı");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(300, 120);
        setLayout(new BorderLayout());
        add(info, BorderLayout.CENTER);
        setLocationRelativeTo(null);
        setVisible(true);

        /* Ağ dinleyicisini kendisine yönlendirir ---------
         *  StartScreen, ClientNetwork'u şöyle yaratmalıdır:
         *
         *    AtomicReference<WaitingScreen> ref = new AtomicReference<>();
         *    ClientNetwork net = new ClientNetwork(ip, port,
         *         msg -> SwingUtilities.invokeLater(() -> ref.get().accept(msg)));
         *    ref.set(new WaitingScreen(net));
         *
         *  Böylece onMessage çağrıları WaitingScreen'e düşer.
         *  (StartScreen içindeki önceki handleMsg artık gereksiz.)
         */
    }

    /* ---------- LegacyMessage Consumer ---------- */
    @Override
    public void accept(LegacyMessage m) {
        switch (m.type()) {
            case "WELCOME" -> {
                myColor = PlayerColor.valueOf(m.get("playerColor"));
                info.setText("Renginiz: " + myColor + "  –  Rakip bekleniyor…");
            }
            case "START" -> {
                // Oyun ekranını aç
                GameScreen gs = new GameScreen(net, myColor);

                // Başlangıç durumunu uygula
                StateCodec.apply(m.get("state"), gs.state);     // GameScreen.state public olmalı
                gs.board.repaint();                             // BoardPanel erişimi de public

                gs.onMessage(m);                                // zar + currentPlayer ayarları
                dispose();                                      // WaitingScreen kapat
            }
            case "ERROR" -> {
                JOptionPane.showMessageDialog(this, m.get("message"),
                        "Sunucu Hatası", JOptionPane.ERROR_MESSAGE);
                try { net.close(); } catch (Exception ignored) {}
                System.exit(0);
            }
        }
    }
}
