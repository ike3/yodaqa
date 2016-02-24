package cz.brmlab.yodaqa.analysis.passage;

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
import cz.brmlab.yodaqa.provider.SpeechKit;
import cz.brmlab.yodaqa.provider.SpeechKit.SpeechKitResponse;
import cz.brmlab.yodaqa.provider.SpeechKit.SpeechKitResponse.*;
import cz.brmlab.yodaqa.provider.SpeechKit.SpeechKitResponse.GeoAddr.Fields;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Create CandidateAnswers for all NEs (named entities) that do not
 * contain supplied clues.
 *
 * This is pretty naive but should generate some useful answers. */

@SofaCapability(
	inputSofas = { "Question", "Result", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class CanBySpeechKitSurprise extends CandidateGenerator {
    private SpeechKit speechKit = new SpeechKit();

	public CanBySpeechKitSurprise() {
		logger = LoggerFactory.getLogger(CanBySpeechKitSurprise.class);
	}

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
        if (!speechKit.isEnabled()) {
            logger.warn("Speech kit is disabled. Please provide its developer key.");
            return;
        }

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
    		    SpeechKitResponse result = speechKit.query(s.getCoveredText());
    	        for (Date date : result.getDate()) {
    	            String label = date.toString();
    	            addCanIfMatches(questionView, passagesView, ri, p, s, label);
    	        }
    	        for (Fio fio : result.getFio()) {
    	            String label = fio.toString();
                    addCanIfMatches(questionView, passagesView, ri, p, s, label);
    	        }
    	        for (GeoAddr addr : result.getGeoAddr()) {
    	            for (Fields fields : addr.getFields()) {
    	                String label = fields.getName();
                        addCanIfMatches(questionView, passagesView, ri, p, s, label);
    	            }
    	        }
            }
		}
	}

    private void addCanIfMatches(JCas questionView, JCas passagesView, ResultInfo ri, Passage p, Sentence base,
            String text) throws AnalysisEngineProcessException {
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
        fv.setFeature(AF.OriginPsgNE, 1.0);
        if (!matches) {
        	/* Surprise! */
        	fv.setFeature(AF.OriginPsgSurprise, 1.0);
        }

        addCandidateAnswer(passagesView, p, base, fv);
    }
}
