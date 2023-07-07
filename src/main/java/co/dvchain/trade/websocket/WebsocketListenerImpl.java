package co.dvchain.trade.websocket;

import co.dvchain.trade.clientmessages.Clientmessages.ClientMessage;
import co.dvchain.trade.clientmessages.Clientmessages.LevelData;
import co.dvchain.trade.clientmessages.Clientmessages.OrderCancelled;
import co.dvchain.trade.clientmessages.Clientmessages.OrderFilled;
import co.dvchain.trade.clientmessages.Clientmessages.OrderOpened;
import co.dvchain.trade.clientmessages.Clientmessages.PricesData;
import java.util.logging.*;

public class WebsocketListenerImpl implements WebsocketListener{
    private static final Logger logger = Logger.getLogger(WebsocketListenerImpl.class.getName());
    private QuoteManager quoteManager = new QuoteManager();

    public LevelData getLatestQuoteForSymbol(String symbol) {
        return quoteManager.getQuote(symbol);
    }

    @Override
    public void onLevelUpdate(LevelData levelData) {
        quoteManager.updateQuote(levelData);
    }

    @Override
    public void onPriceUpdate(PricesData pricesData) {
        logger.info("Price update: " + pricesData.toString());
    }

    @Override
    public void onOrderFill(OrderFilled orderFilled) {
        logger.info("Order Fill update: " + orderFilled.toString());
    }

    @Override
    public void onOrderCancel(OrderCancelled orderCancelled) {
        logger.info("Order Cancel update: " + orderCancelled.toString());
    }

    @Override
    public void onOrderOpened(OrderOpened orderOpened) {
        logger.info("Order Cancel update: " + orderOpened.toString());
    }

    @Override
    public void onClientMessage(ClientMessage message) {
        // Receive a raw message
    }
}
