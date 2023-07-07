### Java Websocket Client Package


This package is based on the protobuf definition in the repository. It provides a implementation for connecting to a websocket to receive market data feed and execute trades. The package supports subscribing to market levels and prices, as well as receiving notifications for order filled, order opened, order cancelled, and other events. It exposes a generic method to custom implement other messages.

#### Prerequisites

Before using this package, ensure that you have the following prerequisites:

- Java Development Kit (JDK) installed on your system
- API key, API URL, and API secret for the target websocket server

#### Installation

To use this package in your Java project, follow these steps:

1. Download the package and add it to your project's classpath.
2. Import the necessary classes into your Java file:
3. Implement your custom websocket listener, a sample can be found in this repository.

#### Usage

The following example demonstrates how to use the Java websocket client package to place and receive.

```java
import org.apache.log4j.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) throws InterruptedException {
        // Load environment variables from a .env file
        Dotenv dotenv = Dotenv.load();

        String apiKey = dotenv.get("API_KEY");
        String apiUrl = dotenv.get("API_URL");
        String apiSecret = dotenv.get("API_SECRET");

        // Create a websocket client instance
        WebsocketClient client = new WebsocketClient(apiUrl, apiKey, apiSecret);

        // Create a websocket listener implementation
        WebsocketListenerImpl websocketHandler = new WebsocketListenerImpl();

        // Set the message handler for the client
        client.setMessageHandler(websocketHandler);

        // Connect to the websocket server
        client.connect();

        // Subscribe to market levels for a specific symbol
        client.subscribeLevel("BTC/USD");

        // Wait for some time to receive the market levels
        Thread.sleep(3000);

        // Retrieve the latest market levels for a symbol
        LevelData levels = websocketHandler.getLatestQuoteForSymbol("BTC/USD");

        // Extract the asset names from the market levels
        String[] assets = levels.getMarket().split("/");

        // Retrieve the first level from the market levels
        Level level = levels.getLevels(0);

        // Place a market order
        CompletableFuture<TradeStatusResponse> marketFuture = client.sendMarketOrder(levels.getQuoteId(), assets[0], assets[1], level.getBuyPrice(), "buy", level.getMaxQuantity(), "dv-sample-order");
        marketFuture.thenAccept(new Consumer<TradeStatusResponse>() {
            @Override
            public void accept(TradeStatusResponse response) {
                logger.info("Received response for market order: " + response.getTradesCount());
            }
        });
        marketFuture.exceptionally(new Function<Throwable, TradeStatusResponse>() {
            @Override
            public TradeStatusResponse apply(Throwable e) {
                logger.info("Error while waiting for the response: " + e.getMessage());
                return null;
            }
        });

        // Place a limit order
        CompletableFuture<TradeStatusResponse> limitFuture = client.sendLimitOrder(assets[0], assets[1], level.getBuyPrice() - 100, "buy", level.getMaxQuantity(), "dv-sample-order");
        limitFuture.thenAccept(new Consumer<TradeStatusResponse>() {
            @Override
            public void accept(TradeStatusResponse response) {
                logger.info("Received response for limit order: " + response.toString());
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
```