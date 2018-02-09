package com.coinbase.exchange.api.mysql;

	import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.persistence.Entity;
	import javax.persistence.GeneratedValue;
	import javax.persistence.GenerationType;
	import javax.persistence.Id;

	@Entity // This tells Hibernate to make a table out of this class
	public class Marketprice {
	    @Id
	    @GeneratedValue(strategy=GenerationType.AUTO)
	    private Integer id;
	    private String runId;
	    private int ind;
	    private String timeStamp;
	    private String currency;
	    private float price;

	    public Marketprice (String runId, int ind, String currency, float price) {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
	    	setRunId(runId);
	    	setInd(ind);
	    	setTimeStamp(timeStamp);
	    	setCurrency(currency);
	    	setPrice(price);
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

		public int getInd() {
			return ind;
		}

		public void setInd(int ind) {
			this.ind = ind;
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

		public float getPrice() {
			return price;
		}

		public void setPrice(float price) {
			this.price = price;
		}

}
