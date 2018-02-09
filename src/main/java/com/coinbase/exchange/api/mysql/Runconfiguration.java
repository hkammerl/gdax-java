package com.coinbase.exchange.api.mysql;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity // This tells Hibernate to make a table out of this class
public class Runconfiguration {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;
    String timeStamp;
    private String runId;
    int historyLength;
    double historyPercentage;
    int spread;

    public Runconfiguration(String runId, int historyLength, double historyPercentage, int spread) { 
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
    	setRunId(runId);
    	setTimeStamp(timeStamp);
    	setHistoryLength(historyLength);
    	setHistoryPercentage(historyPercentage);
    	setSpread(spread);
    }
    
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getHistoryLength() {
		return historyLength;
	}

	public void setHistoryLength(int historyLength) {
		this.historyLength = historyLength;
	}

	public double getHistoryPercentage() {
		return historyPercentage;
	}

	public void setHistoryPercentage(double historyPercentage) {
		this.historyPercentage = historyPercentage;
	}

	public int getSpread() {
		return spread;
	}

	public void setSpread(int spread) {
		this.spread = spread;
	}
	

	public String getRunId() {
		return runId;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}
	
	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	

}