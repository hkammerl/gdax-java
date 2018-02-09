package com.coinbase.exchange.api.gui;

import com.coinbase.exchange.api.GdaxApiApplication;
import com.coinbase.exchange.api.accounts.Account;
import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.entity.NewMarketOrderSingle;
import com.coinbase.exchange.api.marketdata.MarketData;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.mysql.Marketprice;
import com.coinbase.exchange.api.mysql.MarketpriceRepository;
import com.coinbase.exchange.api.mysql.Runconfiguration;
import com.coinbase.exchange.api.mysql.RunconfigurationRepository;
import com.coinbase.exchange.api.mysql.Trade;
import com.coinbase.exchange.api.mysql.TradeRepository;
import com.coinbase.exchange.api.orders.Order;
import com.coinbase.exchange.api.orders.OrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.coinbase.exchange.api.GdaxApiApplication.SYSTEM_PROPERTY_JAVA_AWT_HEADLESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by robevans.uk on 01/09/2017.
 */
@Component
public class PriceTracker {

	static final Logger log = LoggerFactory.getLogger(PriceTracker.class);

	History history;
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
	BigDecimal LongStop = BigDecimal.valueOf(0.0);
	BigDecimal ShortStop = BigDecimal.valueOf(0.0);
	BigDecimal Avg = BigDecimal.valueOf(0.0);
	BigDecimal AvgPercentage = BigDecimal.valueOf(0.0);
	BigDecimal AvgOld = BigDecimal.valueOf(0.0);
	BigDecimal TradeStart = BigDecimal.valueOf(0.0);
	int LongShort = 0;
	BigDecimal AskAvgSpread = BigDecimal.valueOf(0.0);
	BigDecimal fee = BigDecimal.valueOf(0.0);
	int LongTrades = 0;
	int ShortTrades = 0;
	Trade transaction = null;
	String transactionId = "";
	String runId = "";
	BigDecimal btcAsk = BigDecimal.valueOf(0);
	int ind = 0;

	Boolean guiEnabled;

	JFrame frame;
	OrderService orderService;
	MarketDataService marketDataService;
	MarketpriceRepository marketpriceRepository;
	TradeRepository tradeRepository;
	RunconfigurationRepository runconfigurationRepository;
	JLabel prices;

	boolean trading;
	int historyLength;
	double historyPercentage;
	double factor;
	int spread;
	boolean shortMarket;
	boolean longMarket;

