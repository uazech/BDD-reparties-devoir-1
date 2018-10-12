package com.crawler.modele;

import java.util.List;

import org.springframework.data.annotation.Id;

public class SortBO {

	@Id
	private int id;

	private String name;

	private List<String> components;

	private boolean spellResistance;

	private List<JobBO> jobs;

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isSpellResistance() {
		return spellResistance;
	}

	public void setSpellResistance(boolean spell_resistance) {
		this.spellResistance = spell_resistance;
	}

	public List<JobBO> getJobs() {
		return jobs;
	}

	public void setJobs(List<JobBO> jobs) {
		this.jobs = jobs;
	}

	public List<String> getComponents() {
		return components;
	}

	public void setComponents(List<String> components) {
		this.components = components;
	}

}
