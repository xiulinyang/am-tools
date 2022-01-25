package de.saar.coli.amtools.decomposition.formalisms.toolsets;

import de.saar.coli.amrtagging.MRInstance;
import de.saar.coli.amrtagging.formalisms.amr.AMRBlobUtils;
import de.saar.coli.amrtagging.formalisms.sdp.psd.ConjHandler;
import de.saar.coli.amrtagging.formalisms.sdp.psd.PSDBlobUtils;

import java.util.List;

public class PSDDecompositionToolset extends SDPDecompositionToolset {

    AMRBlobUtils blobUtils = new PSDBlobUtils();

    /**
     * @param fasterModeForTesting If fasterModeForTesting is true, the decomposition packages will not fill the empty NE/lemma/POS slots of
     *                         the amconll file with the Stanford NLP solution.
     **/
    public PSDDecompositionToolset(Boolean fasterModeForTesting) {
        super(fasterModeForTesting);
    }

    @Override
    public void applyPreprocessing(List<MRInstance> corpus) {
        for (MRInstance inst : corpus) {
            inst.setGraph(ConjHandler.handleConj(inst.getGraph(), (PSDBlobUtils)getEdgeHeuristics(), false));
        }
    }

    @Override
    public AMRBlobUtils getEdgeHeuristics() {
        return blobUtils;
    }
}
