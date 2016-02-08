package cz.brmlab.yodaqa.answer.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
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

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.answer.*;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.TyCor.*;
import cz.brmlab.yodaqa.pipeline.YodaQA;

public class LATBySpeechKitTest extends MultiCASPipelineTest {
    private static String EXPECTED_OUTPUT;

    public static class Tested extends SimpleQuestion {

        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                AnswerInfo ai = new AnswerInfo(jcas);
                ai.setCanonText("правило правая нога ппн");
                ai.addToIndexes(jcas);
                AnswerFV fv = new AnswerFV(ai);
                ai.setFeatures(fv.toFSArray(jcas));

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

    /**
     * Для англ не работает, но нам главное чтобы не падало
     */
    @Test
    public void runEN() throws Exception {
        new YodaQA();
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATBySpeechKit.class));

        EXPECTED_OUTPUT = "";
        runPipeline(Tested.class, "who wrote Harry Potter at Lenina str in New York?", builder, TestConsumer.class);
    }

    @Test
    public void runRU() throws Exception {
        new YodaQA();
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATBySpeechKit.class));

        EXPECTED_OUTPUT = "15 Feb 2015,25 Jan 0000,гарри поттер,новосибирск,проспект ленина";
        runPipeline(Tested.class, "кто написал Гарри Поттера 25 января и 15 февраля 2015 на проспекте Ленина в Новосибирске", builder, TestConsumer.class);
    }
}
