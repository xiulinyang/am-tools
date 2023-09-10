package de.saar.coli.amtools.decomposition.formalisms.toolsets;

import com.google.gson.Gson;
import de.saar.coli.amrtagging.Alignment;
import de.saar.coli.amrtagging.MRInstance;
import de.saar.coli.amtools.decomposition.formalisms.DRTEdgeAttachmentHeuristic;
import de.saar.coli.amtools.decomposition.formalisms.EdgeAttachmentHeuristic;
import de.up.ling.irtg.algebra.graph.GraphEdge;
import de.up.ling.irtg.algebra.graph.GraphNode;
import de.up.ling.irtg.algebra.graph.SGraph;
import org.apache.commons.lang.ObjectUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class GraphbankDecompositionDRT extends GraphbankDecompositionToolset {
    //    public static List<String> getFilePaths(String directory, String extension) throws Exception {
//        // specify the directory to search in
//        Path dirPath = Paths.get(directory);
//
//        // use Files.walk() to search for files recursively and collect the paths of files that match the pattern
//        try (Stream<Path> paths = Files.walk(dirPath)) {
//            Predicate<Path> predicate = path -> path.toString().endsWith(extension);
//            return paths.filter(predicate)
//                    .map(Path::toString)
//                    .collect(Collectors.toList());
//        }
//    }
    public GraphbankDecompositionDRT(Boolean fasterModeForTesting) {
        super(fasterModeForTesting);
    }
    public static List<String> readParentDirectories(String filePath, String stemDirectory) throws IOException {
        List<String> parentDirectories = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        List<String> combinedDirectories = new ArrayList<>();
        for (String parentDirectory : parentDirectories) {
            String combinedDirectory = "/Users/shirleenyoung/Desktop/TODO/MA_Thesis/ud-boxer/data/pmb-4.0.0/data/en/"+ parentDirectory + "/" + stemDirectory;
            combinedDirectories.add(combinedDirectory);
        }

        return combinedDirectories;
    }
    public static List<String> getDirecotries(String filePath) throws IOException {
        List<String> parentDirectories = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        return parentDirectories;
    }


    public SGraph buildGraph(String Path) throws FileNotFoundException {
        List<MRInstance> returnedCorpus = new ArrayList();
        Map<String, GraphNode> nodeMap = new HashMap<>();
        List<List<String>> edgeList = new ArrayList<>();

        SGraph g = new SGraph();
        try {
            BufferedReader in = new BufferedReader(new FileReader(Path));
            String str;
            while ((str = in.readLine()) != null) {
                if (str.startsWith("(")) {
                    String[] line = str.substring(1, str.length() - 1).split(", ");
                    line[0] = line[0].substring(1, line[0].length() - 1).replace("'", "");
                    line[1] = line[1].substring(2, line[1].length() - 1).replace("'", "");
                    line[2] = line[2].substring(1, line[2].length() - 1).replace("'", "").replace("\"", "");
                    if (line[1].contains("instance")) {
                        if (line[0].contains("b0")) {
                            g.addSource("root", line[0]);
                        }
                        GraphNode currentNode;
                        currentNode = g.addNode(line[0], line[2]);
                        nodeMap.put(line[0], currentNode);

//                        GraphNode currentNode;
//                        if (line[2].contains("c")) {
//                            currentNode = g.addAnonymousNode(line[2]);
//                            nodeMap.put(line[0], currentNode);
//                        } else {
//                            currentNode = g.addNode(line[0], line[2]);
//                            nodeMap.put(line[0], currentNode);
//                        }


//
                    }
                    else {
                        List<String> singleEdge = new ArrayList<>();
                        singleEdge.add(line[0]);
                        singleEdge.add(line[1]);
                        singleEdge.add(line[2]);
                        edgeList.add(singleEdge);
                    }


                }
            }

            for (List<String> edgeInfo : edgeList) {
                String currentNodeID = edgeInfo.get(0);
                String edgeLabel = edgeInfo.get(1);
                String targetNodeID = edgeInfo.get(2);

                GraphNode currentNode = nodeMap.get(currentNodeID);
                GraphNode targetNode = nodeMap.get(targetNodeID);
                if (currentNode == null || targetNode == null) {
                    System.out.println("Error: currentNode or targetNode is null.");
                    // Handle this situation, e.g., log an error, throw an exception, or skip adding the edge.
                } else {
                    System.out.println("Adding edge: " + currentNode + " -> " + targetNode + " with label " + edgeLabel);
                    GraphEdge edge = g.addEdge(currentNode, targetNode, edgeLabel);
                }
            }

//                GraphEdge edge = g.addEdge(currentNode, targetNode, edgeLabel);
//            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return g;
    }

    public List<String> tokenizeSentence(String Path) throws IOException {
        List<String> tokenizedSentence = new ArrayList<>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(Path));
            String str;
            while ((str = in.readLine()) != null) {

                if (str.startsWith("tokenized")) {
                    tokenizedSentence = Arrays.asList(str.substring(19).split("\\s+"));

                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tokenizedSentence;
    }

    public List<Alignment> createAlignment(String Path) throws FileNotFoundException {
        List<Alignment> allAlignmentsInTheSentence = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(Path));
            String str;
            Map<String, Object>[] alignment = new Map[0];
            while ((str = in.readLine()) != null) {
                if (str.startsWith("alignment:")) {
                    // Parse alignment info as list of dictionaries
                    String nodeInfo = str.substring(10);
                    allAlignmentsInTheSentence = new ArrayList<>();
                    Gson gson = new Gson();
                    alignment = gson.fromJson(nodeInfo, Map[].class);
                }

            }
            for (Map<String, Object> dict : alignment) {
                int index = 0;
                Object itemIndex = dict.get("token_id");
                if (itemIndex instanceof Integer) {
                    index = ((Integer) itemIndex).intValue();
                } else if (itemIndex instanceof Double) {
                    index = ((Double) itemIndex).intValue();
                } else {
                    index = (int) itemIndex;
                }
                if (dict.size() == 2) {
                    String node = dict.get("node_id").toString();
                    allAlignmentsInTheSentence.add(new Alignment(node, index));
                } else if (dict.size() == 3) {
                    Set<String> nodeIdList = new HashSet<>();
                    List<String> nodeLists = (List<String>) dict.get("node_id");
                    nodeIdList.addAll(nodeLists);
                    String lexicalNode = (String) dict.get("lexical_node");
                    allAlignmentsInTheSentence.add(new Alignment(nodeIdList, index, lexicalNode));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return allAlignmentsInTheSentence;
    }

    @Override
    public List<MRInstance> readCorpus(String Path) throws IOException {
        String stemDirectory = "en.drs.penmaninfo";
        List<String> corpus = readParentDirectories(Path, stemDirectory);
        List<MRInstance> returnedCorpus = new ArrayList();
        int id = 0;
        for (String instance : corpus) {
            List<String> tokenizedSentence = tokenizeSentence(instance);
            SGraph graph = buildGraph(instance);
            List<Alignment> allAlignmentsInTheSentence = createAlignment(instance);
            MRInstance mrInstance = new MRInstance(tokenizedSentence, graph, allAlignmentsInTheSentence);
            id++;
            mrInstance.setId(id + "");
            returnedCorpus.add(mrInstance);
        }
        return returnedCorpus;

    }

    @Override
    public EdgeAttachmentHeuristic getEdgeHeuristic() {
        return new DRTEdgeAttachmentHeuristic();
    }


}