package com.crawler.step.crawl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import com.crawler.modele.SortBO;
import com.crawler.repository.sqlite.SortMongoRepository;

public class SortItemWriter implements ItemWriter<SortBO> {

	private static final Logger LOG = LoggerFactory.getLogger(SortItemWriter.class);

	@Autowired
	private SortMongoRepository sortRepository;

	@Override
	public void write(List<? extends SortBO> arg0) throws Exception {
		LOG.debug("writing");
		for (SortBO sort : arg0) {
			if (sort.getName() != null)
				sortRepository.save(sort);
		}
	}

}