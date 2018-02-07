package com.coinbase.exchange.api.gui;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* SQL Schema und Tabellen Generator
CREATE DATABASE `gdax` !40100 DEFAULT CHARACTER SET utf8 ;
CREATE TABLE `trading` (
  `RUN_ID` varchar(200) DEFAULT NULL,
  `IND` int(11) DEFAULT NULL,
  `TIMESTAMP` timestamp(6) NULL DEFAULT NULL,
  `CURRENCY` varchar(10) DEFAULT NULL,
  `PRICE` float DEFAULT NULL,
  `LONG_TRANSACTION_ID` varchar(200) DEFAULT NULL,
  `LONG_BUY_SELL` varchar(5) DEFAULT NULL,
  `SHORT_TRANSACTION_ID` varchar(200) DEFAULT NULL,
  `SHORT_BUY_SELL` varchar(5) DEFAULT NULL,
  `FEE` float DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
*/

public class MySql {
	static final Logger log = LoggerFactory.getLogger(PriceTracker.class);
	String myDriver = "com.mysql.jdbc.Driver";
	String myUrl = "jdbc:mysql://localhost/gdax";
	Connection conn;

	void OpenDB() {
		try {
			Class.forName(myDriver);
			conn = DriverManager.getConnection(myUrl, "root", "dorabald");

		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());
		}

	}

	void CloseDB() {
		try {
			Statement st = conn.createStatement();
			st.close();
		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());
		}

	}

	void InsertRunSet(String runId, int history, int spread) {
		try {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
			Statement st = conn.createStatement();
			String query = String.format(Locale.US, "insert into runset values (\"%s\", \"%s\", %d, %d)",
					runId, timeStamp, history, spread);
			st.executeUpdate(query);
		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());
		}
	}
	
	
	void InsertPrice(String runId, int index, String currency, float price) {
		try {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
			Statement st = conn.createStatement();
			String query = String.format(Locale.US, "insert into history values (\"%s\", %d, \"%s\", \"%s\", %f)",
					runId, index, timeStamp, currency, price);
			st.executeUpdate(query);
		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());
		}
	}

	void TransactionLongBuy(String TransactionId, String runId, int indBuy, float priceBuy, float feeBuy) {
		try {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());

			Statement st = conn.createStatement();
			String query = String.format(Locale.US,
					"insert into transaction values (\"%s\", \"%s\", \"LONG\", \"%s\", %d, null, \"%s\", null, %f, null, %f, null)",
					TransactionId, timeStamp, runId, indBuy, timeStamp, priceBuy, feeBuy);
			log.info(query);
			st.executeUpdate(query);
		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());
		}
	}
	
	void TransactionLongSell(String TransactionId, String runId, int indSell, float priceSell, float feeSell) {
		try {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());

			Statement st = conn.createStatement();
			String query = String.format(Locale.US,
					"update transaction set ind_sell = %d, timestamp_sell = \"%s\", price_sell = %f, fee_sell = %f where transaction_id = \"%s\"", 
					indSell, timeStamp, priceSell, feeSell, TransactionId);
			log.info(query);
			st.executeUpdate(query);
		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());
		}
	}

	void TransactionShortSell(String TransactionId, String runId, int indSell, float priceSell, float feeSell) {
		try {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());

			Statement st = conn.createStatement();
			String query = String.format(Locale.US,
					"insert into transaction values (\"%s\", \"%s\", \"SHORT\", \"%s\", null, %d, null, \"%s\", null, %f, null, %f)",
					TransactionId, timeStamp, runId, indSell, timeStamp, priceSell, feeSell);
			log.info(query);
			st.executeUpdate(query);
		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());
		}

	}
	
	void TransactionShortBuy(String TransactionId, String runId, int indBuy, float priceBuy, float feeBuy) {
		try {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());

			Statement st = conn.createStatement();
			String query = String.format(Locale.US,
					"update transaction set ind_buy = %d, timestamp_buy = \"%s\", price_buy = %f, fee_buy = %f where transaction_id = \"%s\"", 
					indBuy, timeStamp, priceBuy, feeBuy, TransactionId);
			log.info(query);
			st.executeUpdate(query);
		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());
		}
	}

}