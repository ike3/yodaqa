package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.Language;
import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.TyCor.QuestionWordLAT;
import cz.brmlab.yodaqa.provider.MultiLanguageDictionaryFacade;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.PointerTarget;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.HashSet;
import java.util.Set;

public class LATBySVRu extends LATBySV {

    MultiLanguageDictionaryFacade dictionary = null;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        dictionary = MultiLanguageDictionaryFacade.getInstance();
    }

    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
        if (JCasUtil.select(jcas, QuestionWordLAT.class).isEmpty())
            return;

		/* Question word LAT is included, try to produce a LAT
		 * based on the SV too. */
//        if (JCasUtil.select(jcas, NSUBJ.class).isEmpty())
//            return;
        boolean hasSpecificDep = false;
        for(Dependency dependency: JCasUtil.select(jcas, Dependency.class)) {
            if(dependency.getDependencyType().equals("огранич")) {
                hasSpecificDep = true;
                break;
            }
        }
        if(!hasSpecificDep) {
            return;
        }
        // we just grab the sv instead of NSUBJ dependent as the SV
        // strategy may be more complex in case of tricky constructions

        for (SV sv : JCasUtil.select(jcas, SV.class)) {
            try {
                deriveSVLAT(jcas, sv);
            } catch (JWNLException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }

    @Override
    public void deriveSVLAT(JCas jcas, SV sv) throws JWNLException {
        IndexWord w = dictionary.getIndexWord(net.sf.extjwnl.data.POS.VERB, sv.getBase().getLemma().getValue(), Language.RUSSIAN);
        if (w == null)
            return;

		/* Try to derive a noun. */
        Set<Long> producedSynsets = new HashSet<>();
        for (Synset synset : w.getSenses()) {
            for (PointerTarget t : synset.getTargets(net.sf.extjwnl.data.PointerType.DERIVATION)) {
                Word nounw = (Word) t;
                if (nounw.getPOS() != net.sf.extjwnl.data.POS.NOUN)
                    continue;
                long ss = nounw.getSynset().getOffset();
                if (producedSynsets.contains(ss))
                    continue;
                else
                    producedSynsets.add(ss);
                addSVLAT(jcas, sv, nounw.getLemma(), ss);
            }
        }
    }
}
