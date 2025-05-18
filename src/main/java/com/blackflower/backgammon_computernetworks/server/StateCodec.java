package com.blackflower.backgammon_computernetworks.server;

import com.blackflower.backgammon_computernetworks.model.*;

/**
 *
 * @author emirs
 */
public final class StateCodec {

    private StateCodec(){}

    /** Dizgi  → mevcut GameState'i SIFIRLAYARAK doldurur. */
    public static void apply(String str, GameState st){
        // Board & bar & off alanlarını temizle
        for(int i=0;i<=24;i++){
            Point p = st.getPoint(i);
            while(!p.isEmpty()) p.pop();
        }
        // bar/off sayaçlarını sıfırlar
        while(st.checkersOnBar(PlayerColor.WHITE)>0) st.enterFromBar(PlayerColor.WHITE);
        while(st.checkersOnBar(PlayerColor.BLACK)>0) st.enterFromBar(PlayerColor.BLACK);
        // borneOff sayaçlarına erişim yoksa yoksay – sadece gösterim için lazımdı

        for(String token : str.split(";")){
            if(token.isBlank()) continue;
            if(token.startsWith("barW=")){
                int n=Integer.parseInt(token.substring(5));
                for(int i=0;i<n;i++) st.sendToBar(new Checker(PlayerColor.WHITE));
            }else if(token.startsWith("barB=")){
                int n=Integer.parseInt(token.substring(5));
                for(int i=0;i<n;i++) st.sendToBar(new Checker(PlayerColor.BLACK));
            }else if(token.contains(":")){
                String[] kv=token.split(":");
                int idx=Integer.parseInt(kv[0]);
                char clr = kv[1].charAt(0);
                int cnt = Integer.parseInt(kv[1].substring(1));
                for(int i=0;i<cnt;i++)
                    st.getPoint(idx).push(new Checker(
                            clr=='W'?PlayerColor.WHITE:PlayerColor.BLACK));
            }
        }
    }
}