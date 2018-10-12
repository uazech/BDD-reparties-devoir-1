package com.crawler.step.crawl;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;

import com.crawler.modele.SortBO;

public class SortItemReader implements ItemReader<SortBO> {

	private static final Logger LOG = LoggerFactory.getLogger(SortItemReader.class);
	private static final String URL = "http://www.dxcontent.com/SDB_SpellBlock.asp?SDBID=";

	private int idSort = 0;
	private int nbPagesVidesConsecutives = 0;

	@Override

	public SortBO read() {
		idSort++;
		LOG.info("Traitement du sort - " + idSort);
		String result = null;
		try {
			try {
				Document doc = Jsoup.connect(URL + idSort).timeout(0).get();
				Elements newsHeadlines = doc.select(".SpellDiv");
				result = newsHeadlines.get(0).toString();
				LOG.debug(result);
			} catch (IOException e) {
				e.printStackTrace();
			}

			String resultat = Jsoup.parse(result).text();
			nbPagesVidesConsecutives = 0;
			return DataProcessor.process(resultat, idSort);
		} catch (IndexOutOfBoundsException ex) {
			nbPagesVidesConsecutives++;
			LOG.warn("Page vide trouvée à l'index : " + idSort);
			if (nbPagesVidesConsecutives > 2) {
				return null;
			}
			return new SortBO();
		}

	}

}