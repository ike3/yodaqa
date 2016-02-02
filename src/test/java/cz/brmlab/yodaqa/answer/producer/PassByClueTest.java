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
import cz.brmlab.yodaqa.analysis.passextract.*;
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.SearchResult.*;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosTagger;

/*
 */
public class PassByClueTest extends MultiCASPipelineTest {

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

                ResultInfo ri = new ResultInfo(rView);
                ri.setDocumentTitle("Harry Potter");
                ri.addToIndexes(rView);

                JCas qView = jcas.createView("Question");
                qView.setDocumentLanguage(jcas.getDocumentLanguage());
                QuestionInfo qInfo = new QuestionInfo(qView);
                qInfo.setSource("interactive");
                qInfo.setQuestionId("1");
                qInfo.addToIndexes(qView);

                clue = new ClueNE(qView);
                clue.setLabel(CLUE_LABEL);
                clue.addToIndexes(qView);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {

        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                Passage passage = JCasUtil.selectSingle(jcas.getView("Passages"), Passage.class);
                Assert.assertEquals(passage.getCoveredText(), EXPECTED_TEXT);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void runEN() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(StanfordSegmenter.class), CAS.NAME_DEFAULT_SOFA, "Result");
        builder.add(createPrimitiveDescription(PassSetup.class));
        builder.add(createPrimitiveDescription(PassByClue.class));

        INPUT = "The Harry Potter's book. The philosoper's stone is a great thing. The writer is happy.";
        CLUE_LABEL = "Harry Potter";
        EXPECTED_TEXT = "The Harry Potter's book.";
        runPipeline(Tested.class, "ignore", builder, TestConsumer.class);
    }

    @Test
    public void runRU() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class), CAS.NAME_DEFAULT_SOFA, "Result");
        builder.add(createPrimitiveDescription(TreeTaggerPosTagger.class), CAS.NAME_DEFAULT_SOFA, "Result");
        builder.add(createPrimitiveDescription(PassSetup.class));
        builder.add(createPrimitiveDescription(PassByClue.class));

        INPUT = "Книга о желтом слоне. Слоны бывают разные. Самый лучший слон в Африке.";
        CLUE_LABEL = "желтый слон";
        EXPECTED_TEXT = "Книга о желтом слоне.";
        runPipeline(Tested.class, "это игнорируется", builder, TestConsumer.class);
    }
}
