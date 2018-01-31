package com.coinbase.exchange.api.gui;

import com.coinbase.exchange.api.GdaxApiApplication;
import com.coinbase.exchange.api.entity.NewMarketOrderSingle;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.orders.Order;
import com.coinbase.exchange.api.orders.OrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

import static com.coinbase.exchange.api.GdaxApiApplication.SYSTEM_PROPERTY_JAVA_AWT_HEADLESS;
import static org.junit.Assert.assertTrue;

/**
 * Created by robevans.uk on 01/09/2017.
 */
@Component
public class PriceTracker {

	static final Logger log = LoggerFactory.getLogger(PriceTracker.class);

	Boolean guiEnabled;

	JFrame frame;

    OrderService orderService;

	MarketDataService marketDataService;

	JLabel prices;

	@Autowired
	public PriceTracker(@Value("${trader.enabled}") boolean enabled, MarketDataService marketDataService, OrderService orderService) {
		log.info("Price Tracker Constructor ..." + enabled);
		this.guiEnabled = enabled;
		this.marketDataService = marketDataService;
		this.orderService = orderService;
		if (enabled) {
			Run();
		}
		BigDecimal x = BigDecimal.valueOf(0.0);
		x=x.add(BigDecimal.valueOf(1.0));
		System.out.println(x);
		System.out.println(x.add(BigDecimal.valueOf(1.0)));
		System.out.println(x);
	}
    private NewMarketOrderSingle createNewMarketOrder(String product, String action, BigDecimal size){
        NewMarketOrderSingle marketOrder = new NewMarketOrderSingle();
        marketOrder.setProduct_id(product);
        marketOrder.setSide(action);
        marketOrder.setSize(size);
        return marketOrder;
    }
	
    public BigDecimal createMarketOrderBuy(){
        NewMarketOrderSingle marketOrder = createNewMarketOrder("BTC-EUR", "buy", new BigDecimal(0.001));
        Order order = orderService.createOrder(marketOrder);

        assertTrue(order != null); //make sure we created an order
        String orderId = order.getId();
        assertTrue(orderId.length() > 0); //ensure we have an actual orderId
        Order filledOrder = orderService.getOrder(orderId);
        assertTrue(filledOrder != null); //ensure our order hit the system
        assertTrue(new BigDecimal(filledOrder.getSize()).compareTo(BigDecimal.ZERO) > 0); //ensure we got a fill
        log.info("Order opened and filled: " + filledOrder.getSize() + " @ " + filledOrder.getExecuted_value()
             + " at the cost of " + filledOrder.getFill_fees());
        return BigDecimal.valueOf(Double.parseDouble(filledOrder.getFill_fees()));        
   }
    

    public BigDecimal createMarketOrderSell(){
        NewMarketOrderSingle marketOrder = createNewMarketOrder("BTC-EUR", "sell", new BigDecimal(0.001));
        Order order = orderService.createOrder(marketOrder);
        assertTrue(order != null); //make sure we created an order
        String orderId = order.getId();
        assertTrue(orderId.length() > 0); //ensure we have an actual orderId
        Order filledOrder = orderService.getOrder(orderId);
        assertTrue(filledOrder != null); //ensure our order hit the system
        assertTrue(new BigDecimal(filledOrder.getSize()).compareTo(BigDecimal.ZERO) > 0); //ensure we got a fill
        log.info("Order opened and filled: " + filledOrder.getSize() + " @ " + filledOrder.getExecuted_value()
                + " at the cost of " + filledOrder.getFill_fees());
        return BigDecimal.valueOf(Double.parseDouble(filledOrder.getFill_fees()));
    }


