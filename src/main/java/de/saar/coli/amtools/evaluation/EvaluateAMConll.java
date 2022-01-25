package de.saar.coli.amtools.evaluation;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.saar.coli.amrtagging.*;


import de.saar.coli.amtools.evaluation.toolsets.EvaluationToolset;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.graph.SGraph;
import de.up.ling.tree.ParseException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


/**
 * Evaluates the AM Dependency Terms in an AMConll corpus and outputs the graphs in a new corpus file. This works in
 * concert with an EvaluationToolset class that specifies the output corpus format and any postprocessing.
 *
 * @author JG, based on previous de.saar.coli.amrtagging.formalisms.amr.tools.EvaluateCorpus
 */
public class EvaluateAMConll {

    @Parameter(names = {"--corpus", "-c"}, description = "Path to the input corpus with decoded AM dependency trees", required = true)
    private String corpusPath = null;

    @Parameter(names = {"--outPath", "-o"}, description = "Path for output files", required = true)
    private String outPath = null;

    @Parameter(names = {"--gold", "-g"}, description = "Path to gold corpus. Usually expected to contain the same instances in the same order as " +
            "the --corpus file (unless the evaluation toolset says otherwise). Giving the gold corpus here is optional, and only works if the evaluation" +
            "toolset has the compareToGold function implemented. Alternatively, use an external evaluation tool after this program has run (such as" +
            "the Smatch script for AMR graphs).")
    private String goldCorpus = null;

    @Parameter(names = {"--evaluationToolset", "-ts"}, description = "Classname of the EvaluationToolset class to be used. Default applies no postprocessing and writes the files in ISI AMR format")
    private String evaluationToolsetName = "de.saar.coli.amtools.evaluation.toolsets.EvaluationToolset";

    @Parameter(names = {"--extras", "-e"}, description = "Additional parameters to the constructor of the Evaluation toolset, as a single string. Optional." +
            " Note that using this parameter causes a different constructor of the evaluation toolset to be called.")
    private String toolsetExtras = null;

    @Parameter(names = {"--help", "-?","-h"}, description = "displays help if this is the only command", help = true)
    private boolean help=false;

    private boolean continueBeyondArgumentReading;



    public static void main(String[] args) throws IOException, ParseException, ParserException, AlignedAMDependencyTree.ConllParserException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, InterruptedException {
        EvaluateAMConll amConllEvaluator = new EvaluateAMConll();

        amConllEvaluator.readCommandLineArguments(args);
        if (!amConllEvaluator.continueBeyondArgumentReading) {
            return;
        }

        List<AmConllSentence> inputAMConllSentences = amConllEvaluator.readAMConllFile();

        EvaluationToolset evaluationToolset = amConllEvaluator.setupEvaluationToolset();

        List<MRInstance> outputCorpus = amConllEvaluator.evaluteAMCorpus(inputAMConllSentences, evaluationToolset);

        amConllEvaluator.writeOutputCorpus(evaluationToolset, outputCorpus);

        amConllEvaluator.compareToGoldIfRequired(evaluationToolset, outputCorpus);
    }


    private void readCommandLineArguments(String[] args) {
        JCommander commander = new JCommander(this);
        commander.setProgramName("constraint_extractor");

        try {
            commander.parse(args);
        } catch (com.beust.jcommander.ParameterException ex) {
            System.err.println("An error occured: " + ex.toString());
            System.err.println("\n Available options: ");
            commander.usage();
            continueBeyondArgumentReading = false;
        }

        if (help) {
            commander.usage();
            continueBeyondArgumentReading = false;
        }

        continueBeyondArgumentReading = true;
    }


    private List<AmConllSentence> readAMConllFile() throws IOException, ParseException {
        return AmConllSentence.readFromFile(corpusPath);
    }

