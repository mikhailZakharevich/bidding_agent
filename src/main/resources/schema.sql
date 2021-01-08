-- sites
CREATE TABLE sites (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	exchange_id INT NOT NULL,
	exchange_site_id INT NOT NULL,
	domain VARCHAR(128) NOT NULL
);
CREATE UNIQUE INDEX sites_domain_idx ON sites (domain);

-- sites_placements
CREATE TABLE sites_placements (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	site_id INT NOT NULL,
	tag_id VARCHAR(128) NOT NULL,
	FOREIGN KEY (site_id) REFERENCES sites(id) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE UNIQUE INDEX sites_placements_site_id_idx ON sites_placements (site_id, tag_id);

-- segments
CREATE TABLE segments (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	site_id INT NOT NULL,
	placement_id INT NOT NULL,
	FOREIGN KEY (placement_id) REFERENCES sites_placements(id) ON UPDATE CASCADE ON DELETE RESTRICT,
	FOREIGN KEY (site_id) REFERENCES sites(id) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE UNIQUE INDEX segments_site_id_idx ON segments (site_id, placement_id);
