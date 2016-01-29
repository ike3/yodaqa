package cz.brmlab.yodaqa.answer.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.SimpleQuestion;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.answer.LATByWnInstance;
import cz.brmlab.yodaqa.analysis.tycor.LATNormalize;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.TyCor.LAT;

/*
 */
public class LATNormalizeTest {
    private static String EXPECTED_OUTPUT;
    private static String INPUT;

    public static class Tested extends SimpleQuestion {

        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                AnswerInfo ai = new AnswerInfo(jcas);
                ai.setCanonText("правило правая нога ппн");
                ai.addToIndexes(jcas);
                AnswerFV fv = new AnswerFV(ai);
                ai.setFeatures(fv.toFSArray(jcas));

                LAT lat = new LAT(jcas);
                lat.setText(INPUT);
                lat.addToIndexes(jcas);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {
        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                Set<String> lats = new TreeSet<>();
                for (LAT lat : JCasUtil.select(jcas, LAT.class)) {
                    lats.add(lat.getText());
                }
                Assert.assertEquals(EXPECTED_OUTPUT, StringUtils.join(lats, ","));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void runRU() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATNormalize.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "ru",
                SimpleQuestion.PARAM_INPUT, "игнорируется");

        INPUT = "большие земли";
        EXPECTED_OUTPUT = "большие земля,земля";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }

    @Test
    public void runEN() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATNormalize.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, "игнорируется");

        INPUT = "several suns";
        EXPECTED_OUTPUT = "several sun,sun";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }
}
