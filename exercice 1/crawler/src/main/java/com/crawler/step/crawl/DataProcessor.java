package com.crawler.step.crawl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawler.modele.JobBO;
import com.crawler.modele.SortBO;

public class DataProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(DataProcessor.class);

	private static final String LEVEL_REGEX = "(?<=Level).+?(?=Casting)";
	private static final Pattern LEVEL_PATTERN = Pattern.compile(LEVEL_REGEX);

	private static final String SPELL_RESISTANCE_REGEX = "(?<= Spell Resistance ).+?(?= )";
	private static final Pattern SPELL_RESISTANCE_PATTERN = Pattern.compile(SPELL_RESISTANCE_REGEX);

	private static final String COMPONENTS_REGEX = "(?<= Components ).+?(?= Effect)";
	private static final Pattern COMPONENTS_PATTERN = Pattern.compile(COMPONENTS_REGEX);

	public static SortBO process(final String data, int idSort) {
		LOG.debug(data);
		SortBO sort = parserNiveauEtNomJob(data);
		if (sort != null) {
			sort.setId(idSort);
			sort.setName(recupererSortName(data));
			sort.setComponents(recupererComponents(data));
			sort.setSpellResistance(recupererSpellResistance(data));
			return sort;
		}
		return null;
	}

	private static List<String> recupererComponents(final String data) {
		List<String> components = new ArrayList<>();
		Matcher m = COMPONENTS_PATTERN.matcher(data);
		if (m.find()) {
			for (String component : m.group(0).split(", ")) {
				components.add(component);
			}
		}
		return components;
	}

	private static String recupererSortName(String data) {
		return data.split(";")[0];
	}

	private static boolean recupererSpellResistance(String data) {
		boolean spellResistance = false;
		Matcher m = SPELL_RESISTANCE_PATTERN.matcher(data);
		if (m.find()) {
			spellResistance = (m.group(0).equals("yes"));
		}
		return spellResistance;
	}

	private static SortBO parserNiveauEtNomJob(String data) {
		Matcher m = LEVEL_PATTERN.matcher(data);
		if (m.find()) {
			SortBO sort = new SortBO();
			sort.setJobs(new ArrayList<>());
			String[] levels = m.group(0).split(",");
			for (String level : levels) {
				try {
					String[] splitLevel = level.split(" ");
					JobBO job = new JobBO();
					job.setJobName(splitLevel[1]);
					try {
						job.setJobLevel(Integer.parseInt(splitLevel[2]));
						sort.getJobs().add(job);

					} catch (NumberFormatException ex) {
						LOG.debug(ex.toString());
					}
				} catch (ArrayIndexOutOfBoundsException ex) {
					LOG.debug(ex.getStackTrace().toString());
				}

			}
			return sort;
		}
		return new SortBO();
	}

}