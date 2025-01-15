package co.dvchain.trade.service;

import co.dvchain.trade.rest.RestClient;
import co.dvchain.trade.rest.model.TradesResponse;
import co.dvchain.trade.rest.model.TradesResponse.Trade;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TradeService {
    private static final Logger logger = Logger.getLogger(TradeService.class.getName());
    private static final long POLLING_INTERVAL_MS = 5000; // 5 seconds
    
    private final RestClient restClient;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Trade> tradeCache;
    private final AtomicLong lastTimestamp;
    private ScheduledFuture<?> pollingTask;
    
    public TradeService(RestClient restClient) {
        this.restClient = restClient;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.tradeCache = new ConcurrentHashMap<>();
        this.lastTimestamp = new AtomicLong(0);
    }
    
    public void start() {
        // Start periodic polling
        pollingTask = scheduler.scheduleAtFixedRate(
            this::pollTrades,
            0,
            POLLING_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        logger.info("Trade service started");
    }
    
    public void stop() {
        if (pollingTask != null) {
            pollingTask.cancel(true);
        }
        scheduler.shutdown();
        logger.info("Trade service stopped");
    }
    
    private void pollTrades() {
        try {
            TradesResponse response = restClient.getTrades(lastTimestamp.get(), "Complete");
            if (response != null && response.getData() != null) {
                for (Trade trade : response.getData()) {
                    processTrade(trade);
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to poll trades: " + e.getMessage());
        }
    }
    
    public void processTrade(Trade trade) {
        // Convert ISO timestamp to epoch milliseconds and update last timestamp
        try {
            Instant instant = ZonedDateTime.parse(trade.getCreatedAt(), DateTimeFormatter.ISO_DATE_TIME).toInstant();
            lastTimestamp.updateAndGet(current -> Math.max(current, instant.toEpochMilli()));
        } catch (Exception e) {
            logger.warning("Failed to parse trade timestamp: " + e.getMessage());
        }
        
        // Store in cache using _id
        tradeCache.put(trade.getId(), trade);
        
        // Log the trade
        logger.info(String.format("Processed trade: ID=%s, Asset=%s/%s, Price=%.2f, Quantity=%.8f",
            trade.getId(), trade.getAsset(), trade.getCounterAsset(), trade.getPrice(), trade.getQuantity()));
    }
    
    // Method to process trades from WebSocket
    public void processWebSocketTrade(Trade trade) {
        processTrade(trade);
    }
    
    public Trade getTrade(String tradeId) {
        return tradeCache.get(tradeId);
    }
    
    public Map<String, Trade> getAllTrades() {
        return new ConcurrentHashMap<>(tradeCache);
    }
}
