package com.blackflower.backgammon_computernetworks.server;

import com.blackflower.backgammon_computernetworks.model.*;

/**
 *
 * @author emirs
 */
public final class StateCodec {

    private StateCodec() {}

    /** ------------- String  ->  GameState (in-place) ------------- */
    public static void apply(String s, GameState st) {
        clear(st);

        for (String token : s.split(";")) {
            if (token.isBlank()) continue;

            if      (token.startsWith("barW="))   pushBar(st, PlayerColor.WHITE,  Integer.parseInt(token.substring(5)));
            else if (token.startsWith("barB="))   pushBar(st, PlayerColor.BLACK,  Integer.parseInt(token.substring(5)));
            else if (token.startsWith("offW="))   pullOff(st, PlayerColor.WHITE,  Integer.parseInt(token.substring(5)));
            else if (token.startsWith("offB="))   pullOff(st, PlayerColor.BLACK,  Integer.parseInt(token.substring(5)));
            else if (token.startsWith("diceUsed=")) {
                String mask = token.substring(9);
                boolean[] used = new boolean[mask.length()];
                for (int i = 0; i < mask.length(); i++) {
                    used[i] = (mask.charAt(i) == '1');
                }
                st.setDiceUsed(used);                      // ← yeni diziyi GameState’e aktar
            }
            else if (token.startsWith("turn=")) {                  // ② sıradaki oyuncu
                st.setCurrentTurn(PlayerColor.valueOf(token.substring(5)));
            }
            else if (token.contains(":")) {                       // point
                String[] kv  = token.split(":");
                int idx      = Integer.parseInt(kv[0]);
                char clr     = kv[1].charAt(0);
                int cnt      = Integer.parseInt(kv[1].substring(1));
                PlayerColor c = (clr=='W')? PlayerColor.WHITE : PlayerColor.BLACK;
                for (int i=0;i<cnt;i++) st.getPoint(idx).push(new Checker(c));
            }
        }
    }

    /** ------------- GameState -> String  (sunucu için) ------------- */
    public static String encode(GameState st){
        StringBuilder sb = new StringBuilder();
        for (int i=1;i<=24;i++){
            Point p = st.getPoint(i);
            if (p.isEmpty()) continue;
            sb.append(i).append(':')
              .append(p.peek().color()==PlayerColor.WHITE?'W':'B')
              .append(p.size()).append(';');
        }
        sb.append("barW=").append(st.checkersOnBar(PlayerColor.WHITE)).append(';');
        sb.append("barB=").append(st.checkersOnBar(PlayerColor.BLACK)).append(';');
        sb.append("offW=").append(st.borneOff(PlayerColor.WHITE)).append(';');
        sb.append("offB=").append(st.borneOff(PlayerColor.BLACK));
        sb.append("diceUsed=").append(usedMask(st.getDiceUsed())).append(';');
        sb.append("turn=").append(st.getCurrentTurn().name());
        return sb.toString();
    }

    /* ---------- yardımcılar ---------- */
    private static void clear(GameState st){
        for(int i=0;i<=24;i++){ Point p=st.getPoint(i); while(!p.isEmpty()) p.pop();}
        while(st.checkersOnBar(PlayerColor.WHITE)>0) st.enterFromBar(PlayerColor.WHITE);
        while(st.checkersOnBar(PlayerColor.BLACK)>0) st.enterFromBar(PlayerColor.BLACK);
        // borneOff sayaçlarını sıfırla:
        while(st.borneOff(PlayerColor.WHITE)>0){ st.bearOff(new Checker(PlayerColor.WHITE)); }
        while(st.borneOff(PlayerColor.BLACK)>0){ st.bearOff(new Checker(PlayerColor.BLACK)); }
    }
    private static void pushBar(GameState st,PlayerColor c,int n){
        for(int i=0;i<n;i++) st.sendToBar(new Checker(c));
    }
    private static void pullOff(GameState st,PlayerColor c,int n){
        for(int i=0;i<n;i++) st.bearOff(new Checker(c));
    }
    
    private static String usedMask(boolean[] arr) {
        StringBuilder b = new StringBuilder();
        for (boolean u : arr) {
            b.append(u ? '1' : '0');
        }
        return b.toString();
    }
}