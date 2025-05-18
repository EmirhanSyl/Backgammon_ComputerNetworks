package com.blackflower.backgammon_computernetworks.server;

import com.blackflower.backgammon_computernetworks.controller.*;
import com.blackflower.backgammon_computernetworks.model.GameState;

/**
 *
 * @author emirs
 */
public final class OnlineMoveController extends MovePhaseController {

    private final ClientNetwork net;
    private final GameState   state;

    public OnlineMoveController(ClientNetwork net, GameState state){
        this.net = net;
        this.state = state;
    }

    @Override
    public void onPointSelected(GameContext ctx,int point){
        super.onPointSelected(ctx,point);  // sadece selectedPoint yönetimi için

        // Seçim tamamlandı mı?
        int from = getSelectedPoint();
        if(from==-1) return;               // henüz birinci tıklama

        // İkinci tıklamada super() applyMove çağrısını yapmaz;
        // hamleyi sunucuya iletmeliyiz.
        // matchDie() kodu protected değilse yeniden hesaplayın; burada basitçe:
        // (Önceki yama ile aynı mantık)
    }

    /** Hamleyi gönder */
    private void sendMove(int from,int to,int die){
        net.send(new LegacyMessage("MOVE")
                    .put("from",from).put("to",to).put("dieUsed",die));
    }
}