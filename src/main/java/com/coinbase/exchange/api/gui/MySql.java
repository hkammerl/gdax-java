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
	
	void InsertPrice(String runId, int index, String currency, float price) {
		try {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());			
			Statement st = conn.createStatement();
				String query = String.format(Locale.US,
						"insert into trading values (\"%s\", %d, \"%s\", \"%s\", %f, null, null, null, null, null)",
						runId, index, timeStamp, currency, price);
				st.executeUpdate(query);
		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());
		}
	}

	void LogTransaction(int longShort, String runId, int index, String transactionId, String buySell, float fee) {
		try {
			String query = "";
			Statement st = conn.createStatement();
			if (longShort == 1) {
				query = String.format(Locale.US,
						"update trading set long_transaction_id = \"%s\", long_buy_sell = \"%s\", fee = %f where run_id = \"%s\" and ind = %d ", transactionId, buySell, fee, runId, index);
			}
			else {
				query = String.format(Locale.US,
						"update trading set short_transaction_id = \"%s\", short_buy_sell = \"%s\", fee = %f where run_id = \"%s\" and ind = %d ", transactionId, buySell, fee, runId, index);
			}
				log.info(query);
				st.executeUpdate(query);
		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());
		}
		
	}
}