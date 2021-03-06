package cz.brmlab.yodaqa.question.analysis;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.pipeline.*;
import cz.brmlab.yodaqa.provider.SyncOpenNlpNameFinder;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosTagger;
import ru.yandex.speechkit.SpeechKitNamedEntityFinder;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.dbpedia.spotlight.uima.SpotlightNameFinder;
import org.junit.*;

import java.util.Set;
import java.util.TreeSet;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

public class NamedEntityFinderTest extends MultiCASPipelineTest {
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
    public void posTagger() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(TreeTaggerPosTagger.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "ru",
                SimpleQuestion.PARAM_INPUT, "большие земли начинаются с маленьких");

        EXPECTED_CLASS = Lemma.class;
        EXPECTED_OUTPUT = "большие,земли,маленьких,начинаются,с";

        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }

    @Test
    public void openNlpNameFinderEN() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class));
        builder.add(createPrimitiveDescription(SyncOpenNlpNameFinder.class));

        EXPECTED_CLASS = NamedEntity.class;
        EXPECTED_OUTPUT = "Harry Potter";
        runPipeline(Tested.class, "Harry Potter", builder, TestConsumer.class);
    }

    @Test
    public void openNlpNameFinderRu() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(SpotlightNameFinder.class));

        EXPECTED_CLASS = NamedEntity.class;
        EXPECTED_OUTPUT = "Гарри Поттера";
        runPipeline(Tested.class, "Гарри Поттера", builder, TestConsumer.class);
    }

    @Test
    @Ignore // requires api key
    public void speechKitNameFinderRu() throws Exception {
        new YodaQA();
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(SpeechKitNamedEntityFinder.class));

        EXPECTED_CLASS = NamedEntity.class;
        EXPECTED_OUTPUT = "Владимир Владимирович Путин";
        runPipeline(Tested.class, "Владимир Владимирович Путин является действующим президентом России", builder, TestConsumer.class);
    }
}
