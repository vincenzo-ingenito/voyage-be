package it.voyage.ms.dto.response;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PointDTO {
	private String name;
	private String type;
	private String description;
	private String cost;
	private Coords coord;
	private String country; // Aggiunto
	private String region;  // Aggiunto
	private String city;    // Aggiunto

	// Getters and Setters...
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCost() {
		return cost;
	}

	public void setCost(String cost) {
		this.cost = cost;
	}

	public Coords getCoord() {
		return coord;
	}

	public void setCoord(Coords coord) {
		this.coord = coord;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}
}