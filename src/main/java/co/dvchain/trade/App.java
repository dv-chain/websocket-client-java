package co.dvchain.trade;

import java.util.concurrent.CompletableFuture;
import java.util.Map;

import co.dvchain.trade.clientmessages.Clientmessages.Level;
import co.dvchain.trade.clientmessages.Clientmessages.LevelData;
import co.dvchain.trade.clientmessages.Clientmessages.TradeStatusResponse;
import co.dvchain.trade.websocket.WebsocketClient;
import co.dvchain.trade.websocket.WebsocketListenerImpl;
import co.dvchain.trade.rest.RestClient;
import co.dvchain.trade.rest.model.Position;
import co.dvchain.trade.rest.model.PositionsResponse;
import co.dvchain.trade.rest.model.TradesResponse.Trade;
import co.dvchain.trade.service.TradeService;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.util.logging.*;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());
    public static void main(String[] args) throws InterruptedException {
        Dotenv dotenv = Dotenv.load();

        String apiKey = dotenv.get("API_KEY");
        String apiUrl = dotenv.get("API_URL");
        String apiSecret = dotenv.get("API_SECRET");
        String restBase = dotenv.get("REST_BASE");

        RestClient restClient = new RestClient(restBase, apiKey, apiSecret);
        TradeService tradeService = new TradeService(restClient);
        
        WebsocketClient client = new WebsocketClient(apiUrl, apiKey, apiSecret);
        WebsocketListenerImpl websocketHandler = new WebsocketListenerImpl();
        client.setMessageHandler(websocketHandler);

        tradeService.start();
        logger.info("Started trade service - monitoring trades via REST polling");

        client.connect();
        client.subscribeLevel("BTC/USD");

        // Example: Get positions
        try {
            PositionsResponse positionsResponse = restClient.getPositions();
            for (Position position : positionsResponse.getAssets()) {
                if (position.getPosition() != 0) {
                    logger.info(String.format("Asset: %s, Position: %.8f, Max Buy: %.8f, Max Sell: %.8f",
                        position.getAsset(),
                        position.getPosition(),
                        position.getMaxBuy(),
                        position.getMaxSell()
                    ));
                }
            }
        } catch (IOException e) {
            logger.severe("Failed to get positions: " + e.getMessage());
        }

        // Wait for some trades to accumulate
        Thread.sleep(10000);

        // Example: Print all accumulated trades
        Map<String, Trade> trades = tradeService.getAllTrades();
        logger.info("Accumulated trades count: " + trades.size());
        for (Trade trade : trades.values()) {
            logger.info(String.format("Trade: %s %s %.8f %s/%s at %.2f",
                trade.getSide(),
                trade.getStatus(),
                trade.getQuantity(),
                trade.getAsset(),
                trade.getCounterAsset(),
                trade.getPrice()
            ));
        }

        // Example: Place a market order
        Thread.sleep(3000);
        LevelData levels = websocketHandler.getLatestQuoteForSymbol("BTC/USD");
        if (levels != null) {
            String[] assets = levels.getMarket().split("/");
            Level level = levels.getLevels(0);

            CompletableFuture<TradeStatusResponse> marketFuture = client.sendMarketOrder(
                levels.getQuoteId(),
                assets[0],
                assets[1],
                level.getBuyPrice(),
                "buy",
                level.getMaxQuantity(),
                "dv-sample-order"
            );

            marketFuture.thenAccept(response -> {
                logger.info("Market order response: " + response.getTradesCount() + " trades");
                // Trades will be automatically tracked via REST polling
            });

            marketFuture.exceptionally(e -> {
                logger.severe("Market order error: " + e.getMessage());
                return null;
            });
        }

        // Keep the application running to continue receiving trades
        Thread.sleep(30000);

        // Cleanup
        tradeService.stop();
        logger.info("Application shutdown complete");
    }
}