	public void Run() {
		try {
			History history = new History(60);
			int StatusLong = 0;
			int StatusShort = 0;
			BigDecimal LongBuy = BigDecimal.valueOf(0.0);
			BigDecimal LongSell = BigDecimal.valueOf(0.0);
			BigDecimal ShortBuy = BigDecimal.valueOf(0.0);
			BigDecimal ShortSell = BigDecimal.valueOf(0.0);
			BigDecimal Long = BigDecimal.valueOf(0.0);
			BigDecimal Short = BigDecimal.valueOf(0.0);
			BigDecimal LongFees = BigDecimal.valueOf(0.0);
			BigDecimal ShortFees = BigDecimal.valueOf(0.0);			
			
			for (int ind = 0; ind < 10000; ind++) {
				BigDecimal btcAsk = this.marketDataService.getMarketDataOrderBook("BTC-EUR", "1").getAsks().get(0).getPrice().divide(BigDecimal.valueOf(1000.0));
				history.Add(btcAsk);
				BigDecimal down = btcAsk.subtract(history.MinElement());
				BigDecimal up = history.MaxElement().subtract(btcAsk);
				if (ind % 50 == 0) {
						log.info("Prize: " + btcAsk.doubleValue() + ", Avg: " + history.Avg().doubleValue() + ", Min: " + down.doubleValue() + ", Max:  " + up.doubleValue() + " - Iteration: " + ind);
				}						
				if (ind > 10 && StatusLong == 0 && down.compareTo(up)==1) {
					StatusLong = 1;
					LongBuy = btcAsk;
					LongFees.add(createMarketOrderBuy());
					log.info("Long-BUY: " + LongBuy);
					log.info("Prize: " + btcAsk.doubleValue() + ", Avg: " + history.Avg().doubleValue() + ", Min: " + down.doubleValue() + ", Max:  " + up.doubleValue() + " - Iteration: " + ind);

				}
				if (StatusLong == 1 && down.compareTo(BigDecimal.valueOf(0.0))==0) {
					StatusLong = 0;
					LongSell = btcAsk;
					Long = Long.add(LongSell.subtract(LongBuy));
					LongFees = LongFees.add(createMarketOrderSell());					
					log.info("Long-SOLD: " + LongSell + ", RESULT = " + LongSell.subtract(LongBuy) + ", Long-SubTotal: " + Long + ", Long-Fees: " + LongFees + ", Long-TOTAL: " + Long.subtract(LongFees));
					log.info("Prize: " + btcAsk.doubleValue() + ", Avg: " + history.Avg().doubleValue() + ", Min: " + down.doubleValue() + ", Max:  " + up.doubleValue() + " - Iteration: " + ind);
				}

				if (ind > 10 && StatusShort == 0 && down.compareTo(up)==-1) {
					StatusShort = 1;
					ShortSell = btcAsk;
					ShortFees = ShortFees.add(createMarketOrderSell());					
					log.info("Short-SELL: " + ShortSell);
					log.info("Prize: " + btcAsk.doubleValue() + ", Avg: " + history.Avg().doubleValue() + ", Min: " + down.doubleValue() + ", Max:  " + up.doubleValue() + " - Iteration: " + ind);
				}
				if (StatusShort == 1 && up.compareTo(BigDecimal.valueOf(0.0))==0) {
					StatusShort = 0;
					ShortBuy = btcAsk;
					Short = Short.add(ShortSell.subtract(ShortBuy));
					ShortFees = ShortFees.add(createMarketOrderBuy());
					log.info("Short-BOUGHT: " + ShortBuy + ", RESULT = " + ShortSell.subtract(ShortBuy) + ", Short-SubTotal: " + Short + ", Short-Fees: " + ShortFees + ", Short-TOTAL: " + Short.subtract(ShortFees));
					log.info("Prize: " + btcAsk.doubleValue() + ", Avg: " + history.Avg().doubleValue() + ", Min: " + down.doubleValue() + ", Max:  " + up.doubleValue() + " - Iteration: " + ind);
				}
				
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void startGui() {
		log.info("Start-GUI - PriceTracker");
		if (guiEnabled) {
			frame = new JFrame("Gdax Desktop Client");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(640, 120);
			frame.setLayout(new BorderLayout());
			frame.setVisible(true);
			prices = new JLabel("BTC: $0.00 | ETH: $0.00 | LTC: $0.00");
			frame.add(prices);
			log.info("JFrame CTOR");
		}

		SwingUtilities.invokeLater(() -> {
			while (true) {
				String btcPrice = marketDataService.getMarketDataOrderBook("BTC-USD", "1").getAsks().get(0).getPrice()
						.toString();
				String ethPrice = marketDataService.getMarketDataOrderBook("ETH-USD", "1").getAsks().get(0).getPrice()
						.toString();
				String ltcPrice = marketDataService.getMarketDataOrderBook("LTC-USD", "1").getAsks().get(0).getPrice()
						.toString();
				prices.setText(String.format("BTC: ${} | ETH: ${} | LTC: ${}", btcPrice, ethPrice, ltcPrice));
			}
		});
	}

	public static void main(String[] args) {
		log.info("Price-Tracker MAIN");
		System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS,
				System.getProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(false)));
		SpringApplication.run(GdaxApiApplication.class, args);
	}
}
