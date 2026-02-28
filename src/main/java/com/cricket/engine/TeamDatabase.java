package com.cricket.engine;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;

/**
 * Loads all players from playerRoles.csv and provides search/filter access.
 * Singleton-style — load once, reuse everywhere.
 */
public class TeamDatabase {

    private final List<PlayerRecord> allPlayers = new ArrayList<>();

    public void load(String filePath) throws Exception {
        allPlayers.clear();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            reader.readNext(); // skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 4) continue;
                String name     = line[1].trim();
                String batRole  = line[2].trim();
                String bowlRole = line[3].trim();
                if (!name.isBlank()) {
                    allPlayers.add(new PlayerRecord(name, batRole, bowlRole));
                }
            }
        }
        // Sort alphabetically by name
        allPlayers.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    /** All players, sorted A-Z. */
    public List<PlayerRecord> getAll() {
        return Collections.unmodifiableList(allPlayers);
    }

    /**
     * Search by name — case-insensitive, matches anywhere in name.
     * Returns all players if query is blank.
     */
    public List<PlayerRecord> search(String query) {
        if (query == null || query.isBlank()) return getAll();
        String q = query.trim().toLowerCase();
        return allPlayers.stream()
                .filter(p -> p.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public PlayerRecord findByName(String name) {
        return allPlayers.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public int size() { return allPlayers.size(); }
}