package co.dvchain.trade;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import co.dvchain.trade.clientmessages.Clientmessages;
import co.dvchain.trade.clientmessages.Clientmessages.Level;
import co.dvchain.trade.clientmessages.Clientmessages.LevelData;
import co.dvchain.trade.clientmessages.Clientmessages.LimitsResponse;
import co.dvchain.trade.clientmessages.Clientmessages.TradeStatusResponse;
import co.dvchain.trade.websocket.WebsocketClient;
import co.dvchain.trade.websocket.WebsocketListenerImpl;
import co.dvchain.trade.rest.RestClient;
import co.dvchain.trade.rest.model.Position;
import co.dvchain.trade.rest.model.PositionsResponse;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Response;
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

        // Initialize REST client
        String restBaseUrl = restBase;
        RestClient restClient = new RestClient(restBaseUrl, apiKey, apiSecret);
        
        // Example: Get positions
        try {
            PositionsResponse positionsResponse = restClient.getPositions();
            logger.info("Got positions response: " + positionsResponse);
            
            // Example: Process individual positions
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

            // Log balances
            logger.info(String.format("Balances - USD: %.2f, CAD: %.2f, USDC: %.8f, USDT: %.8f",
                positionsResponse.getUsdBalance(),
                positionsResponse.getCadBalance(),
                positionsResponse.getUsdcBalance(),
                positionsResponse.getUsdtBalance()
            ));
        } catch (IOException e) {
            logger.severe("Failed to get positions: " + e.getMessage());
        }
        // Initialize WebSocket client
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
                if (response.getTradesCount() > 0) {
                    String orderId = response.getTrades(0).getId();
                    logger.info("Order placed with ID: " + orderId);
                    
                    // Wait a bit before canceling
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Cancel the order
                    CompletableFuture<TradeStatusResponse> cancelFuture = client.cancelOrder(orderId);
                    cancelFuture.thenAccept(new Consumer<TradeStatusResponse>() {
                        @Override
                        public void accept(TradeStatusResponse cancelResponse) {
                            logger.info("Order cancellation response: " + cancelResponse.toString());
                        }
                    });

                    cancelFuture.exceptionally(new Function<Throwable, TradeStatusResponse>() {
                        @Override
                        public TradeStatusResponse apply(Throwable e) {
                            logger.severe("Error canceling order: " + e.getMessage());
                            return null;
                        }
                    });
                }
            }
        });
    
        limitFuture.exceptionally(new Function<Throwable, TradeStatusResponse>() {
            @Override
            public TradeStatusResponse apply(Throwable e) {
                logger.severe("Error placing limit order: " + e.getMessage());
                return null;
            }
        });

        // Wait for all operations to complete
        Thread.sleep(10000);
    }
}