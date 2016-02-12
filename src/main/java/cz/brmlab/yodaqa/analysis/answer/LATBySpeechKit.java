package cz.brmlab.yodaqa.analysis.answer;

import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.*;

import cz.brmlab.yodaqa.analysis.MultiLanguageParser;
import cz.brmlab.yodaqa.model.TyCor.*;
import cz.brmlab.yodaqa.provider.SpeechKit;
import cz.brmlab.yodaqa.provider.SpeechKit.SpeechKitResponse;
import cz.brmlab.yodaqa.provider.SpeechKit.SpeechKitResponse.*;
import cz.brmlab.yodaqa.provider.SpeechKit.SpeechKitResponse.GeoAddr.Fields;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.*;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class LATBySpeechKit extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATBySpeechKit.class);

	private SpeechKit speechKit = new SpeechKit();

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* Skip an empty answer. */
		if (jcas.getDocumentText().matches("^\\s*$"))
			return;

        SpeechKitResponse result = speechKit.query(jcas.getDocumentText());
		for (Date date : result.getDate()) {
		    String label = date.toString();
            addLAT(jcas, new SpKitDateLAT(jcas), SpeechKit.getBegin(date.getTokens(), result), SpeechKit.getEnd(date.getTokens(), result), label, MultiLanguageParser.getLanguage(label));
		}
		for (Fio fio : result.getFio()) {
            String label = fio.toString();
            addLAT(jcas, new SpKitFioLAT(jcas), SpeechKit.getBegin(fio.getTokens(), result), SpeechKit.getEnd(fio.getTokens(), result), label, MultiLanguageParser.getLanguage(label));
		}
		for (GeoAddr addr : result.getGeoAddr()) {
		    for (Fields fields : addr.getFields()) {
                String label = fields.getName();
                addLAT(jcas, new SpKitGeoAddrLAT(jcas), SpeechKit.getBegin(addr.getTokens(), result), SpeechKit.getEnd(addr.getTokens(), result), label, MultiLanguageParser.getLanguage(label));
		    }
		}
	}

	protected void addLAT(JCas jcas, LAT lat, int begin, int end, String text, String language) {
	    logger.debug("SpeechKit LATs/0 {}", text);
		lat.setBegin(begin);
		lat.setEnd(end);
		List<Token> tokens = JCasUtil.selectCovering(jcas, Token.class, begin, end);
		if (tokens.size() > 0) {
		    lat.setBase(tokens.get(0));
		}

		POS pos = new NN(jcas);
        pos.setBegin(begin);
        pos.setEnd(end);
        pos.setPosValue("NNS");
        pos.addToIndexes();
        lat.setPos(pos);

		lat.setText(text);
		lat.addToIndexes();
		lat.setLanguage(language);
	}
}
