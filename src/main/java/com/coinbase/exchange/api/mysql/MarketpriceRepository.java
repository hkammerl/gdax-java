package com.coinbase.exchange.api.mysql;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.coinbase.exchange.api.mysql.Marketprice;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface MarketpriceRepository extends CrudRepository<Marketprice, Long> {
	
    @Query(value = "SELECT * FROM marketprice mp WHERE " +
            "mp.run_id = :runId and mp.ind = :ind",
            nativeQuery = true
    )
    List<Marketprice> findByRunIdAndInd(@Param("runId") String runId, @Param("ind") int ind);


}
