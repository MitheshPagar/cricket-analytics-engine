package com.cricket;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import com.opencsv.CSVReader;

public class PlayerRoleLoader {
    
    private Map<String, String> batRoleMap = new HashMap<>();
    private Map<String, String> bowlRoleMap = new HashMap<>();

    public void load(String filePath) throws Exception {

    try (CSVReader reader = new CSVReader(new FileReader(filePath))) {

        String[] line;
        reader.readNext(); // skip header

        while ((line = reader.readNext()) != null) {

            if (line.length < 4) continue;

            String player = line[1].trim();
            String batRole = line[2].trim();
            String bowlRole = line[3].trim();   // ✅ third column

            batRoleMap.put(player, batRole);
            bowlRoleMap.put(player, bowlRole);
        }
    }
}

    public String getBatRole(String player){
        return batRoleMap.getOrDefault(player, "");
    }

    public String getBowlRole(String player){
        return bowlRoleMap.getOrDefault(player, "");
    }

    public boolean contains(String player){
        return batRoleMap.containsKey(player);
    }
}
