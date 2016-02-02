package cz.brmlab.yodaqa.answer.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.answer.SyntaxCanonization;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;

/*
 * Просто приводит к нижнему регистру
 * Для анлг удаляет the, is и т.п.
 */
public class SyntaxCanonizationTest extends MultiCASPipelineTest {

    public static class Tested extends SimpleQuestion {
        public void initCas(JCas jcas) {
            super.initCas(jcas);

            AnswerInfo ai = new AnswerInfo(jcas);
            ai.addToIndexes(jcas);
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {
        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            AnswerInfo ai = JCasUtil.selectSingle(jcas, AnswerInfo.class);
            Assert.assertEquals("правило правая нога", ai.getCanonText());
        }
    }

    @Test
    public void syntaxCanonization() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(SyntaxCanonization.class));

        runPipeline(Tested.class, "Правило правАЯ НОГА", builder, TestConsumer.class);
    }
}