    private EvaluationToolset setupEvaluationToolset() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Class<?> clazz = getEvaluationToolsetClass();
        return createEvaluationToolsetObject(clazz);
    }

    private Class<?> getEvaluationToolsetClass() {
        Class<?> clazz;
        try {
            clazz = Class.forName(evaluationToolsetName);
        } catch (ClassNotFoundException ex) {
            try {
                clazz = Class.forName("de.saar.coli.amtools.evaluation.toolsets." + evaluationToolsetName);
            } catch (ClassNotFoundException ex2) {
                throw new RuntimeException("Neither class "+evaluationToolsetName+
                        " nor de.saar.coli.amtools.decomposition.formalisms.toolsets." + evaluationToolsetName+" could be found! Aborting.");
            }
        }
        return clazz;
    }

    private EvaluationToolset createEvaluationToolsetObject(Class<?> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        // Calling a different constructor depending on whether the --extra option was used.
        if (toolsetExtras != null) {
            Constructor<?> ctor = clazz.getConstructor(String.class);
            return (EvaluationToolset)ctor.newInstance(new Object[] { toolsetExtras});
        } else {
            Constructor<?> ctor = clazz.getConstructor();
            return (EvaluationToolset)ctor.newInstance(new Object[] {});
        }
    }


    private static List<MRInstance> evaluteAMCorpus(List<AmConllSentence> inputAMConllSentences, EvaluationToolset evaluationToolset) throws ParseException, ParserException, AlignedAMDependencyTree.ConllParserException {
        List<MRInstance> outputCorpus = new ArrayList<>();

        for (AmConllSentence inputSentence : inputAMConllSentences) {
            ensureCompatibilityWithOldPipeline(inputSentence); //TODO is this the right place for this? Or is it AMR specific?

            SGraph evaluatedGraph = evaluateToAlignedGraph(inputSentence);
            List<Alignment> alignments = AlignedAMDependencyTree.extractAlignments(evaluatedGraph);
            MRInstance mrInst = encodeAsMRInstance(inputSentence, evaluatedGraph, alignments);

            evaluationToolset.applyPostprocessing(mrInst, inputSentence);

            outputCorpus.add(mrInst);
        }

        return outputCorpus;
    }

    private static SGraph evaluateToAlignedGraph(AmConllSentence s) throws ParseException, ParserException, AlignedAMDependencyTree.ConllParserException {
        AlignedAMDependencyTree amdep = AlignedAMDependencyTree.fromSentence(s);
        SGraph evaluatedGraph = amdep.evaluateWithoutRelex(true);
        evaluatedGraph = evaluatedGraph.withFreshNodenames();
        return evaluatedGraph;
    }

    private static MRInstance encodeAsMRInstance(AmConllSentence s, SGraph graph, List<Alignment> alignments) {
        MRInstance mrInst = new MRInstance(s.words(), graph, alignments);
        mrInst.setPosTags(s.getFields(AmConllEntry::getPos));
        mrInst.setLemmas(s.lemmas());
        mrInst.setNeTags(s.getFields(AmConllEntry::getNe));
        return mrInst;
    }

    private static void ensureCompatibilityWithOldPipeline(AmConllSentence s) {
        //fix the REPL problem:
        //the NN was trained with data where REPL was used for some nouns because the lexical label was lower-cased
        //we don't want $REPL$ in our output, so let's replace predictions that contain REPL but where there is no replacement field
        //with the word form.
        for (AmConllEntry e : s) {
            if (e.getLexLabel().contains(AmConllEntry.REPL_PLACEHOLDER) && e.getReplacement().equals(AmConllEntry.DEFAULT_NULL)) {
                e.setLexLabel(e.getReLexLabel().replace(AmConllEntry.REPL_PLACEHOLDER, AmConllEntry.FORM_PLACEHOLDER));
            }
        }
    }

    private void writeOutputCorpus(EvaluationToolset evaluationToolset, List<MRInstance> outputCorpus) throws IOException, InterruptedException {
        evaluationToolset.writeCorpus(outPath, outputCorpus);
    }

    private void compareToGoldIfRequired(EvaluationToolset evaluationToolset, List<MRInstance> outputCorpus) throws IOException {
        if (goldCorpus != null) {
            evaluationToolset.compareToGold(outputCorpus, goldCorpus);
        }
    }





}
