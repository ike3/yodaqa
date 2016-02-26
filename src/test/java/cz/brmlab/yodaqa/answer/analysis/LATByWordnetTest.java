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
import cz.brmlab.yodaqa.analysis.tycor.*;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.TyCor.*;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.*;

/*
 */
public class LATByWordnetTest extends MultiCASPipelineTest {
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
                lat.setLanguage(jcas.getDocumentLanguage());
                lat.setText(INPUT);
                lat.addToIndexes(jcas);

                POS pos = new NN(jcas);
                pos.setPosValue("NN");
                lat.setPos(pos);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {
        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                for (WordnetLAT lat : JCasUtil.select(jcas, WordnetLAT.class)) {
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
        builder.add(createPrimitiveDescription(LATByWordnet.class));

        INPUT = "sun";
        EXPECTED_OUTPUT = "star";
        runPipeline(Tested.class, "ignore", builder, TestConsumer.class);
    }

    @Test
    public void runRU() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(LATByWordnet.class));

        INPUT = "Солнце";
        EXPECTED_OUTPUT = "божество";
        runPipeline(Tested.class, "это игнорируется", builder, TestConsumer.class);
    }
}
