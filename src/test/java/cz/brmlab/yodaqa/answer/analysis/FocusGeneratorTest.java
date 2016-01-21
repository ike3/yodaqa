package cz.brmlab.yodaqa.answer.analysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.*;

import cz.brmlab.yodaqa.SimpleQuestion;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.*;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/*
 * Ищет фокус на основе зависимостей
 *
 * T1, T2, R
 * R (зависит от) T1
 * R (зависит от) T2
 * фокусом будет R (рутовый токен)
 */
public class FocusGeneratorTest {
    private static Token root;

    public static class Tested extends SimpleQuestion {

        public void initCas(JCas jcas) {
            try {
                jcas.setDocumentLanguage("ru");

                AnswerInfo ai = new AnswerInfo(jcas);
                ai.setCanonText("правило правая нога ппн");
                ai.addToIndexes(jcas);

                Token t1 = new Token(jcas, 0, 1);
                t1.addToIndexes(jcas);

                Token t2 = new Token(jcas, 2, 3);
                t2.addToIndexes(jcas);

                root = new Token(jcas, 4, 5);
                root.addToIndexes(jcas);

                Dependency d1 = new Dependency(jcas);
                d1.setGovernor(root);
                d1.setDependent(t1);
                d1.addToIndexes(jcas);

                Dependency d2 = new Dependency(jcas);
                d2.setGovernor(root);
                d2.setDependent(t2);
                d2.addToIndexes(jcas);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TestConsumer extends JCasConsumer_ImplBase {
        @Override
        public void process(JCas jcas) throws AnalysisEngineProcessException {
            try {
                Focus f = JCasUtil.selectSingle(jcas, Focus.class);
                Assert.assertEquals(root, f.getToken());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void run() throws Exception {
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createPrimitiveDescription(cz.brmlab.yodaqa.analysis.answer.FocusGenerator.class));

        CollectionReaderDescription reader = createReaderDescription(
                Tested.class,
                SimpleQuestion.PARAM_LANGUAGE, "ru",
                SimpleQuestion.PARAM_INPUT, "это игнорируется");

        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(TestConsumer.class));
    }
}
