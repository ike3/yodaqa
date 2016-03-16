package cz.brmlab.yodaqa.io.interactive;

import java.io.*;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerResource;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;

/**
 * A trivial consumer that will extract the final answer and print it
 * on the standard output for the user to "officially" see.
 *
 * Pair this with InteractiveQuestionReader.
 */

public class InteractiveAnswerWriter extends JCasConsumer_ImplBase {

    public static final String PARAM_DIRECTORY = "directory";

    @ConfigurationParameter(name = PARAM_DIRECTORY, mandatory = true)
    private String directory;

	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, answerHitlist;
		try {
			questionView = jcas.getView("Question");
			answerHitlist = jcas.getView("AnswerHitlist");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		FSIndex idx = answerHitlist.getJFSIndexRepository().getIndex("SortedAnswers");
		FSIterator answers = idx.iterator();

		PrintStream ps;
        try {
            File dir = new File(directory);
            dir.mkdirs();
            ps = new PrintStream(new File(dir, String.format("Question-%s.txt", qi.getQuestionId())));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        ps.println(questionView.getDocumentText());
        ps.println("=============");
        ps.println();

		if (answers.hasNext()) {
			//int counter = 0;
			int i = 1;
			while (answers.hasNext()) {
				Answer answer = (Answer) answers.next();
				StringBuilder sb = new StringBuilder();
				sb.append(i++);
				sb.append(". ");
				sb.append(answer.getText());
				sb.append(" (conf. ");
				sb.append(answer.getConfidence());
				sb.append(")");
				/* PRINT the passages assigned to this answer
				sb.append("\n");
				for(int ID: answer.getPassageIDs().toArray()){
					sb.append("		");
					sb.append(counter++);
					sb.append(". ");
					sb.append(QuestionDashboard.getInstance().getPassage(ID));
					sb.append(" (");
					sb.append(ID);
					sb.append(")");
					sb.append("\n");

				}
				counter = 0;
				*/
				if (answer.getResources() != null) {
					for (FeatureStructure resfs : answer.getResources().toArray()) {
						sb.append(" ");
						sb.append(((AnswerResource) resfs).getIri());
					}
				}
				ps.println(sb.toString());
			}
		} else {
		    ps.println("No answer found.");
		}
		ps.flush();
		ps.close();
		Question q = QuestionDashboard.getInstance().get(qi.getQuestionId());
		// q.setAnswers(answers); XXX
		QuestionDashboard.getInstance().finishQuestion(q);
	}
}
