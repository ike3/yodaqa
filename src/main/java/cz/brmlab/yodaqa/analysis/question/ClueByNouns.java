package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.CluePhrase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.N;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

public class ClueByNouns extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for(Token token: JCasUtil.select(aJCas, Token.class)) {
            if(token.getPos() instanceof N) {
                addClue(aJCas, token);
            }
        }
    }

    private void addClue(JCas jcas, Token token) {
        Clue clue = new CluePhrase(jcas);
        clue.setBegin(token.getBegin());
        clue.setEnd(token.getEnd());
        clue.setBase(token.getLemma());
        clue.setWeight(1.0);
        clue.setLabel(token.getLemma().getValue());
        clue.addToIndexes();
    }

}
