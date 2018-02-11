package com.coinbase.exchange.api.mysql;

	import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.persistence.Entity;
	import javax.persistence.GeneratedValue;
	import javax.persistence.GenerationType;
	import javax.persistence.Id;

	@Entity // This tells Hibernate to make a table out of this class
	public class Trade {
	    @Id
	    @GeneratedValue(strategy=GenerationType.AUTO)
	    private Integer id;
	    private String runId;
	    private String transactionId;
	    private String transactionType;
	    private int indBuy;
	    private int indSell;
	    private float priceBuy;
	    private float priceSell;
	    private float feeBuy;
	    private float feeSell;
	    private String currency;
	    private String timeStamp;
	    private int idBuy;
	    private int idSell;

	    public Trade (String transactionId, String runId, String transactionType, String currency) {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
	    	setRunId(runId);
	    	setTimeStamp(timeStamp);
	    	setCurrency(currency);
	    	setTransactionId(transactionId);
	    	setCurrency(currency);
	    	setTransactionType(transactionType);
	    }
	    
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getRunId() {
			return runId;
		}

		public void setRunId(String runId) {
			this.runId = runId;
		}

		public String getTransactionId() {
			return transactionId;
		}

		public void setTransactionId(String transactionId) {
			this.transactionId = transactionId;
		}
		
		public String getTransactionType() {
			return transactionType;
		}

		public void setTransactionType(String transactionType) {
			this.transactionType = transactionType;
		}
		
		public int getIndBuy() {
			return indBuy;
		}

		public void setIndBuy(int ind) {
			this.indBuy = ind;
		}
		
		public int getIndSell() {
			return indSell;
		}

		public void setIndSell(int ind) {
			this.indSell = ind;
		}

		public String getTimeStamp() {
			return timeStamp;
		}

		public void setTimeStamp(String timeStamp) {
			this.timeStamp = timeStamp;
		}

		public String getCurrency() {
			return currency;
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}

		public float getPriceBuy() {
			return priceBuy;
		}

		public void setPriceBuy(float price) {
			this.priceBuy = price;
		}
		
		public float getPriceSell() {
			return priceSell;
		}

		public void setPriceSell(float price) {
			this.priceSell = price;
		}
		
		public float getFeeBuy() {
			return feeBuy;
		}

		public void setFeeBuy(float fee) {
			this.feeBuy = fee;
		}

		public float getFeeSell() {
			return feeSell;
		}

		public void setFeeSell(float fee) {
			this.feeSell = fee;
		}
		
		public int getIdBuy() {
			return idBuy;
		}

		public void setIdBuy(int id) {
			this.idBuy = id;
		}
		public int getIdSell() {
			return idSell;
		}

		public void setIdSell(int id) {
			this.idSell = id;
		}

	
	}
