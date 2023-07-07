package de.saar.coli.amtools.decomposition.analysis;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VulcanJSONWriter {

    Map<String, List<Object>> data;
    Map<String, String> name2type;

    public VulcanJSONWriter(Map<String, String> name2type) {

        this.name2type = name2type;
        this.data = new HashMap<>();
        for (String name: name2type.keySet()) {
            data.put(name, new ArrayList<>());
        }

    }

    public void addInstance(String name, Object instance) {
        data.get(name).add(instance);
    }


    public void writeJSON(String filePath) throws IOException {
        List<Map<String, Object>> dataObject = new ArrayList<>();
        for (String name: data.keySet()) {
            Map<String, Object> dataEntry = new HashMap<>();
            dataEntry.put("name", name);
            dataEntry.put("type", "data");
            dataEntry.put("format", name2type.get(name));
            dataEntry.put("instances", data.get(name));
            dataObject.add(dataEntry);
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(new File(filePath), dataObject);
    }

    public static void main(String[] args) throws IOException {
        HashMap<String, String> name2type = new HashMap<>();
        name2type.put("string", "string");
        name2type.put("tree", "nltk_tree_string");

        VulcanJSONWriter vulcan_writer = new VulcanJSONWriter(name2type);

        vulcan_writer.addInstance("string", "hello world");
        vulcan_writer.addInstance("tree", "(S (NP (DT the) (NN cat)) (VP (VBD ate) (NP (DT a) (NN cookie))))");

        vulcan_writer.addInstance("string", "hello to you too");
        vulcan_writer.addInstance("tree", "(S (NP (DT the) (NN cat)) (VP (VBD ate) (NP (DT another) (NN cookie))))");

        vulcan_writer.writeJSON("test.json");
    }

}

