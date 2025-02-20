package com.example.ltrynek.client.config;

import java.util.List;
import java.util.ArrayList;

public class ServerEntry {
    public List<String> domains = new ArrayList<>();
    public String profileName;

    public String loreRegex = "Cena: (\\d+)";
    public String highlightColor = "#80FF00";
    public String highlightColorStack = "#FF8000";


    public List<PriceEntry> prices = new ArrayList<>();
}
