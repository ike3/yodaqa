package ru.yandex.speechkit;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.*;

import cz.brmlab.yodaqa.provider.SpeechKit;
import cz.brmlab.yodaqa.provider.SpeechKit.SpeechKitResponse;
import cz.brmlab.yodaqa.provider.SpeechKit.SpeechKitResponse.Fio;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntityEx;

/**
 * Generate Clue annotations in a QuestionCAS. These represent key information
 * stored in the question that is then used in primary search.  E.g. "What was
 * the first book written by Terry Pratchett?" should generate clues "first",
 * "book", "first book", "write" and "Terry Pratchett".
 *
 * This generates clues from all NamedEntities, e.g. "Terry Pratchett",
 * recognized in the document. */

public class SpeechKitNamedEntityFinder extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(SpeechKitNamedEntityFinder.class);

	SpeechKit speechKit = new SpeechKit();

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
	    SpeechKitResponse result = speechKit.query(jcas.getDocumentText());
        for (Fio fio : result.getFio()) {
            String label = fio.toCanonicString();
            NamedEntityEx ne = new NamedEntityEx(jcas, SpeechKit.getBegin(fio.getTokens(), result), SpeechKit.getEnd(fio.getTokens(), result));
            ne.setValue("person");
            ne.addToIndexes(jcas);
            ne.setCanonText(label);
        }
	}
}
