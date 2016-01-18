package cz.brmlab.yodaqa;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.*;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.*;

import cz.brmlab.yodaqa.flow.dashboard.*;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;


public class SimpleQuestion extends CasCollectionReader_ImplBase {
    public static final String PARAM_INPUT = "input";
	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;

	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	private int index = 1;

	@ConfigurationParameter(name = PARAM_INPUT, mandatory = true)
	private String input;

	@Override
	public boolean hasNext() throws CollectionException {
		return input != null;
	}

	public void initCas(JCas jcas) {
		jcas.setDocumentLanguage(language);

		QuestionInfo qInfo = new QuestionInfo(jcas);
		qInfo.setSource("interactive");
		qInfo.setQuestionId(Integer.toString(index));
		qInfo.addToIndexes(jcas);
	}

	@Override
	public void getNext(CAS aCAS) throws CollectionException {
		Question q = new Question(Integer.toString(index), input);
		QuestionDashboard.getInstance().askQuestion(q);
		QuestionDashboard.getInstance().getQuestionToAnswer();

		try {
			JCas jcas = aCAS.getJCas();
			initCas(jcas);
			jcas.setDocumentText(input);
		} catch (CASException e) {
			throw new CollectionException(e);
		}
		input = null;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[]{new ProgressImpl(index, -1, Progress.ENTITIES)};
	}

	@Override
	public void close() {
	}
}
