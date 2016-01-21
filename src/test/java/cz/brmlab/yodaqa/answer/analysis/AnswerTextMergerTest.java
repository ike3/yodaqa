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
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.pipeline.AnswerTextMerger;

/*
 */
public class AnswerTextMergerTest {
    private static String EXPECTED_OUTPUT;

    public static class Tested extends SimpleQuestion {


        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                Answer a1 = new Answer(jcas);
                a1.setCanonText("правило правая нога ппн");
                a1.setText("Правило Правая Нога ппн");
                a1.setConfidence(0.9);
                a1.addToIndexes(jcas);

                Answer a2 = new Answer(jcas);
                a2.setCanonText("правило правая нога ппн");
                a2.setText("Правило правая yога ппн");
                a2.setConfidence(0.8);
                a2.addToIndexes(jcas);

                Answer a3 = new Answer(jcas);
                a3.setCanonText("правило левая нога ппн");
                a3.setText("правило Левая нога ппн");
                a3.setConfidence(0.5);
                a3.addToIndexes(jcas);

                AnswerFV fv = new AnswerFV(a1);
                a1.setFeatures(fv.toFSArray(jcas));


            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {
        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                List<String> answers = new ArrayList<>();
                for (Answer answer : JCasUtil.select(jcas, Answer.class)) {
                    answers.add(answer.getText());
                }
                Assert.assertEquals(EXPECTED_OUTPUT, StringUtils.join(answers, ','));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void run() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(AnswerTextMerger.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, "игнор");

        EXPECTED_OUTPUT = "Правило Правая Нога ппн,правило Левая нога ппн";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }
}
