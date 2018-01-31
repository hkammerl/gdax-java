package com.coinbase.exchange.api.gui;

import java.awt.List;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class History {
	static final Logger log = LoggerFactory.getLogger(History.class);

	ArrayList<BigDecimal> history;
	int elements;

	public History(int e) {
		history = new ArrayList<BigDecimal>();
		elements = e;
	}

	public void Add(BigDecimal p) {
		history.add(p);
		if (history.size() > elements) {
			history.remove(0);
		}
		//log.info(history.toString());
	}

	public BigDecimal LastElement() {
		return (history.get(history.size() - 1));
	}

	public BigDecimal MaxElement() {
		BigDecimal max = BigDecimal.valueOf(-10000000.0);
		for (int ind = 0; ind < history.size(); ind++) {
			if (history.get(ind).compareTo(max) == 1) {
				max = history.get(ind);
			}
		}
		return max;
	}

	public BigDecimal MinElement() {
		BigDecimal min = BigDecimal.valueOf(10000000.0);
		for (int ind = 0; ind < history.size(); ind++) {
			if (history.get(ind).compareTo(min) == -1) {
				min = history.get(ind);
			}
		}
		return min;
	}

	public BigDecimal Avg() {
		BigDecimal sum = BigDecimal.valueOf(0.0);
		for (int ind = 0; ind < history.size(); ind++) {
			sum = sum.add(history.get(ind));
		}
		return sum.divide(BigDecimal.valueOf(history.size()),1);
	}
	
	public int Size() {
		return history.size();
	}

}
