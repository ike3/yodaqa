package cz.brmlab.yodaqa.answer.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.util.*;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.SimpleQuestion;
import cz.brmlab.yodaqa.analysis.ansscore.*;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.pipeline.*;

/*
 */
public class EvidenceDiffusionTest {

    public static class Tested extends SimpleQuestion {


        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                Answer a1 = new Answer(jcas);
                a1.setCanonText("правило правая нога (ппн)");
                a1.setText("Правило Правая Нога (ппн)");
                a1.setConfidence(0.9);
                a1.addToIndexes(jcas);
                a1.setFeatures(new AnswerFV(a1).toFSArray(jcas));

                Answer a2 = new Answer(jcas);
                a2.setCanonText("правило правая нога");
                a2.setText("Правило правая нога");
                a2.setConfidence(0.8);
                a2.addToIndexes(jcas);
                a2.setFeatures(new AnswerFV(a2).toFSArray(jcas));

                Answer a3 = new Answer(jcas);
                a3.setCanonText("ппн");
                a3.setText("ппн");
                a3.setConfidence(0.5);
                a3.addToIndexes(jcas);
                a3.setFeatures(new AnswerFV(a3).toFSArray(jcas));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {
        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                Map<String, Answer> answers = new HashMap<>();
                for (Answer answer : JCasUtil.select(jcas, Answer.class)) {
                    answers.put(answer.getText(), answer);
                }

                AnswerFV fv = new AnswerFV(answers.get("Правило Правая Нога (ппн)"));
                Assert.assertEquals(0.8, fv.getFeatureValue(AF.EvDPrefixedScore), 0.1);
                Assert.assertEquals(0.5, fv.getFeatureValue(AF.EvDSubstredScore), 0.1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void run() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(EvidenceDiffusion.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, "игнор");

        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }
}
