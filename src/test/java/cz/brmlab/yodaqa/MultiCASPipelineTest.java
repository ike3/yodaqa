package cz.brmlab.yodaqa;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.*;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.analysis.MultiLanguageParser;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.pipeline.CleanupTempFiles;

public abstract class MultiCASPipelineTest {

    protected void runPipeline(Class<? extends CasCollectionReader_ImplBase> tested, String input,
            AggregateBuilder builder, Class<? extends JCasConsumer_ImplBase> consumer)
            throws UIMAException, IOException, ResourceInitializationException {
        builder.add(createPrimitiveDescription(CleanupTempFiles.class));

        CollectionReaderDescription reader = createReaderDescription(
                tested,
                SimpleQuestion.PARAM_LANGUAGE, MultiLanguageParser.getLanguage(input),
                SimpleQuestion.PARAM_INPUT, input)
                ;
        MultiCASPipeline.runPipeline(reader, builder.createAggregateDescription(), createEngineDescription(consumer));
    }

}
