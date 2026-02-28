package com.cricket.engine;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.*;

/**
 * Persists team selections to saved_teams.csv.
 *
 * File format (one row per player slot):
 *   teamName, playerName
 *
 * Example:
 *   India,RG Sharma
 *   India,V Kohli
 *   Australia,SPD Smith
 */
public class SavedTeamsStore {

    private static final String FILE_PATH = "saved_teams.csv";

    /**
     * Save a team. Overwrites any existing entry with the same name.
     */
    public void saveTeam(String teamName, List<String> players) throws Exception {
        // Load all existing teams first
        Map<String, List<String>> all = loadAll();
        all.put(teamName, new ArrayList<>(players));
        writeAll(all);
    }

    /**
     * Load all saved teams. Returns map of teamName → player list.
     */
    public Map<String, List<String>> loadAll() {
        Map<String, List<String>> teams = new LinkedHashMap<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return teams;

        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 2) continue;
                String teamName  = line[0].trim();
                String playerName = line[1].trim();
                if (!teamName.isBlank() && !playerName.isBlank()) {
                    teams.computeIfAbsent(teamName, k -> new ArrayList<>())
                         .add(playerName);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not read saved_teams.csv: " + e.getMessage());
        }
        return teams;
    }

    /**
     * Returns just the team names for the dropdown.
     */
    public List<String> getSavedTeamNames() {
        return new ArrayList<>(loadAll().keySet());
    }

    /**
     * Load a specific team by name.
     */
    public List<String> loadTeam(String teamName) {
        return loadAll().getOrDefault(teamName, new ArrayList<>());
    }

    /**
     * Delete a saved team.
     */
    public void deleteTeam(String teamName) throws Exception {
        Map<String, List<String>> all = loadAll();
        all.remove(teamName);
        writeAll(all);
    }

    private void writeAll(Map<String, List<String>> teams) throws Exception {
        try (CSVWriter writer = new CSVWriter(new FileWriter(FILE_PATH))) {
            for (Map.Entry<String, List<String>> entry : teams.entrySet()) {
                for (String player : entry.getValue()) {
                    writer.writeNext(new String[]{ entry.getKey(), player });
                }
            }
        }
    }
}