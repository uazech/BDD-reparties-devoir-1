package com.crawler.repository.sqlite;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.crawler.modele.SortBO;

public interface SortMongoRepository extends MongoRepository<SortBO, String> {

	public List<SortBO> findAll();

}