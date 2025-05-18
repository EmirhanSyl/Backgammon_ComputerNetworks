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
        broadcast(new LegacyMessage("START")
                .put("state", encodeState(state))
                .put("dice", diceString())
                .put("currentPlayer", currentTurn));
    }

    /* ------------- Hamle İşle ---------- */
    private synchronized void handleMove(ClientHandler from,
            int src, int dst, int dieIdx) {

        /* 0) sıra kontrolü */
        if (from.color != currentTurn) {
            from.send(new LegacyMessage("ILLEGAL_MOVE")
                    .put("reason", "Not your turn"));
            return;
        }

        /* 1) hamleyi doğrula */
        int dieVal = state.getDice()[dieIdx % 2].get();
        Move mv = new Move(src, dst);
        if (!validator.isLegal(state, mv, dieVal)) {
            from.send(new LegacyMessage("ILLEGAL_MOVE")
                    .put("reason", "Illegal by rules"));
            return;
        }

        /* 2) hamleyi uygula */
        state.moveChecker(src, dst);
        state.getDiceUsed()[dieIdx] = true;           // << doğru indisi işaretle

        /* 3) sıra değişimi + zar at */
        if (state.allDiceUsed()) {
            currentTurn = (currentTurn == PlayerColor.WHITE)
                    ? PlayerColor.BLACK : PlayerColor.WHITE;
            state.setCurrentTurn(currentTurn);        // modelle senkron
            state.rollDice();
        }

        /* 4) oyun bitti mi? */
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

        /* 5) normal durum – herkese güncel tahta */
        broadcast(new LegacyMessage("STATE_UPDATE")
                .put("state", encodeState(state))
                .put("dice", diceString())
                .put("currentPlayer", currentTurn));
    }

    /* ------------- Yardımcılar --------- */
    private void markDieUsed(int die) {
        boolean[] used = state.getDiceUsed();
        for (int i = 0; i < used.length; i++) {
            if (!used[i] && state.getDice()[i % 2].get() == die) {
                used[i] = true; break;
            }
        }
    }
    private String diceString() {
        return state.getDice()[0].get() + "," + state.getDice()[1].get();
    }
    /** Basit, tek satırlık tahta kodlaması: her point "idx:W3/B2" vs. boş ise atlanır. */
    private String encodeState(GameState st) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 24; i++) {
            Point p = st.getPoint(i);
            if (p.isEmpty()) continue;
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
    /** Ters işlem (isteyen istemci tarafında yapsın). */

    private void broadcast(LegacyMessage m) { players.forEach(p -> p.send(m)); }
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
                                m.getInt("from"), m.getInt("to"), m.getInt("dieIdx"));
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