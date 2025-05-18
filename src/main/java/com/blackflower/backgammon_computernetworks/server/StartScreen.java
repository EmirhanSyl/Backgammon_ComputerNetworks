package com.blackflower.backgammon_computernetworks.server;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author emirs
 */

public final class StartScreen extends JFrame {

    private final JTextField ipField   = new JTextField("3.122.xx.xx");
    private final JTextField portField = new JTextField("9000");
    private final JTextField nickField = new JTextField("Player");

    public StartScreen() {
        setTitle("Backgammon – Sunucuya Bağlan");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(320, 200);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 2, 8, 8));

        /* --- Form --- */
        add(new JLabel("AWS IP:", SwingConstants.RIGHT));   add(ipField);
        add(new JLabel("Port:",   SwingConstants.RIGHT));   add(portField);
        add(new JLabel("Nick:",   SwingConstants.RIGHT));   add(nickField);

        JButton connectBtn = new JButton("Connect");
        add(new JLabel());                // boş hücre
        add(connectBtn);

        /* -------------------------------------------------
         *  ActionListener – TAM OLARAK BURADA KULLANIYORUZ
         * ------------------------------------------------- */
        connectBtn.addActionListener(e -> {
            try {
                /* 1) WaitingScreen referansı (sonradan mesaj yönlendirmek için) */
                AtomicReference<WaitingScreen> waitRef = new AtomicReference<>();

                /* 2) Ağ katmanını başlat – her mesajı EDT’ye taşımak için
                      SwingUtilities.invokeLater(_) kullandık                 */
                ClientNetwork net = new ClientNetwork(
                        ipField.getText(),
                        Integer.parseInt(portField.getText()),
                        msg -> SwingUtilities.invokeLater(() -> {
                            WaitingScreen ws = waitRef.get();
                            if (ws != null) ws.accept(msg);   // mesajı WaitingScreen’e ilet
                        }));

                /* 3) Sunucuya HELLO gönder */
                net.send(new LegacyMessage("HELLO")
                             .put("nickname", nickField.getText()));

                /* 4) WaitingScreen’i aç ve referansı sakla */
                WaitingScreen ws = new WaitingScreen(net);
                waitRef.set(ws);

                /* 5) Bu pencereyi gizle (isterseniz dispose edebilirsiniz) */
                setVisible(false);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Bağlanılamadı: " + ex.getMessage(),
                        "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });

        setVisible(true);
    }
}