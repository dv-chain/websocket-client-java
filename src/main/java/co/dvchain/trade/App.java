package co.dvchain.trade;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import co.dvchain.trade.clientmessages.Clientmessages.Level;
import co.dvchain.trade.clientmessages.Clientmessages.LevelData;
import co.dvchain.trade.clientmessages.Clientmessages.TradeStatusResponse;
import co.dvchain.trade.websocket.WebsocketClient;
import co.dvchain.trade.websocket.WebsocketListenerImpl;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.logging.*;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());
    public static void main(String[] args) throws InterruptedException {
        Dotenv dotenv = Dotenv.load();

        String apiKey = dotenv.get("API_KEY");
        String apiUrl = dotenv.get("API_URL");
        String apiSecret = dotenv.get("API_SECRET");

        WebsocketClient client = new WebsocketClient(apiUrl, apiKey, apiSecret);
        
        WebsocketListenerImpl websocketHandler = new WebsocketListenerImpl();
        
        client.setMessageHandler(websocketHandler);

        client.connect();

        client.subscribeLevel("BTC/USD");

        Thread.sleep(3000);
        
        LevelData levels = websocketHandler.getLatestQuoteForSymbol("BTC/USD");


        String[] assets = levels.getMarket().split("/");

        Level level = levels.getLevels(0);

        CompletableFuture<TradeStatusResponse> marketFuture  = client.sendMarketOrder(levels.getQuoteId(), assets[0], assets[1], level.getBuyPrice(), "buy", level.getMaxQuantity(), "dv-sample-order");
        marketFuture.thenAccept(new Consumer<TradeStatusResponse>() {
            @Override
            public void accept(TradeStatusResponse response) {
                logger.info("Received response for market order " + response.getTradesCount());
            }
        });
    
        marketFuture.exceptionally(new Function<Throwable, TradeStatusResponse>() {
            @Override
            public TradeStatusResponse apply(Throwable e) {
                logger.info("Error while waiting for the response: " + e.getMessage());
                return null;
            }
        });


        CompletableFuture<TradeStatusResponse> limitFuture  = client.sendLimitOrder(assets[0], assets[1], level.getBuyPrice() - 100, "buy", level.getMaxQuantity(), "dv-sample-order");
        limitFuture.thenAccept(new Consumer<TradeStatusResponse>() {
            @Override
            public void accept(TradeStatusResponse response) {
                logger.info("Received response for limit order " + response.toString());
            }
        });
    
        limitFuture.exceptionally(new Function<Throwable, TradeStatusResponse>() {
            @Override
            public TradeStatusResponse apply(Throwable e) {
                logger.info("Error while waiting for the response: " + e.getMessage());
                return null;
            }
        });
    }
}