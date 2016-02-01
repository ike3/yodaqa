package cz.brmlab.yodaqa.answer.producer;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.passage.*;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.provider.SyncOpenNlpNameFinder;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

/*
 * Сравнивает Clue и NamedEntity
 */
public class CanBlacklistTest {

    private static String EXPECTED_TEXT;
    private static String INPUT = "между the слон and book";

    public static class Tested extends SimpleQuestion {

        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                String[] words = INPUT.split("\\s");
                int pos = 0;
                for (String word : words) {
                    CandidateAnswer a1 = new CandidateAnswer(jcas);
                    a1.setBegin(pos);
                    a1.setEnd(pos + word.length());
                    a1.addToIndexes(jcas);
                    pos += word.length() + 1;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {

        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                PrintAnnotations.printAnnotations(jcas.getCas(), System.out);
                Collection<CandidateAnswer> cans = JCasUtil.select(jcas, CandidateAnswer.class);
                List<String> answers = new ArrayList<>();
                for (CandidateAnswer can : cans) {
                    answers.add(can.getCoveredText());
                }
                Assert.assertEquals(EXPECTED_TEXT, StringUtils.join(answers, ","));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void run() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(CanBlacklist.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, INPUT);

        EXPECTED_TEXT = "слон,book";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }
}
