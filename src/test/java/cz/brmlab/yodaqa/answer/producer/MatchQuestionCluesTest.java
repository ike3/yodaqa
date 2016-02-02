package cz.brmlab.yodaqa.answer.producer;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.passage.MatchQuestionClues;
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.SearchResult.*;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosTagger;

/*
 * Сравнивает текст Passage с Clue из вопроса (по регулярке)
 * Реализовано: для русского языка сравнение идет по леммам
 */
public class MatchQuestionCluesTest extends MultiCASPipelineTest {

    private static ClueNE clue;
    private static String INPUT;
    private static String CLUE_LABEL;
    private static String EXPECTED_TEXT;

    public static class Tested extends SimpleQuestion {


        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                JCas rView = jcas.createView("Result");
                rView.setDocumentText(INPUT);
                rView.setDocumentLanguage(jcas.getDocumentLanguage());

                JCas qView = jcas.createView("Question");
                qView.setDocumentLanguage(jcas.getDocumentLanguage());
                QuestionInfo qInfo = new QuestionInfo(qView);
                qInfo.setSource("interactive");
                qInfo.setQuestionId("1");
                qInfo.addToIndexes(qView);

                clue = new ClueNE(qView);
                clue.setLabel(CLUE_LABEL);
                clue.addToIndexes(qView);

                JCas pView = jcas.createView("PickedPassages");
                pView.setDocumentLanguage(jcas.getDocumentLanguage());
                pView.setDocumentText(rView.getDocumentText());
                Passage passage = new Passage(pView, 0, rView.getDocumentText().length());
                passage.addToIndexes(pView);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {

        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                QuestionClueMatch match = JCasUtil.selectSingle(jcas.getView("PickedPassages"), QuestionClueMatch.class);
                Assert.assertEquals(clue, match.getBaseClue());
                Assert.assertEquals(match.getCoveredText(), EXPECTED_TEXT);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void runEN() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(MatchQuestionClues.class));

        INPUT = "The Harry Potter's book";
        CLUE_LABEL = "Harry Potter";
        EXPECTED_TEXT = "Harry Potter'";
        runPipeline(Tested.class, "ignore", builder, TestConsumer.class);
    }

    @Test
    public void runRU() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class),
                CAS.NAME_DEFAULT_SOFA, "PickedPassages");
        builder.add(createPrimitiveDescription(TreeTaggerPosTagger.class),
                CAS.NAME_DEFAULT_SOFA, "PickedPassages");

        builder.add(createPrimitiveDescription(MatchQuestionClues.class));

        INPUT = "Книга о желтом слоне";
        CLUE_LABEL = "желтый слон";
        EXPECTED_TEXT = "желтом слоне";
        runPipeline(Tested.class, "это игнорируется", builder, TestConsumer.class);
    }
}
