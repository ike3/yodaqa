package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.Language;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.N;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

public class LatByNoun extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for(Token token: JCasUtil.select(aJCas, Token.class)) {
            if(token.getPos() instanceof N) {
                LAT lat = new LAT(aJCas, token.getBegin(), token.getEnd());
                lat.setBase(token);
                lat.setPos(token.getPos());
                lat.setText(token.getLemma().getValue());
                lat.setSpecificity(-10);
                lat.setSynset(0);
                lat.addToIndexes(aJCas);
                lat.setLanguage(Language.RUSSIAN);
            }
        }
    }

}
