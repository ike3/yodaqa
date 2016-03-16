package cz.brmlab.yodaqa;

import java.io.*;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.*;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.*;

import cz.brmlab.yodaqa.flow.dashboard.*;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;

public class LoadQuestionsFromFile extends CasCollectionReader_ImplBase {
    public static final String PARAM_INPUT = "input";
    public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;

    @ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
    private String language;

    private int index = 0;

    @ConfigurationParameter(name = PARAM_INPUT, mandatory = true)
    private String input;

    private BufferedReader reader;
    private String nextLine;

    @Override
    public boolean hasNext() throws CollectionException {
        return nextLine != null;
    }

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        try {
            reader = new BufferedReader(new FileReader(input));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        readNextLine();
    }

    private void readNextLine() {
        try {
            nextLine = reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        index++;
    }

    @Override
    public void getNext(CAS aCAS) throws CollectionException {
        try {
            QuestionInfo qInfo = new QuestionInfo(aCAS.getJCas());
            qInfo.setSource("interactive");
            qInfo.setQuestionId(Integer.toString(index));
            qInfo.addToIndexes(aCAS.getJCas());
            Question q = new Question(Integer.toString(index), nextLine);
            QuestionDashboard.getInstance().askQuestion(q);
            QuestionDashboard.getInstance().getQuestionToAnswer();
            try {
                JCas jcas = aCAS.getJCas();
                jcas.setDocumentLanguage(language);
                jcas.setDocumentText(nextLine);
            } catch (CASException e) {
                throw new CollectionException(e);
            }
            readNextLine();
        } catch (Exception e) {
            throw new CollectionException(e);
        }
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[] { new ProgressImpl(index, -1, Progress.ENTITIES) };
    }

    @Override
    public void close() {
    }
}
