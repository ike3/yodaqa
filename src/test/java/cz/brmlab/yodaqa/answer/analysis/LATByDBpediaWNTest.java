package cz.brmlab.yodaqa.answer.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.answer.LATByDBpediaWN;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.TyCor.DBpWNLAT;

/*
 */
public class LATByDBpediaWNTest extends MultiCASPipelineTest {
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
                for (DBpWNLAT lat : JCasUtil.select(jcas, DBpWNLAT.class)) {
                    if (EXPECTED_OUTPUT.equals(lat.getText()))
                        return;
                }
                Assert.fail();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void runEN() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATByDBpediaWN.class));

        EXPECTED_OUTPUT = "company";
        runPipeline(Tested.class, "Ciber", builder, TestConsumer.class);
    }

    @Test
    public void runRU() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATByDBpediaWN.class));

        EXPECTED_OUTPUT = "musician";
        runPipeline(Tested.class, "Маликов, Дмитрий Юрьевич", builder, TestConsumer.class);
    }
}
