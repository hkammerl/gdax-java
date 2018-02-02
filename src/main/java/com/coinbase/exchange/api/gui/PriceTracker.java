package com.coinbase.exchange.api.gui;

import com.coinbase.exchange.api.GdaxApiApplication;
import com.coinbase.exchange.api.accounts.Account;
import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.entity.NewMarketOrderSingle;
import com.coinbase.exchange.api.marketdata.MarketData;
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
import java.util.List;
import java.util.Optional;

import static com.coinbase.exchange.api.GdaxApiApplication.SYSTEM_PROPERTY_JAVA_AWT_HEADLESS;
import static org.junit.Assert.assertEquals;
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

	boolean trading;
	int historyLength;
	double factor;
	double spread;
	boolean shortMarket;
	boolean longMarket;

	@Autowired
	public PriceTracker(@Value("${trader.enabled}") boolean enabled, @Value("${trader.trading}") boolean trading,
			@Value("${trader.shortMarket}") boolean shortMarket, @Value("${trader.longMarket}") boolean longMarket,
			@Value("${trader.historyLength}") int historyLength, @Value("${trader.factor}") double factor,
			@Value("${trader.spread}") double spread, MarketDataService marketDataService, OrderService orderService) {
		log.info("Price Tracker Constructor ..." + enabled);
		this.guiEnabled = enabled;
		this.marketDataService = marketDataService;
		this.orderService = orderService;
		this.trading = trading;
		this.historyLength = historyLength;
		this.factor = factor;
		this.spread = spread;
		this.shortMarket = shortMarket;
		this.longMarket = longMarket;
		log.info("Trading: " + this.trading + ", HistoryLength: " + this.historyLength + ", Factor: " + this.factor
				+ ", Spread: " + this.spread);
		if (enabled) {
			Run();
		}
	}

	private NewLimitOrderSingle getNewLimitOrderSingle(String BuyOrSell, String productId, BigDecimal price,
			BigDecimal size) {
		NewLimitOrderSingle limitOrder = new NewLimitOrderSingle();
		limitOrder.setProduct_id(productId);
		limitOrder.setSide(BuyOrSell);
		limitOrder.setType("limit");
		limitOrder.setPrice(price);
		limitOrder.setSize(size);
		limitOrder.setPost_only(false);
		return limitOrder;
	}

	public Order CreateLimitOrder(String BuyOrSell, BigDecimal price, BigDecimal size) {
		if (trading) {
			NewLimitOrderSingle limitOrder = getNewLimitOrderSingle(BuyOrSell, "BTC-EUR", price, size);
			Order order = orderService.createOrder(limitOrder);
			log.info("Limit Order :" + order.toString() + " - " + order.getFilled_size() + " - " + order.getStatus()
					+ "-" + order.getPrice() + " - " + order.getSettled());
			return order;
		}
		return null;
	}

	private NewMarketOrderSingle createNewMarketOrder(String product, String action, BigDecimal size) {
		NewMarketOrderSingle marketOrder = new NewMarketOrderSingle();
		marketOrder.setProduct_id(product);
		marketOrder.setSide(action);
		marketOrder.setSize(size);
		return marketOrder;
	}

	public BigDecimal createMarketOrderBuy() {
		if (trading) {
			NewMarketOrderSingle marketOrder = createNewMarketOrder("BTC-EUR", "buy", new BigDecimal(0.001));
			Order order = orderService.createOrder(marketOrder);

			assertTrue(order != null); // make sure we created an order
			String orderId = order.getId();
			assertTrue(orderId.length() > 0); // ensure we have an actual orderId
			Order filledOrder = orderService.getOrder(orderId);
			assertTrue(filledOrder != null); // ensure our order hit the system
			assertTrue(new BigDecimal(filledOrder.getSize()).compareTo(BigDecimal.ZERO) > 0); // ensure we got a fill
			log.info("Order opened and filled: " + filledOrder.getSize() + " @ " + filledOrder.getExecuted_value()
					+ " at the cost of " + filledOrder.getFill_fees());
			return BigDecimal.valueOf(Double.parseDouble(filledOrder.getFill_fees()))
					.divide(BigDecimal.valueOf(factor));
		} else {
			// return BigDecimal.valueOf(0.02).divide(BigDecimal.valueOf(factor));
			return BigDecimal.valueOf(spread);
		}
	}

	public BigDecimal createMarketOrderSell() {
		if (trading) {
			NewMarketOrderSingle marketOrder = createNewMarketOrder("BTC-EUR", "sell", new BigDecimal(0.001));
			Order order = orderService.createOrder(marketOrder);
			assertTrue(order != null); // make sure we created an order
			String orderId = order.getId();
			assertTrue(orderId.length() > 0); // ensure we have an actual orderId
			Order filledOrder = orderService.getOrder(orderId);
			assertTrue(filledOrder != null); // ensure our order hit the system
			assertTrue(new BigDecimal(filledOrder.getSize()).compareTo(BigDecimal.ZERO) > 0); // ensure we got a fill
			log.info("Order opened and filled: " + filledOrder.getSize() + " @ " + filledOrder.getExecuted_value()
					+ " at the cost of " + filledOrder.getFill_fees());
			return BigDecimal.valueOf(Double.parseDouble(filledOrder.getFill_fees()))
					.divide(BigDecimal.valueOf(factor));
		} else {
			// return BigDecimal.valueOf(0.02).divide(BigDecimal.valueOf(factor));
			return BigDecimal.valueOf(spread);
		}
	}

	public void Run() {
		try {
			History history = new History(historyLength);
			int LongStatus = 0;
			int ShortStatus = 0;
			BigDecimal LongBuy = BigDecimal.valueOf(0.0);
			BigDecimal LongSell = BigDecimal.valueOf(0.0);
			BigDecimal ShortBuy = BigDecimal.valueOf(0.0);
			BigDecimal ShortSell = BigDecimal.valueOf(0.0);
			BigDecimal Long = BigDecimal.valueOf(0.0);
			BigDecimal Short = BigDecimal.valueOf(0.0);
			BigDecimal LongFees = BigDecimal.valueOf(0.0);
			BigDecimal ShortFees = BigDecimal.valueOf(0.0);
			BigDecimal lastBtcAsk = BigDecimal.valueOf(0.0);
			BigDecimal LongStop = BigDecimal.valueOf(0.0);
			BigDecimal ShortStop = BigDecimal.valueOf(0.0);
			BigDecimal Avg = BigDecimal.valueOf(0.0);
			BigDecimal AvgPercentage = BigDecimal.valueOf(0.0);
			BigDecimal AvgOld = BigDecimal.valueOf(0.0);
			int LongShort = 0;
			BigDecimal AskAvgSpread = BigDecimal.valueOf(0.0);
			boolean StopOrderActive = false;

			int LongTrades = 0;
			int ShortTrades = 0;

			Order stopOrder = null;

			// CreateLimitOrder("buy", BigDecimal.valueOf(7240.0),
			// BigDecimal.valueOf(0.001));
			// CreateLimitOrder("sell", BigDecimal.valueOf(5000.0),
			// BigDecimal.valueOf(0.001));
			log.info("PriceTracker - RUN");
			// log.info("Create Limit Order");
			// Order o = CreateLimitOrder("buy",
			// BigDecimal.valueOf(2000.0),BigDecimal.valueOf(0.001));
			// log.info("Status: " + o.getStatus());
			// Thread.sleep(10000);
			// log.info("Cancel Order: ");
			// orderService.cancelOrder(o.getId());
			// log.info("Status: " + o.getStatus());

			for (int ind = 0; ind >= 0; ind++) {
				BigDecimal btcAsk = this.marketDataService.getMarketDataOrderBook("BTC-EUR", "1").getAsks().get(0)
						.getPrice();
				history.Add(btcAsk);
				BigDecimal down = btcAsk.subtract(history.MinElement());
				BigDecimal up = history.MaxElement().subtract(btcAsk);
				AvgOld = Avg;
				Avg = history.Avg();
				AvgPercentage = history.AvgLastPercentage(0.1);
				AskAvgSpread = AvgPercentage.subtract(Avg);
				LongShort = Avg.compareTo(AvgOld);

				if (LongStatus == 1 && Avg.compareTo(LongStop) == 1) { // (LongStatus == 1 &&
																		// btcAsk.subtract(BigDecimal.valueOf(spread)).compareTo(LongStop)
																		// == 1) {
					LongStop = Avg;
					log.info("# Stop-Long: " + LongStop + ", book: " + btcAsk.subtract(LongBuy) + ", Fix: "
							+ LongStop.subtract(LongBuy));
				}
				if (ShortStatus == 1 && Avg.compareTo(ShortStop) == -1) { // (ShortStatus == 1 &&
																			// btcAsk.add(BigDecimal.valueOf(spread)).compareTo(ShortStop)
																			// == -1) {
					ShortStop = Avg;
					log.info("# Stop-Short: " + ShortStop + ", book: " + ShortSell.subtract(btcAsk) + ", Fix: "
							+ ShortSell.subtract(ShortStop));
				}
				if (ind % 100000 == 0 && btcAsk.compareTo(lastBtcAsk) != 0) {
					lastBtcAsk = btcAsk;
					log.info("LONG-SHORT: " + LongShort + ", Spread: " + AskAvgSpread + ", btcAsk: "
							+ btcAsk.doubleValue() + ", AvgP: " + AvgPercentage + ", Avg: " + Avg + ", AvgOld: "
							+ AvgOld + ", Min: " + down.doubleValue() + ", Max:  " + up.doubleValue() + ", Long-STOP: "
							+ LongStop + ", Short-STOP: " + ShortStop + " - Iteration: " + ind);
				}
				if (ind == 0) {
					log.info("LONG  TRADES - Status: " + LongStatus + ", Trades: " + LongTrades + ", book: "
							+ btcAsk.subtract(LongBuy) + ", Fix: " + LongStop.subtract(LongBuy) + ", Win: " + Long
							+ ", Fees: " + LongFees + ", Total: " + Long.subtract(LongFees) + ", Value: " + LongBuy
							+ ", Stop: " + LongStop + " -- " + ind);
					log.info("SHORT TRADES - Status: " + ShortStatus + ", Trades: " + ShortTrades + ", book: "
							+ ShortSell.subtract(btcAsk) + ", Fix: " + ShortSell.subtract(ShortStop) + ", Win: " + Short
							+ ", Fees: " + ShortFees + ", Total: " + Short.subtract(ShortFees) + ", Value: " + ShortSell
							+ ", Stop: " + ShortStop + " -- " + ind);
				}
				// LONG - TRADES
				if (longMarket && ind > historyLength && LongStatus == 0 && LongShort == 1
						&& AskAvgSpread.longValue() > spread) {
					LongStatus = 1;
					LongTrades = LongTrades + 1;
					LongBuy = btcAsk;
					// LongStop = LongBuy.subtract(BigDecimal.valueOf(spread));
					LongStop = Avg;
					LongFees = LongFees.add(createMarketOrderBuy());
					log.info("++ Long-BUY: " + LongBuy + ", Long-STOP: " + LongStop);
					log.info("++ LONG-SHORT: " + LongShort + ", Spread: " + AskAvgSpread + ", btcAsk: "
							+ btcAsk.doubleValue() + ", Avg: " + Avg + ", AvgOld: " + AvgOld + ", Min: "
							+ down.doubleValue() + ", Max:  " + up.doubleValue() + ", Long-STOP: " + LongStop
							+ ", Short-STOP: " + ShortStop + " - Iteration: " + ind);
					log.info("++ LONG  TRADES - Status: " + LongStatus + ", Trades: " + LongTrades + ", book: "
							+ btcAsk.subtract(LongBuy) + ", Fix: " + LongStop.subtract(LongBuy) + ", Win: " + Long
							+ ", Fees: " + LongFees + ", Total: " + Long.subtract(LongFees) + ", Value: " + LongBuy
							+ ", Stop: " + LongStop + " -- " + ind);
					log.info("++ SHORT TRADES - Status: " + ShortStatus + ", Trades: " + ShortTrades + ", book: "
							+ ShortSell.subtract(btcAsk) + ", Fix: " + ShortSell.subtract(ShortStop) + ", Win: " + Short
							+ ", Fees: " + ShortFees + ", Total: " + Short.subtract(ShortFees) + ", Value: " + ShortSell
							+ ", Stop: " + ShortStop + " -- " + ind);

				}
				if (LongStatus == 1 && btcAsk.compareTo(LongStop) == -1) { // (LongStatus == 1 &&
																			// btcAsk.compareTo(LongStop) == -1 &&
																			// AskAvgSpread.longValue() < 0.0) {
					LongStatus = 0;
					LongTrades = LongTrades + 1;
					LongSell = btcAsk;
					Long = Long.add(btcAsk.subtract(LongBuy));
					LongFees = LongFees.add(createMarketOrderSell());
					LongStop = BigDecimal.valueOf(0);
					log.info("## Long-SOLD: " + LongSell + ", RESULT = " + LongSell.subtract(LongBuy)
							+ ", Long-SubTotal: " + Long + ", Long-Fees: " + LongFees + ", Long-TOTAL: "
							+ Long.subtract(LongFees));
					log.info("## LONG-SHORT: " + LongShort + ", Spread: " + AskAvgSpread + ", btcAsk: "
							+ btcAsk.doubleValue() + ", Avg: " + Avg + ", AvgOld: " + AvgOld + ", Min: "
							+ down.doubleValue() + ", Max:  " + up.doubleValue() + ", Long-STOP: " + LongStop
							+ ", Short-STOP: " + ShortStop + " - Iteration: " + ind);
					log.info("## LONG  TRADES - Status: " + LongStatus + ", Trades: " + LongTrades + ", book: "
							+ btcAsk.subtract(LongBuy) + ", Fix: " + LongStop.subtract(LongBuy) + ", Win: " + Long
							+ ", Fees: " + LongFees + ", Total: " + Long.subtract(LongFees) + ", Value: " + LongBuy
							+ ", Stop: " + LongStop + " -- " + ind);
					log.info("## SHORT TRADES - Status: " + ShortStatus + ", Trades: " + ShortTrades + ", book: "
							+ ShortSell.subtract(btcAsk) + ", Fix: " + ShortSell.subtract(ShortStop) + ", Win: " + Short
							+ ", Fees: " + ShortFees + ", Total: " + Short.subtract(ShortFees) + ", Value: " + ShortSell
							+ ", Stop: " + ShortStop + " -- " + ind);
				}
				// SHORT - TRADES
				if (shortMarket && ind > historyLength && ShortStatus == 0 && LongShort == -1
						&& AskAvgSpread.longValue() < (-1.0) * spread) {
					ShortStatus = 1;
					ShortTrades = ShortTrades + 1;
					ShortSell = btcAsk;
					ShortStop = Avg;
					ShortFees = ShortFees.add(createMarketOrderSell());
					log.info("-- Short-SELL: " + ShortSell + ", Short-STOP: " + ShortStop);
					log.info("-- LONG-SHORT: " + LongShort + ", Spread: " + AskAvgSpread + ", btcAsk: "
							+ btcAsk.doubleValue() + ", Avg: " + Avg + ", AvgOld: " + AvgOld + ", Min: "
							+ down.doubleValue() + ", Max:  " + up.doubleValue() + ", Long-STOP: " + LongStop
							+ ", Short-STOP: " + ShortStop + " - Iteration: " + ind);
					log.info("-- LONG  TRADES - Status: " + LongStatus + ", Trades: " + LongTrades + ", book: "
							+ btcAsk.subtract(LongBuy) + ", Fix: " + LongStop.subtract(LongBuy) + ", Win: " + Long
							+ ", Fees: " + LongFees + ", Total: " + Long.subtract(LongFees) + ", Value: " + LongBuy
							+ ", Stop: " + LongStop + " -- " + ind);
					log.info("-- SHORT TRADES - Status: " + ShortStatus + ", Trades: " + ShortTrades + ", book: "
							+ ShortSell.subtract(btcAsk) + ", Fix: " + ShortSell.subtract(ShortStop) + ", Win: " + Short
							+ ", Fees: " + ShortFees + ", Total: " + Short.subtract(ShortFees) + ", Value: " + ShortSell
							+ ", Stop: " + ShortStop + " -- " + ind);
				}
				if (ShortStatus == 1 && btcAsk.compareTo(ShortStop) == 1) {// (ShortStatus == 1 &&
																			// btcAsk.compareTo(ShortStop) == 1 &&
																			// AskAvgSpread.longValue() > 0.0) {
					ShortStatus = 0;
					ShortTrades = ShortTrades + 1;
					ShortBuy = btcAsk;
					Short = Short.add(ShortSell.subtract(ShortBuy));
					ShortFees = ShortFees.add(createMarketOrderBuy());
					ShortStop = BigDecimal.valueOf(0);
					log.info("## Short-BOUGHT: " + ShortBuy + ", RESULT = " + ShortSell.subtract(ShortBuy)
							+ ", Short-SubTotal: " + Short + ", Short-Fees: " + ShortFees + ", Short-TOTAL: "
							+ Short.subtract(ShortFees));
					log.info("## LONG-SHORT: " + LongShort + ", Spread: " + AskAvgSpread + ", btcAsk: "
							+ btcAsk.doubleValue() + ", Avg: " + Avg + ", AvgOld: " + AvgOld + ", Min: "
							+ down.doubleValue() + ", Max:  " + up.doubleValue() + ", Long-STOP: " + LongStop
							+ ", Short-STOP: " + ShortStop + " - Iteration: " + ind);
					log.info("## LONG  TRADES - Status: " + LongStatus + ", Trades: " + LongTrades + ", book: "
							+ btcAsk.subtract(LongBuy) + ", Fix: " + LongStop.subtract(LongBuy) + ", Win: " + Long
							+ ", Fees: " + LongFees + ", Total: " + Long.subtract(LongFees) + ", Value: " + LongBuy
							+ ", Stop: " + LongStop + " -- " + ind);
					log.info("## SHORT TRADES - Status: " + ShortStatus + ", Trades: " + ShortTrades + ", book: "
							+ ShortSell.subtract(btcAsk) + ", Fix: " + ShortSell.subtract(ShortStop) + ", Win: " + Short
							+ ", Fees: " + ShortFees + ", Total: " + Short.subtract(ShortFees) + ", Value: " + ShortSell
							+ ", Stop: " + ShortStop + " -- " + ind);
				}

				Thread.sleep(1000);
			}
		} catch (

		InterruptedException e) {
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
