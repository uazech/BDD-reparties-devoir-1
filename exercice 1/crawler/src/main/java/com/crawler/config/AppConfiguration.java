package com.crawler.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.crawler.modele.SortBO;
import com.crawler.step.crawl.DataProcessor;
import com.crawler.step.crawl.SortItemReader;
import com.crawler.step.crawl.SortItemWriter;
import com.crawler.step.sqlite.SqliteTasklet;

@Configuration
@EnableBatchProcessing
public class AppConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	private SqliteTasklet sqliteTasklet;

	@Value("${crawler.read.commit-interval}")
	private int COMMIT_INTERVAL;

	@Bean
	public SortItemReader reader() {
		return new SortItemReader();
	}

	@Bean
	public DataProcessor processor() {
		return new DataProcessor();
	}

	@Bean
	public SortItemWriter writer() {

		return new SortItemWriter();
	}

	@Primary
	@Bean
	public Job CrawlJob(Step crawl) {
		return jobBuilderFactory.get("crawl").incrementer(new RunIdIncrementer()).flow(crawlChunkStep()).end().build();
	}

	@Bean
	public Job job() {
		return jobBuilderFactory.get("sqlite").incrementer(new RunIdIncrementer()).start(sqliteStep()).build();
	}

	@Bean
	public Step sqliteStep() {
		return stepBuilderFactory.get("sqlite").tasklet(sqliteTasklet).build();
	}

	@Bean
	@Primary
	public Step crawlChunkStep() {
		return stepBuilderFactory.get("crawlChunk").<SortBO, SortBO>chunk(COMMIT_INTERVAL).reader(reader())
				.writer(writer()).build();
	}

}