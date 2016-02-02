package cz.brmlab.yodaqa.answer.producer;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.dbpedia.spotlight.uima.SpotlightNameFinder;
import org.junit.*;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.passage.CanByNESurprise;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.SearchResult.*;
import cz.brmlab.yodaqa.provider.SyncOpenNlpNameFinder;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

/*
 * Сравнивает Clue и NamedEntity
 */
public class CanByNESurpriseTest extends MultiCASPipelineTest {

    private static String INPUT;
    private static String EXPECTED_TEXT;

    public static class Tested extends SimpleQuestion {


        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                JCas rView = jcas.createView("Result");
                rView.setDocumentText(INPUT);
                rView.setDocumentLanguage(jcas.getDocumentLanguage());
                ResultInfo ri = new ResultInfo(rView);
                ri.addToIndexes(rView);

                JCas qView = jcas.createView("Question");
                qView.setDocumentLanguage(jcas.getDocumentLanguage());
                QuestionInfo qInfo = new QuestionInfo(qView);
                qInfo.setSource("interactive");
                qInfo.setQuestionId("1");
                qInfo.addToIndexes(qView);

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
                PrintAnnotations.printAnnotations(jcas.getView("PickedPassages").getCas(), System.out);
                CandidateAnswer can = JCasUtil.selectSingle(jcas.getView("PickedPassages"), CandidateAnswer.class);
                Assert.assertEquals(can.getBase().getCoveredText(), EXPECTED_TEXT);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void runEN() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class),
                CAS.NAME_DEFAULT_SOFA, "PickedPassages");
        builder.add(createPrimitiveDescription(SyncOpenNlpNameFinder.class),
                CAS.NAME_DEFAULT_SOFA, "PickedPassages");

        builder.add(createPrimitiveDescription(CanByNESurprise.class));

        INPUT = "The Harry Potter's book";
        EXPECTED_TEXT = "Harry Potter's";
        runPipeline(Tested.class, "ignore", builder, TestConsumer.class);
    }

    @Test
    public void runRU() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(SpotlightNameFinder.class),
                CAS.NAME_DEFAULT_SOFA, "PickedPassages");

        builder.add(createPrimitiveDescription(CanByNESurprise.class));

        INPUT = "Книга про Гарри Поттера";
        EXPECTED_TEXT = "Гарри Поттера";
        runPipeline(Tested.class, "это игнорируется", builder, TestConsumer.class);
    }
}
