package cz.brmlab.yodaqa;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;
import cz.brmlab.yodaqa.io.interactive.InteractiveAnswerPrinter;
import cz.brmlab.yodaqa.pipeline.YodaQA;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.junit.Test;

import java.io.IOException;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

public class RunRuPipelineTest {

    @Test
    public void testRuPipeline() throws UIMAException, IOException {
        System.setProperty("cz.brmlab.yodaqa.pipline_ru", "true");
        System.setProperty("cz.brmlab.yodaqa.spotlight_name_finder_endpoint", "http://spotlight.sztaki.hu:2227/rest/annotate");
        System.setProperty("cz.brmlab.yodaqa.fuzzy_lookup_url", "http://localhost:5000");

        AnalysisEngineDescription pipeline = YodaQA.createEngineDescription();

        CollectionReaderDescription reader = createReaderDescription(
                SimpleQuestion.class,
                SimpleQuestion.PARAM_LANGUAGE, Language.RUSSIAN,
                SimpleQuestion.PARAM_INPUT, "Где родился Лучано Паваротти?");

        AnalysisEngineDescription printer = createEngineDescription(InteractiveAnswerPrinter.class);

        ParallelEngineFactory.registerFactory();

        MultiCASPipeline.runPipeline(reader, pipeline, printer);
    }

}
