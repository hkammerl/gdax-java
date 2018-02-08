package com.coinbase.exchange.api.mysql;

import org.springframework.data.repository.CrudRepository;

import com.coinbase.exchange.api.mysql.Marketprice;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface MarketpriceRepository extends CrudRepository<Marketprice, Long> {

}
