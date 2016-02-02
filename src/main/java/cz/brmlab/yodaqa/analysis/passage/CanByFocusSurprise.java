package cz.brmlab.yodaqa.analysis.passage;

import java.util.*;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.*;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.*;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.*;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Create CandidateAnswers for all NEs (named entities) that do not
 * contain supplied clues.
 *
 * This is pretty naive but should generate some useful answers. */

@SofaCapability(
	inputSofas = { "Question", "Result", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class CanByFocusSurprise extends CandidateGenerator {
	public CanByFocusSurprise() {
		logger = LoggerFactory.getLogger(CanByFocusSurprise.class);
	}

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, resultView, passagesView;
		try {
			questionView = jcas.getView("Question");
			resultView = jcas.getView("Result");
			passagesView = jcas.getView("PickedPassages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		ResultInfo ri = JCasUtil.selectSingle(resultView, ResultInfo.class);

		for (Passage p: JCasUtil.select(passagesView, Passage.class)) {
		    for (Sentence s : JCasUtil.selectCovered(passagesView, Sentence.class, p)) {
    	        Set<Token> tokens = new HashSet<Token>();
    	        for (Token t : JCasUtil.selectCovered(passagesView, Token.class, s))
    	            tokens.add(t);

    	        SortedSet<Token> governors = new TreeSet<Token>(
    	            new Comparator<Token>(){ @Override
    	                public int compare(Token t1, Token t2){
    	                    return t1.getBegin() - t2.getBegin();
    	                }
    	            });
    	        for (Dependency d : JCasUtil.selectCovered(passagesView, Dependency.class, s)) {
    	            if (tokens.contains(d.getGovernor())) {
    	                governors.add(d.getGovernor());
    	                // logger.debug("+ governor {}", d.getGovernor());
    	            }
    	        }
    	        for (Dependency d : JCasUtil.selectCovered(passagesView, Dependency.class, s)) {
    	            if (tokens.contains(d.getGovernor())) {
    	                governors.remove(d.getDependent());
    	                // logger.debug("- dependent {}", d.getDependent());
    	            }
    	        }

    	        for (Token t : governors) {
    	            String text = (t.getLemma() != null ? t.getLemma().getValue() : t.getCoveredText());

    				/* TODO: This can be optimized a lot. */
    				boolean matches = false;
    				for (Clue clue : JCasUtil.select(questionView, Clue.class)) {
    					if (text.endsWith(clue.getLabel())) {
    						matches = true;
    						break;
    					}
    				}

    				AnswerFV fv = new AnswerFV(ri.getAnsfeatures());
    				fv.merge(new AnswerFV(p.getAnsfeatures()));
    				fv.setFeature(AF.OriginPsgNP, 1.0);
    				if (!matches) {
    					/* Surprise! */
    					fv.setFeature(AF.OriginPsgSurprise, 1.0);
    				}

    				addCandidateAnswer(passagesView, p, s, fv);
    			}
		    }
		}
	}
}