	@Autowired
	public PriceTracker(@Value("${trader.enabled}") boolean enabled, @Value("${trader.trading}") boolean trading,
			@Value("${trader.shortMarket}") boolean shortMarket, @Value("${trader.longMarket}") boolean longMarket,
			@Value("${trader.historyLength}") int historyLength,
			@Value("${trader.historyPercentage}") double historyPercentage, @Value("${trader.factor}") double factor,
			@Value("${trader.spread}") int spread, MarketDataService marketDataService, OrderService orderService,
			MarketpriceRepository marketpriceRepository, TradeRepository tradeRepository,
			RunconfigurationRepository runconfigurationRepository) {
		log.info("Price Tracker Constructor ..." + enabled);
		this.guiEnabled = enabled;
		this.marketDataService = marketDataService;
		this.orderService = orderService;
		this.marketpriceRepository = marketpriceRepository;
		this.tradeRepository = tradeRepository;
		this.runconfigurationRepository = runconfigurationRepository;

		this.trading = trading;
		this.historyLength = historyLength;
		this.historyPercentage = historyPercentage;
		this.factor = factor;
		this.spread = spread;
		this.shortMarket = shortMarket;
		this.longMarket = longMarket;

		history = new History(historyLength);

		TradeStart = this.marketDataService.getMarketDataOrderBook("BTC-EUR", "1").getAsks().get(0).getPrice();
		runId = UUID.randomUUID().toString();

		Runconfiguration runconfiguration = new Runconfiguration(runId, historyLength, historyPercentage, spread);
		runconfigurationRepository.save(runconfiguration);

		log.info("Trading: " + this.trading + ", HistoryLength: " + this.historyLength + ", HistoryPercentage: "
				+ this.historyPercentage + ", Factor: " + this.factor + ", Spread: " + this.spread);
		// if (enabled) {
		// Run();
		// }
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
			return this.marketDataService.getMarketDataOrderBook("BTC-EUR", "1").getAsks().get(0).getPrice()
					.multiply(BigDecimal.valueOf(0.0025));
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
			return this.marketDataService.getMarketDataOrderBook("BTC-EUR", "1").getAsks().get(0).getPrice()
					.multiply(BigDecimal.valueOf(0.0025));
		}
	}
	@Scheduled(fixedRate = 5000)
	public void Run() {
		ind = ind + 1;
		log.info("PriceTracker - RUN: " + runId, ", Ind: " + ind);

		try {
			btcAsk = this.marketDataService.getMarketDataOrderBook("BTC-EUR", "1").getAsks().get(0).getPrice();
			Marketprice mp = new Marketprice(runId, ind, "BTC", btcAsk.floatValue());
			marketpriceRepository.save(mp);
		} catch (Exception e) {
			e.printStackTrace();

		}
		history.Add(btcAsk);
		BigDecimal down = btcAsk.subtract(history.MinElement());
		BigDecimal up = history.MaxElement().subtract(btcAsk);
		AvgOld = Avg;
		Avg = history.Avg();
		AvgPercentage = history.AvgLastPercentage(0.1);
		AskAvgSpread = AvgPercentage.subtract(Avg);
		LongShort = Avg.compareTo(AvgOld);

		// STOP - VALUES
		if (LongStatus == 1 && Avg.compareTo(LongStop) == 1) {
			LongStop = Avg;
		}
		if (ShortStatus == 1 && Avg.compareTo(ShortStop) == -1) {
			ShortStop = Avg;
		}

		// TRACING
		if (ind < 0) {

			log.info("LONG-SHORT: " + LongShort + ", Spread: " + AskAvgSpread + ", btcAsk: " + btcAsk.doubleValue()
					+ ", AvgP: " + AvgPercentage + ", Avg: " + Avg + ", AvgOld: " + AvgOld + ", Min: "
					+ down.doubleValue() + ", Max:  " + up.doubleValue() + ", Long-STOP: " + LongStop + ", Short-STOP: "
					+ ShortStop + " - Iteration: " + ind);
		}
		if (ind == 0) {
			log.info("LONG  TRADES - Status: " + LongStatus + ", Trades: " + LongTrades + ", book: "
					+ btcAsk.subtract(LongBuy) + ", Fix: " + LongStop.subtract(LongBuy) + ", Win: " + Long + ", Fees: "
					+ LongFees + ", Total: " + Long.subtract(LongFees) + ", Market: " + btcAsk.subtract(TradeStart)
					+ ", Value: " + LongBuy + ", Stop: " + LongStop + " -- " + ind);
			log.info("SHORT TRADES - Status: " + ShortStatus + ", Trades: " + ShortTrades + ", book: "
					+ ShortSell.subtract(btcAsk) + ", Fix: " + ShortSell.subtract(ShortStop) + ", Win: " + Short
					+ ", Fees: " + ShortFees + ", Total: " + Short.subtract(ShortFees) + ", Market: "
					+ TradeStart.subtract(btcAsk) + ", Value: " + ShortSell + ", Stop: " + ShortStop + " -- " + ind);
		}

		// LONG - TRADES
		if (longMarket && ind > historyLength && LongStatus == 0 && ShortStatus == 0 && LongShort == 1
				&& AskAvgSpread.longValue() > spread) {
			transactionId = UUID.randomUUID().toString();
			LongStatus = 1;
			LongTrades = LongTrades + 1;
			LongBuy = btcAsk;
			LongStop = Avg;
			fee = createMarketOrderBuy();
			LongFees = LongFees.add(fee);
			transaction = new Trade(transactionId, runId, "LONG", "BTC");
			transaction.setIndBuy(ind);
			transaction.setPriceBuy(btcAsk.floatValue());
			transaction.setFeeBuy(fee.floatValue());
			log.info("++ Long-BUY: " + LongBuy + ", Long-STOP: " + LongStop);
			log.info("++ LONG-SHORT: " + LongShort + ", Spread: " + AskAvgSpread + ", btcAsk: " + btcAsk.doubleValue()
					+ ", Avg: " + Avg + ", AvgOld: " + AvgOld + ", Min: " + down.doubleValue() + ", Max:  "
					+ up.doubleValue() + ", Long-STOP: " + LongStop + ", Short-STOP: " + ShortStop + " - Iteration: "
					+ ind);
		}
		if (LongStatus == 1 && LongShort == -1 && btcAsk.compareTo(LongStop) == -1) {
			LongStatus = 0;
			LongTrades = LongTrades + 1;
			LongSell = btcAsk;
			Long = Long.add(btcAsk.subtract(LongBuy));
			LongStop = BigDecimal.valueOf(0);
			fee = createMarketOrderSell();
			LongFees = LongFees.add(fee);
			transaction.setIndSell(ind);
			transaction.setPriceSell(btcAsk.floatValue());
			transaction.setFeeSell(ind);
			tradeRepository.save(transaction);
			log.info("++ Long-SOLD: " + LongSell + ", RESULT = " + LongSell.subtract(LongBuy) + ", Long-SubTotal: "
					+ Long + ", Long-Fees: " + LongFees + ", Long-TOTAL: " + Long.subtract(LongFees));
			log.info("++ LONG-SHORT: " + LongShort + ", Spread: " + AskAvgSpread + ", btcAsk: " + btcAsk.doubleValue()
					+ ", Avg: " + Avg + ", AvgOld: " + AvgOld + ", Min: " + down.doubleValue() + ", Max:  "
					+ up.doubleValue() + ", Long-STOP: " + LongStop + ", Short-STOP: " + ShortStop + " - Iteration: "
					+ ind);
			log.info("## LONG  TRADES - Status: " + LongStatus + ", Trades: " + LongTrades + ", book: "
					+ btcAsk.subtract(LongBuy) + ", Fix: " + LongStop.subtract(LongBuy) + ", Win: " + Long + ", Fees: "
					+ LongFees + ", Total: " + Long.subtract(LongFees) + ", Market: " + btcAsk.subtract(TradeStart)
					+ ", Value: " + LongBuy + ", Stop: " + LongStop + " -- " + ind);
			log.info("## SHORT TRADES - Status: " + ShortStatus + ", Trades: " + ShortTrades + ", book: "
					+ ShortSell.subtract(btcAsk) + ", Fix: " + ShortSell.subtract(ShortStop) + ", Win: " + Short
					+ ", Fees: " + ShortFees + ", Total: " + Short.subtract(ShortFees) + ", Market: "
					+ TradeStart.subtract(btcAsk) + ", Value: " + ShortSell + ", Stop: " + ShortStop + " -- " + ind);
		}
		// SHORT - TRADES
		if (shortMarket && ind > historyLength && ShortStatus == 0 && LongStatus == 0 && LongShort == -1
				&& AskAvgSpread.longValue() < (-1.0) * spread) {
			transactionId = UUID.randomUUID().toString();
			ShortStatus = 1;
			ShortTrades = ShortTrades + 1;
			ShortSell = btcAsk;
			ShortStop = Avg;
			fee = createMarketOrderSell();
			ShortFees = ShortFees.add(fee);
			transaction = new Trade(transactionId, runId, "SHORT", "BTC");
			transaction.setIndSell(ind);
			transaction.setPriceSell(btcAsk.floatValue());
			transaction.setFeeSell(fee.floatValue());
			log.info("-- Short-SELL: " + ShortSell + ", Short-STOP: " + ShortStop);
			log.info("-- LONG-SHORT: " + LongShort + ", Spread: " + AskAvgSpread + ", btcAsk: " + btcAsk.doubleValue()
					+ ", Avg: " + Avg + ", AvgOld: " + AvgOld + ", Min: " + down.doubleValue() + ", Max:  "
					+ up.doubleValue() + ", Long-STOP: " + LongStop + ", Short-STOP: " + ShortStop + " - Iteration: "
					+ ind);
		}
		if (ShortStatus == 1 && LongShort == 1 && btcAsk.compareTo(ShortStop) == 1) {
			ShortStatus = 0;
			ShortTrades = ShortTrades + 1;
			ShortBuy = btcAsk;
			Short = Short.add(ShortSell.subtract(ShortBuy));
			fee = createMarketOrderBuy();
			ShortFees = ShortFees.add(fee);
			transaction.setIndBuy(ind);
			transaction.setPriceBuy(btcAsk.floatValue());
			transaction.setFeeBuy(ind);
			tradeRepository.save(transaction);

			ShortStop = BigDecimal.valueOf(0);
			log.info(
					"-- Short-BOUGHT: " + ShortBuy + ", RESULT = " + ShortSell.subtract(ShortBuy) + ", Short-SubTotal: "
							+ Short + ", Short-Fees: " + ShortFees + ", Short-TOTAL: " + Short.subtract(ShortFees));
			log.info("-- LONG-SHORT: " + LongShort + ", Spread: " + AskAvgSpread + ", btcAsk: " + btcAsk.doubleValue()
					+ ", Avg: " + Avg + ", AvgOld: " + AvgOld + ", Min: " + down.doubleValue() + ", Max:  "
					+ up.doubleValue() + ", Long-STOP: " + LongStop + ", Short-STOP: " + ShortStop + " - Iteration: "
					+ ind);
			log.info("## LONG  TRADES - Status: " + LongStatus + ", Trades: " + LongTrades + ", book: "
					+ btcAsk.subtract(LongBuy) + ", Fix: " + LongStop.subtract(LongBuy) + ", Win: " + Long + ", Fees: "
					+ LongFees + ", Total: " + Long.subtract(LongFees) + ", Market: " + btcAsk.subtract(TradeStart)
					+ ", Value: " + LongBuy + ", Stop: " + LongStop + " -- " + ind);
			log.info("## SHORT TRADES - Status: " + ShortStatus + ", Trades: " + ShortTrades + ", book: "
					+ ShortSell.subtract(btcAsk) + ", Fix: " + ShortSell.subtract(ShortStop) + ", Win: " + Short
					+ ", Fees: " + ShortFees + ", Total: " + Short.subtract(ShortFees) + ", Market: "
					+ TradeStart.subtract(btcAsk) + ", Value: " + ShortSell + ", Stop: " + ShortStop + " -- " + ind);
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
