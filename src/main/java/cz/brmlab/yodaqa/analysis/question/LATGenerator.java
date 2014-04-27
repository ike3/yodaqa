package cz.brmlab.yodaqa.analysis.question;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.Question.LAT;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;

/**
 * Generate LAT annotations in a QuestionCAS. These are words that should
 * be type-coercable to the answer term. E.g. "Who starred in Moon?" should
 * generate LATs "who", "actor", possibly "star".  Candidate answers will be
 * matched against LATs to acquire score.  Focus is typically always also an
 * LAT.
 *
 * Prospectively, we will want to add multiple diverse LAT annotators. This
 * one simply generates a single LAT from the Focus. */

public class LATGenerator extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* A Focus is also an LAT. */
		for (Focus focus : JCasUtil.select(jcas, Focus.class)) {
			addFocusLAT(jcas, focus);
		}

		/* TODO: Also derive an LAT from SV subject nominalization
		 * using wordnet. */
	}

	protected void addFocusLAT(JCas jcas, Focus focus) {
		/* Convert focus to its lemma. */
		Annotation fbase = focus.getBase();
		Token ftok;
		if (focus.getTypeIndexID() == NSUBJ.type) {
			ftok = ((NSUBJ) fbase).getDependent();
		} else {
			ftok = (Token) fbase;
		}
		String text = ftok.getLemma().getValue();

		/* If focus is the question word, convert to an appropriate
		 * concept word or give up. */
		if (text.equals("who") || text.equals("whom")) {
			text = "person";
		} else if (text.equals("when")) {
			text = "time";
		} else if (text.equals("where")) {
			text = "location";

		} else if (text.matches("^what|why|how|which|name$")) {
			System.err.println("?! Skipping focus LAT for ambiguous qlemma " + text);
			return;
		}

		addLAT(jcas, focus.getBegin(), focus.getEnd(), focus, text);
	}

	protected void addLAT(JCas jcas, int begin, int end, Annotation base, String text) {
		LAT lat = new LAT(jcas);
		lat.setBegin(begin);
		lat.setEnd(end);
		lat.setBase(base);
		lat.setText(text);
		lat.addToIndexes();
	}
}