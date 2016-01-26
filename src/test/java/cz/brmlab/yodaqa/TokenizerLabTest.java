package cz.brmlab.yodaqa;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.*;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.*;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ExternalResourceDescription;
import org.dbpedia.spotlight.uima.SpotlightNameFinder;
import org.junit.*;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.provider.SyncOpenNlpNameFinder;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import ru.kfu.cll.uima.tokenizer.fstype.SW;
import ru.kfu.itis.issst.uima.morph.dictionary.MorphDictionaryAPIFactory;
import ru.kfu.itis.issst.uima.morph.lemmatizer.Lemmatizer;
import ru.kfu.itis.issst.uima.segmentation.SentenceSplitter;
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

    @Test
    @Ignore
    /*
     * Для запуска нужно положить dict.opcorpora.ser в корень yodaqa
     */
    public void lemmatizer() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(InitialTokenizer.class));
        builder.add(createPrimitiveDescription(PostTokenizer.class));
        builder.add(createPrimitiveDescription(SentenceSplitter.class));
        // TODO: Нужен парсер, который бы аннотировал нам org.opencorpora.cas.Word
        AnalysisEngineDescription desc = createPrimitiveDescription(Lemmatizer.class);
        //MorphDictionary morphDict = MorphDictionaryAPIFactory.getMorphDictionaryAPI().getCachedInstance().getResource();
        ExternalResourceDescription morphDictDesc = MorphDictionaryAPIFactory.getMorphDictionaryAPI().getResourceDescriptionForCachedInstance();
        ExternalResourceFactory.bindExternalResource(desc, "morphDictionary", morphDictDesc);
        builder.add(desc);

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, "большие земли начинаются с маленьких");

        EXPECTED_CLASS = Token.class;
        EXPECTED_OUTPUT = "";

        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }

    @Test
    public void breakIteratorSegmenter() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "ru",
                SimpleQuestion.PARAM_INPUT, "Большие земли начинаются с маленьких.");

        EXPECTED_CLASS = Token.class;
        EXPECTED_OUTPUT = ".,Большие,земли,маленьких,начинаются,с";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }

    @Test
    public void openNlpNameFinderEN() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class));
        builder.add(createPrimitiveDescription(SyncOpenNlpNameFinder.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "en",
                SimpleQuestion.PARAM_INPUT, "Harry Potter");

        EXPECTED_CLASS = NamedEntity.class;
        EXPECTED_OUTPUT = "Harry Potter";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }

    @Test
    public void openNlpNameFinderRu() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(
                SpotlightNameFinder.class,
                SpotlightNameFinder.PARAM_ENDPOINT, "http://spotlight.sztaki.hu:2227/rest/annotate"));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "ru",
                SimpleQuestion.PARAM_INPUT, "Берлин");

        EXPECTED_CLASS = NamedEntity.class;
        EXPECTED_OUTPUT = "Берлин";
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }
}
