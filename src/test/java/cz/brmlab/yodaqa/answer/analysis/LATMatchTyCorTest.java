package cz.brmlab.yodaqa.answer.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.ansscore.*;
import cz.brmlab.yodaqa.analysis.tycor.LATMatchTyCor;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.TyCor.WordnetLAT;

/*
 * Сравнивает LATs вопроса и ответа. Берет только WordnetLAT!
 * Устанавливает AF.SpWordNet в e^spec в случае совпадения
 * текст LAT должен быть полностью идентичным
 */
public class LATMatchTyCorTest extends MultiCASPipelineTest {

    public static class Tested extends SimpleQuestion {


        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                JCas qView = jcas.createView("Question");
                JCas aView = jcas.createView("Answer");

                AnswerInfo ai = new AnswerInfo(aView);
                ai.setCanonText("правило правая нога ппн");
                ai.addToIndexes(aView);
                AnswerFV fv = new AnswerFV(ai);
                ai.setFeatures(fv.toFSArray(aView));

                WordnetLAT qlat = new WordnetLAT(qView);
                qlat.setText("lat1");
                qlat.setSpecificity(0.4);
                qlat.addToIndexes(qView);

                WordnetLAT alat = new WordnetLAT(aView);
                alat.setText("lat1");
                alat.setSpecificity(0.6);
                alat.addToIndexes(aView);
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
                Assert.assertEquals(Math.exp(1.0), fv.getFeatureValue(AF.SpWordNet), 0.1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void run() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATMatchTyCor.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, "игнор");

        runPipeline(Tested.class, "это игнорируется", builder, TestConsumer.class);
    }
}
