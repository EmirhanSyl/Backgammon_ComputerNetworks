package com.blackflower.backgammon_computernetworks.server;

import com.blackflower.backgammon_computernetworks.GameInitializer;
import com.blackflower.backgammon_computernetworks.model.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author emirs
 */
public final class BackgammonServer {

    private static final int PORT = 9000;

    /* ------------- Oda ----------------- */
    private final List<ClientHandler> players = new CopyOnWriteArrayList<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    private volatile GameState state;
    private volatile PlayerColor currentTurn;
    private final MoveValidator validator = new StandardMoveValidator();

    /* ------------- main ---------------- */
    public static void main(String[] args) throws IOException {
        new BackgammonServer().start();
    }

    /* ------------- Sunucu Döngüsü ------ */
    private void start() throws IOException {
        try (ServerSocket ss = new ServerSocket(PORT)) {
            System.out.println("Legacy Tavla sunucusu port " + PORT + " üzerinde çalışıyor.");
            while (true) {
                Socket s = ss.accept();
                if (players.size() >= 2) {               // oda dolu
                    writeLine(s, new LegacyMessage("ERROR")
                                    .put("message", "Room is full").encode());
                    s.close();
                    continue;
                }
                ClientHandler ch = new ClientHandler(s);
                players.add(ch);
                pool.submit(ch);
            }
        }
    }

    /* ------------- Oyun Başlat --------- */
    private synchronized void beginGame() {
        state = GameInitializer.defaultSetup();
        currentTurn = PlayerColor.WHITE;
        state.setCurrentTurn(currentTurn);
        players.get(0).color = PlayerColor.WHITE;
        players.get(1).color = PlayerColor.BLACK;

        players.get(0).send(new LegacyMessage("WELCOME").put("playerColor", "WHITE"));
        players.get(1).send(new LegacyMessage("WELCOME").put("playerColor", "BLACK"));

        state.rollDice();
        /* YENİ – hamle yoksa anında pasla (en çok 2 defa: çift pas ihtimali) */
        for (int i = 0; i < 2 && !hasAnyLegalMove(); i++) {
            endTurnAndRoll();
        }
        broadcast(new LegacyMessage("START")
                .put("state", encodeState(state))
                .put("dice", diceString())
                .put("currentPlayer", currentTurn));
    }

    /* ------------- Hamle İşle ---------- */
    private synchronized void handleMove(ClientHandler from,
            int src, int dst, int dieVal) {

        /* sıra kontrolü */
        if (from.color != currentTurn) {
            from.send(new LegacyMessage("ILLEGAL_MOVE")
                    .put("reason", "Not your turn"));
            return;
        }

        Move mv = new Move(src, dst);

        /* hamleyi doğrula */
        if (!validator.isLegal(state, mv, dieVal)) {
            from.send(new LegacyMessage("ILLEGAL_MOVE")
                    .put("reason", "Illegal by rules"));
            return;
        }

        /* hamleyi uygula */
        state.moveChecker(src, dst);
        state.markDieUsed(dieVal);                 // <-- yalnız değerle işaretle

        /* sıra değişimi + yeni zar */
        if (state.allDiceUsed()) {
            currentTurn = (currentTurn == PlayerColor.WHITE)
                    ? PlayerColor.BLACK : PlayerColor.WHITE;
            state.setCurrentTurn(currentTurn);
            state.rollDice();
        }

        /* oyun bitti mi? */
        if (state.allBorneOff(from.color)) {
            broadcast(new LegacyMessage("STATE_UPDATE")
                    .put("state", encodeState(state))
                    .put("dice", diceString())
                    .put("currentPlayer", currentTurn));
            broadcast(new LegacyMessage("GAME_OVER")
                    .put("winnerColor", from.color));
            resetRoom();
            return;
        }

        /* normal durum */
        broadcast(new LegacyMessage("STATE_UPDATE")
                .put("state", encodeState(state))
                .put("dice", diceString())
                .put("currentPlayer", currentTurn));
    }

    /* ------------- Yardımcılar --------- */
    private String diceString() {
        return state.getDice()[0].get() + "," + state.getDice()[1].get();
    }
    
    private boolean hasAnyLegalMove() {
        return new StandardMoveValidator().playerHasMove(state);
    }

    private void endTurnAndRoll() {
        currentTurn = (currentTurn == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
        state.setCurrentTurn(currentTurn);
        state.rollDice();
    }

    /**
     * Basit, tek satırlık tahta kodlaması: her point "idx:W3/B2" vs. boş ise
     * atlanır.
     */
    private String encodeState(GameState st) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 24; i++) {
            Point p = st.getPoint(i);
            if (p.isEmpty()) {
                continue;
            }
            sb.append(i).append(':')
                    .append(p.peek().color() == PlayerColor.WHITE ? 'W' : 'B')
                    .append(p.size()).append(';');
        }
        sb.append("barW=").append(st.checkersOnBar(PlayerColor.WHITE)).append(';');
        sb.append("barB=").append(st.checkersOnBar(PlayerColor.BLACK)).append(';');
        sb.append("offW=").append(st.borneOff(PlayerColor.WHITE)).append(';');
        sb.append("offB=").append(st.borneOff(PlayerColor.BLACK)).append(';');
        sb.append("diceUsed=").append(usedMask(state.getDiceUsed())).append(';');
        sb.append("turn=").append(currentTurn.name());
        return sb.toString();
    }

    private String usedMask(boolean[] arr) {
        StringBuilder b = new StringBuilder();
        for (boolean u : arr) {
            b.append(u ? '1' : '0');
        }
        return b.toString();
    }

    /**
     * Ters işlem (isteyen istemci tarafında yapsın).
     */

    private void broadcast(LegacyMessage m) {
        System.out.println("→ SEND to ALL : " + m);   // DEBUG
        players.forEach(p -> p.send(m));
    }

    private static void writeLine(Socket s, String line) throws IOException {
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {
            pw.println(line);
        }
    }

    private synchronized void resetRoom() { beginGame(); }

    /* ------------- İç Sınıf ------------ */
    private final class ClientHandler implements Runnable {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private PlayerColor color;

        ClientHandler(Socket s) throws IOException {
            socket = s;
            in  = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(
                    new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        @Override public void run() {
            try {
                // HELLO bekle
                LegacyMessage hello = LegacyMessage.decode(in.readLine());
                if (!"HELLO".equals(hello.type())) {
                    send(new LegacyMessage("ERROR").put("message","Expected HELLO"));
                    socket.close(); return;
                }
                if (players.size() == 2) beginGame();        // oda dolunca başlat

                String line;
                while ((line = in.readLine()) != null) {
                    LegacyMessage m = LegacyMessage.decode(line);
                    System.out.println("Message Recieved From Color:" + color.toString() + " Message: " + m.toString());
                    switch (m.type()) {
                        case "MOVE" -> handleMove(this,
                                m.getInt("from"), m.getInt("to"), m.getInt("die"));
                        case "PING" -> send(new LegacyMessage("PONG"));
                        default     -> send(new LegacyMessage("ERROR")
                                            .put("message", "Unknown type " + m.type()));
                    }
                }
            } catch (IOException e) {
                System.out.println("Bağlantı koptu: " + e.getMessage());
            } finally {
                players.remove(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
        void send(LegacyMessage m) { out.println(m.encode()); }
    }
}