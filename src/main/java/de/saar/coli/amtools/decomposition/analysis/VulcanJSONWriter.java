package de.saar.coli.amtools.decomposition.analysis;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.saar.basic.Pair;
import de.saar.coli.amrtagging.Alignment;
import de.saar.coli.amrtagging.AmConllEntry;
import de.saar.coli.amrtagging.AmConllSentence;
import de.up.ling.irtg.algebra.graph.AMDependencyTree;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VulcanJSONWriter {

    Map<String, List<Object>> data;
    Map<String, String> name2type;
    Map<String, Map<String, List<List<Pair<String, String>>>>> linkers;

    public VulcanJSONWriter(Map<String, String> name2type) {

        this.name2type = name2type;
        this.data = new HashMap<>();
        for (String name: name2type.keySet()) {
            data.put(name, new ArrayList<>());
        }
        linkers = new HashMap<>();

    }

    public void initializeAlignmentLinkers(String graphName, String alignmentName, String sentenceName) {
        // add linker pairs alignment-graph and alignment-sentence and sentence-graph
        linkers.put(alignmentName, new HashMap<>());
        linkers.get(alignmentName).put(graphName, new ArrayList<>());
        linkers.get(alignmentName).put(sentenceName, new ArrayList<>());
        linkers.put(sentenceName, new HashMap<>());
        linkers.get(sentenceName).put(graphName, new ArrayList<>());
    }

    public void addInstance(String name, Object instance) {
        data.get(name).add(instance);
    }

    public void addAMTree(String name, AMDependencyTree amTree) {
        if (amTree == null) {
            AmConllSentence amConllSentence = makeDummyAmConllSentence();
            data.get(name).add(amConllSentence.toString());
        } else {
            try {
                AmConllSentence amConllSentence = new AmConllSentence();
                addAmConllEntriesRecursive(amTree, AmConllEntry.ROOT_SYM, amConllSentence, 0);
                data.get(name).add(amConllSentence.toString());
            } catch (Exception e) {
//                System.err.println("Minor warning: mal-formed AM dependency tree encountered when writing Vulcan JSON, " +
//                        "writing dummy graph instead. " +
//                        "This is to some extent expected. Have a closer look if you are using Vulcan for analysis, " +
//                        "and you are finding too many dummy graphs.");
                AmConllSentence amConllSentence = makeDummyAmConllSentence();
                data.get(name).add(amConllSentence.toString());
            }
        }
    }

    @NotNull
    private static AmConllSentence makeDummyAmConllSentence() {
        AmConllSentence amConllSentence = new AmConllSentence();
        AmConllEntry entry = new AmConllEntry(1, "null");
        entry.setAligned(false);
        entry.setHead(0);
        entry.setEdgeLabel(AmConllEntry.ROOT_SYM);
        entry.setDelexSupertag("(n / null)");
        amConllSentence.add(entry);
        return amConllSentence;
    }

    private void addAmConllEntriesRecursive(AMDependencyTree amTree, String operation,
                                            AmConllSentence amConllSentence, int parentID) {
        int thisID = amConllSentence.size() + 1;
        AmConllEntry entry = new AmConllEntry(thisID, "null");
        entry.setAligned(false);
        entry.setHead(parentID);
        entry.setEdgeLabel(operation);
        entry.setDelexSupertag(amTree.getHeadGraph().left.toIsiAmrStringWithSources());
        amConllSentence.add(entry);
        for (Pair<String, AMDependencyTree> operationAndChild: amTree.getOperationsAndChildren()) {
            addAmConllEntriesRecursive(operationAndChild.right, operationAndChild.left, amConllSentence, thisID);
        }
    }

    public void addAlignmentLinkers(String graphName, String alignmentName, String sentenceName,
                                    List<Alignment> alignments) {
        List<Pair<String, String>> alignmentGraphLinkerList = new ArrayList<>();
        linkers.get(alignmentName).get(graphName).add(alignmentGraphLinkerList);
        List<Pair<String, String>> alignmentSentenceLinkerList = new ArrayList<>();
        linkers.get(alignmentName).get(sentenceName).add(alignmentSentenceLinkerList);
        List<Pair<String, String>> sentenceGraphLinkerList = new ArrayList<>();
        linkers.get(sentenceName).get(graphName).add(sentenceGraphLinkerList);
        int alignmentID = 0;
        for (Alignment alignment: alignments) {
            Collection<String> nodenames = alignment.nodes;
            List<Integer> tokenIDs = new ArrayList<>();
            for (int i = alignment.span.start; i < alignment.span.end; i++) {
                tokenIDs.add(i);
            }
            for (String nn : nodenames) {
                alignmentGraphLinkerList.add(new Pair<>(Integer.toString(alignmentID), nn));
                for (int tokenID : tokenIDs) {
                    sentenceGraphLinkerList.add(new Pair<>(Integer.toString(tokenID), nn));
                }
            }
            for (int tokenID : tokenIDs) {
                alignmentSentenceLinkerList.add(new Pair<>(Integer.toString(alignmentID),
                        Integer.toString(tokenID)));
            }
            alignmentID++;
        }
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
        for (String name: linkers.keySet()) {
            for (String name2 : linkers.get(name).keySet()) {
                List<Map<String, Map<String, Float>>> scoreList = new ArrayList<>();
                for (List<Pair<String, String>> alignedNamesList : linkers.get(name).get(name2)) {
                    Map<String, Map<String, Float>> scores = new HashMap<>();
                    scoreList.add(scores);
                    for (Pair<String, String> alignedNames : alignedNamesList) {
                        if (!scores.containsKey(alignedNames.left)) {
                            scores.put(alignedNames.left, new HashMap<>());
                        }
                        scores.get(alignedNames.left).put(alignedNames.right, 1.0f);
                    }
                }
                Map<String, Object> dataEntry = new HashMap<>();
                dataEntry.put("name1", name);
                dataEntry.put("name2", name2);
                dataEntry.put("type", "linker");
                dataEntry.put("scores", scoreList);
                dataObject.add(dataEntry);
            }
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

