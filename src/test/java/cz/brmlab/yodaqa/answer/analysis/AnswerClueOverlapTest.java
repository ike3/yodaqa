package cz.brmlab.yodaqa.answer.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.ansscore.*;
import cz.brmlab.yodaqa.analysis.answer.AnswerClueOverlap;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.*;

/*
 * Потенциальный ответ (Что такое ППН?)
 *
 * Clue: ППН
 * Clue: правило
 * Answer: правило правая нога (ппн)
 *
 * Это генерит MetaMatch
 */
public class AnswerClueOverlapTest extends MultiCASPipelineTest {

    public static class Tested extends SimpleQuestion {
        public void initCas(JCas jcas) {
            try {
                jcas.setDocumentLanguage("ru");
                JCas q = jcas.createView("Question");
                JCas a = jcas.createView("Answer");

                QuestionInfo qInfo = new QuestionInfo(q);
                qInfo.setSource("interactive");
                qInfo.setQuestionId("1");
                qInfo.addToIndexes(q);

                AnswerInfo ai = new AnswerInfo(a);
                ai.setCanonText("правило правая нога ппн");
                ai.addToIndexes(a);

                ClueNE c1 = new ClueNE(q);
                c1.setLabel("ппн");
                c1.addToIndexes(q);

                ClueConcept c2 = new ClueConcept(q);
                c2.setLabel("правило");
                c2.addToIndexes(q);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {
        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                AnswerInfo ai = JCasUtil.selectSingle(jcas.getView("Answer"), AnswerInfo.class);
                AnswerFV fv = new AnswerFV(ai);
                Assert.assertEquals(1.0, fv.getFeatureValue(AF.ClOCPrefixedScore), 0.1);
                Assert.assertEquals(1.0, fv.getFeatureValue(AF.ClOCSuffixedScore), 0.1);
                Assert.assertEquals(1.0, fv.getFeatureValue(AF.ClOCMetaMatchScore), 0.1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void run() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(AnswerClueOverlap.class));

        runPipeline(Tested.class, "это игнорируется, см ai.setCanonText", builder, TestConsumer.class);
    }
}
