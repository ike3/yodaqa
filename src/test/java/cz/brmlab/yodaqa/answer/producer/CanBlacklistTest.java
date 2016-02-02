package cz.brmlab.yodaqa.answer.producer;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.passage.CanBlacklist;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;

/*
 * Сравнивает Clue и NamedEntity
 */
public class CanBlacklistTest extends MultiCASPipelineTest {

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

        INPUT = "между the слон and book";
        EXPECTED_TEXT = "слон,book";
        runPipeline(Tested.class, INPUT, builder, TestConsumer.class);
    }
}
