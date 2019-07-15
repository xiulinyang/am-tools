package de.saar.coli.amrtagging.formalisms.amr.tools.preproc;

import edu.stanford.nlp.ling.CoreLabel;

import java.util.List;

public interface NamedEntityRecognizer {
    public static final String OTHER = "O";
    public static final String PERSON = "PERSON"; // TODO UIUC is PER
    public static final String ORGANIZATION = "ORGANIZATION"; // TODO UIUC is ORG
    public static final String LOCATION = "LOCATION"; // TODO UIUC
    public static final String MISCELLANEOUS = "MISCELLANEOUS"; // TODO UIUC? how does Stanford call it?


    /**
     * Annotates the given tokens with named entity information. We use the four-class CoNLL
     * NER tagset, with values PER/  TODO write this.
     * Tokens which are not part of a named entity receive the label "O".
     *
     * <p>
     *     The NER tags are added to the given {@link CoreLabel} objects
     *     under the annotation type {@link edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation}.
     *     The tokens which are passed to this method are expected to define
     *     the word and begin and end position.
     * </p>
     *
     * @param tokens
     * @return
     * @throws PreprocessingException
     */
    public List<CoreLabel> tag(List<CoreLabel> tokens) throws PreprocessingException;
}
