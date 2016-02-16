package cz.brmlab.yodaqa.answer.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.*;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.*;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import cz.brmlab.yodaqa.*;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.answer.AnswerAnalysisAE;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.pipeline.*;
import cz.brmlab.yodaqa.provider.OpenNlpNamedEntities;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.maltparser.MaltParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosTagger;

/*
 */
public class FullStackTest extends MultiCASPipelineTest {
    private static final String dir = System.getProperty("java.io.tmpdir");
    private static String QUESTION;
    private static String ANSWER;

    public static class Tested extends SimpleQuestion {


        public void initCas(JCas jcas) {
            try {
                super.initCas(jcas);

                JCas qView = jcas.createView("Question");
                qView.setDocumentText(QUESTION);
                qView.setDocumentLanguage(jcas.getDocumentLanguage());

                QuestionClass qc = new QuestionClass(qView);
                qc.setQuestionClass("HUM");
                qc.addToIndexes(qView);

                /*ClueNE c1 = new ClueNE(qView);
                c1.setLabel("wrote");
                c1.addToIndexes(qView);

                ClueConcept c2 = new ClueConcept(qView);
                c2.setLabel("Harry Potter");
                c2.addToIndexes(qView);*/

                JCas aView = jcas.createView("Answer");
                aView.setDocumentText(ANSWER);
                aView.setDocumentLanguage(jcas.getDocumentLanguage());

                AnswerInfo ai = new AnswerInfo(aView);
                ai.addToIndexes(aView);
                AnswerFV fv = new AnswerFV(ai);
                ai.setFeatures(fv.toFSArray(aView));
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
                //PrintAnnotations.printAnnotations(jcas.getView("Answer").getCas(), System.out);

                // SyntaxCanonization
                //Assert.assertEquals("j. k. rowling", ai.getCanonText());
                System.err.println("canon text = " + ai.getCanonText());

                // AnswerClueOverlap

                // FocusGenerator
                Collection<Focus> fs = JCasUtil.select(jcas.getView("Answer"), Focus.class);
                //Assert.assertEquals("Rowling", f.getCoveredText());
                for (Focus f : fs) {
                    System.err.println("focus = " + f.getCoveredText());
                }
                if (fs.isEmpty()) {
                    System.err.println("NO focus!");
                }

                // LATs
                Set<String> latTexts = extractLats(jcas);
                //Assert.assertEquals("communicator,person,writer", StringUtils.join(latTexts, ","));
                System.err.println("LATs = " + StringUtils.join(latTexts, ","));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private Set<String> extractLats(JCas jcas) throws CASException {
            Collection<LAT> lats = JCasUtil.select(jcas.getView("Answer"), LAT.class);
            Set<String> latTexts = new TreeSet<>();
            for (LAT lat : lats) {
                latTexts.add(lat.getText());
            }
            return latTexts;
        }
    }

    @Test
    public void runEN() throws Exception {
        new YodaQA();
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class),
                CAS.NAME_DEFAULT_SOFA, "Answer");
        builder.add(createPrimitiveDescription(LanguageToolLemmatizer.class),
                CAS.NAME_DEFAULT_SOFA, "Answer");
        builder.add(OpenNlpNamedEntities.createEngineDescription(),
                CAS.NAME_DEFAULT_SOFA, "Answer");

        builder.add(AnswerAnalysisAE.createEngineDescription());
        builder.add(AnalysisEngineFactory.createEngineDescription(
                DumpCAS2File.class,
                DumpCAS2File.PARAM_SAVE_DIR, dir,
                DumpCAS2File.PARAM_SUFFIX, "FullStack"));

        QUESTION = "ignored";
        ANSWER = "White House at the Spring lake";
        runPipeline(Tested.class, "ignored", builder, TestConsumer.class);
    }

    @Test
    public void runRU() throws Exception {
        new YodaQA();
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class),
                CAS.NAME_DEFAULT_SOFA, "Answer");
        builder.add(createPrimitiveDescription(TreeTaggerPosTagger.class),
                CAS.NAME_DEFAULT_SOFA, "Answer");
        builder.add(AnalysisEngineFactory.createEngineDescription(MaltParser.class),
                CAS.NAME_DEFAULT_SOFA, "Answer");
        /*builder.add(createPrimitiveDescription(SpotlightNameFinder.class,
                SpotlightNameFinder.PARAM_ENDPOINT, "http://spotlight.sztaki.hu:2227/rest/annotate"),
                CAS.NAME_DEFAULT_SOFA, "Answer");*/

        builder.add(AnswerAnalysisAE.createEngineDescription());
        builder.add(AnalysisEngineFactory.createEngineDescription(
                DumpCAS2File.class,
                DumpCAS2File.PARAM_SAVE_DIR, dir,
                DumpCAS2File.PARAM_SUFFIX, "FullStack"));

        QUESTION = "не имеет значения";
        ANSWER = "23 февраля вилку следует держать правой рукой";
        runPipeline(Tested.class, "игнор", builder, TestConsumer.class);
    }
}
