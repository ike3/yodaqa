package cz.brmlab.yodaqa.analysis;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.HashMap;
import java.util.Map;

public class TreeTaggerPosToSynTagRus extends JCasAnnotator_ImplBase {

    final Map<String, String> POS_VALUES = new HashMap<>();

    {
        POS_VALUES.put("^A.*", "A");
        POS_VALUES.put("^C$", "CONJ");
        POS_VALUES.put("^I$", "INTJ");
        POS_VALUES.put("^M.*", "NUM");
        POS_VALUES.put("^N.*", "S");
        POS_VALUES.put("^R.*", "ADV");
        POS_VALUES.put("^V.*", "V");
        POS_VALUES.put("^S.*", "PR");
        POS_VALUES.put("^Q$", "PART");
    }


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for(Token token: JCasUtil.select(aJCas, Token.class)) {
            for(Map.Entry<String, String> entry: POS_VALUES.entrySet()) {
                if(token.getPos().getPosValue().matches(entry.getKey())) {
                    token.getPos().setPosValue(entry.getValue());
                }
            }
        }
    }

}
