package cz.brmlab.yodaqa.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.SimpleQuestion;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.answer.*;
import cz.brmlab.yodaqa.analysis.tycor.LATByWordnet;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.*;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NUM;

/*
 * Опирается на наличие аннотации NUM, причем она должна быть частью фокуса
 */
public class LATByQuantityTest {
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

                Token t = new Token(jcas, 0, 3);
                t.addToIndexes(jcas);

                POS pos = new POS(jcas, 3, 10);
                pos.setPosValue("CD");
                pos.addToIndexes(jcas);

                Token d = new Token(jcas, 3, 10);
                d.setPos(pos);
                d.addToIndexes(jcas);

                Focus f = new Focus(jcas, 0, 10);
                f.setToken(t);
                f.addToIndexes(jcas);

                NUM num = new NUM(jcas, 0, 3);
                num.addToIndexes(jcas);
                num.setGovernor(t);
                num.setDependent(d);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {
        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                for (QuantityCDLAT lat : JCasUtil.select(jcas, QuantityCDLAT.class)) {
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
        builder.add(createPrimitiveDescription(LATByQuantity.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, "240 meters");

        EXPECTED_OUTPUT = "measure";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }

    @Test
    public void runRU() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATByQuantity.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "ru",
                SimpleQuestion.PARAM_INPUT, "240 метров");

        EXPECTED_OUTPUT = "величина";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }
}
