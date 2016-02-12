package cz.brmlab.yodaqa;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;
import cz.brmlab.yodaqa.io.interactive.InteractiveAnswerPrinter;
import cz.brmlab.yodaqa.pipeline.YodaQA;

public class RunRuPipelineTest {

    private static final String dir = System.getProperty("java.io.tmpdir");

    public static void main(String[] args) throws Exception {
        System.setProperty("cz.brmlab.yodaqa.cas_dump_dir", dir);
        System.setProperty("cz.brmlab.yodaqa.pipline_ru", "true");

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
