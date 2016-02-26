package cz.brmlab.yodaqa.answer.producer;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.*;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.passage.CanByLATSubject;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.SearchResult.*;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.maltparser.*;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosTagger;

/*
 */
public class CanByLATSubjectTest extends MultiCASPipelineTest {

    private static String LAT_TEXT;
    private static String INPUT;
    private static String EXPECTED_TEXT;

    public static class Tested extends SimpleQuestion {


        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                JCas rView = jcas.createView("Result");
                rView.setDocumentText(INPUT);
                rView.setDocumentLanguage(jcas.getDocumentLanguage());
                ResultInfo ri = new ResultInfo(rView);
                ri.addToIndexes(rView);

                JCas qView = jcas.createView("Question");
                qView.setDocumentLanguage(jcas.getDocumentLanguage());
                QuestionInfo qInfo = new QuestionInfo(qView);
                qInfo.setSource("interactive");
                qInfo.setQuestionId("1");
                qInfo.addToIndexes(qView);

                LAT lat = new LAT(qView);
                lat.setText(LAT_TEXT);
                lat.addToIndexes(qView);

                JCas pView = jcas.createView("PickedPassages");
                pView.setDocumentLanguage(jcas.getDocumentLanguage());
                pView.setDocumentText(rView.getDocumentText());
                Passage passage = new Passage(pView, 0, rView.getDocumentText().length());
                passage.addToIndexes(pView);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {

        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                PrintAnnotations.printAnnotations(jcas.getView("PickedPassages").getCas(), System.out);
                PrintAnnotations.printDependecies(jcas.getView("PickedPassages"), System.out);
                CandidateAnswer can = JCasUtil.selectSingle(jcas.getView("PickedPassages"), CandidateAnswer.class);
                Assert.assertEquals(can.getBase().getCoveredText(), EXPECTED_TEXT);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void runEN() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class),
                CAS.NAME_DEFAULT_SOFA, "PickedPassages");
        builder.add(createPrimitiveDescription(
                StanfordParser.class,
                StanfordParser.PARAM_MAX_TOKENS, 50, // more takes a lot of RAM and is sloow, StanfordParser is O(N^2)
                StanfordParser.PARAM_WRITE_POS, true),
            CAS.NAME_DEFAULT_SOFA, "PickedPassages");
        builder.add(createPrimitiveDescription(LanguageToolLemmatizer.class),
                CAS.NAME_DEFAULT_SOFA, "PickedPassages");

        builder.add(createPrimitiveDescription(CanByLATSubject.class));

        LAT_TEXT = "book";
        INPUT = "The Harry Potter's book is black";
        EXPECTED_TEXT = "black";
        runPipeline(Tested.class, "ignore", builder, TestConsumer.class);
    }

    @Test
    public void runRU() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class),
                CAS.NAME_DEFAULT_SOFA, "PickedPassages");
        builder.add(createPrimitiveDescription(TreeTaggerPosTagger.class),
                CAS.NAME_DEFAULT_SOFA, "PickedPassages");
        builder.add(AnalysisEngineFactory.createEngineDescription(SingleThreadedMaltParser.class),
                CAS.NAME_DEFAULT_SOFA, "PickedPassages");

        builder.add(createPrimitiveDescription(CanByLATSubject.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "ru",
                SimpleQuestion.PARAM_INPUT, "игнор");

        LAT_TEXT = "книга";
        INPUT = "книга о Гарри Поттере имеет черную обложку";
        EXPECTED_TEXT = "черную";
        runPipeline(Tested.class, "игнор", builder, TestConsumer.class);
    }
}
