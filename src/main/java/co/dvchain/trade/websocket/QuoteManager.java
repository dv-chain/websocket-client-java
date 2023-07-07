package co.dvchain.trade.websocket;

import java.util.HashMap;

import co.dvchain.trade.clientmessages.Clientmessages.LevelData;

public class QuoteManager {
    private HashMap<String, LevelData> latestQuote = new HashMap<String, LevelData>();

    public void updateQuote(LevelData levelData) {
        latestQuote.put(levelData.getMarket(), levelData);
    }

    public LevelData getQuote(String symbol) {
        return latestQuote.get(symbol);
    }
}
