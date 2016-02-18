package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.Language;
import cz.brmlab.yodaqa.analysis.QuestionWord;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.QuestionWordLAT;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import static cz.brmlab.yodaqa.analysis.QuestionWord.getQuestionWordByText;

public class ClueByQuestionWord extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        QuestionWord questionWord = null;
        String questionText = null;
        for (LAT lat : JCasUtil.select(aJCas, LAT.class)) {
            if (lat instanceof QuestionWordLAT) {
                questionWord = getQuestionWordByText(lat.getText()) != null ? getQuestionWordByText(lat.getText()) : null;
                if(questionWord == null) {
                    questionText = "";
                } else if(Language.RUSSIAN.equals(lat.getLanguage())) {
                    questionText = questionWord.getRuText();
                }
                else {
                    questionText = questionWord.getEngText();
                }
                Clue clue = new Clue(aJCas);
                clue.setBegin(lat.getBegin());
                clue.setEnd(lat.getEnd());
                clue.setBase(lat);
                clue.setWeight(1.5);
                clue.setLabel(questionText);
                clue.setIsReliable(false);
                clue.addToIndexes(aJCas);
            }
        }
    }

}
