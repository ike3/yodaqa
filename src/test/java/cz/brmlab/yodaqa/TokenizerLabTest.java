package cz.brmlab.yodaqa;

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
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.*;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import ru.kfu.cll.uima.tokenizer.fstype.SW;
import ru.kfu.itis.issst.uima.tokenizer.*;

public class TokenizerLabTest {
    private static String EXPECTED_OUTPUT;
    private static Class<? extends Annotation> EXPECTED_CLASS;

    public static class Tested extends SimpleQuestion {

        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {

        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                PrintAnnotations.printAnnotations(jcas.getCas(), System.out);
                Set<String> lats = new TreeSet<>();
                for (Annotation a : JCasUtil.select(jcas, EXPECTED_CLASS)) {
                    lats.add(a.getCoveredText());
                }
                Assert.assertEquals(EXPECTED_OUTPUT, StringUtils.join(lats, ","));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void simpleTokenizer() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(InitialTokenizer.class));
        builder.add(createPrimitiveDescription(PostTokenizer.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "ru",
                SimpleQuestion.PARAM_INPUT, "Большие земли начинаются с маленьких.");

        EXPECTED_CLASS = SW.class;
        EXPECTED_OUTPUT = "земли,маленьких,начинаются,с";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }

    @Test
    public void openNlpSegmenter() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(OpenNlpSegmenter.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, "big lands");

        EXPECTED_CLASS = Token.class;
        EXPECTED_OUTPUT = "big,lands";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }
}
