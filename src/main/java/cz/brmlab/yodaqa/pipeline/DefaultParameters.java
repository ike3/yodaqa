package cz.brmlab.yodaqa.pipeline;

import org.apache.commons.lang3.StringUtils;

public class DefaultParameters {
    private static final String dir = System.getProperty("java.io.tmpdir");

    public static void apply() {
        if (StringUtils.isEmpty(System.getProperty("cz.brmlab.yodaqa.spotlight_name_finder_endpoint"))) {
            System.setProperty("cz.brmlab.yodaqa.spotlight_name_finder_endpoint", "http://spotlight.sztaki.hu:2227/rest/annotate");
        }
        if (StringUtils.isEmpty(System.getProperty("cz.brmlab.yodaqa.spotlight_name_finder_enable"))) {
            System.setProperty("cz.brmlab.yodaqa.spotlight_name_finder_enable", "true");
        }

        if (StringUtils.isEmpty(System.getProperty("cz.brmlab.yodaqa.speech_kit_endpoint"))) {
            System.setProperty("cz.brmlab.yodaqa.speech_kit_endpoint", "https://vins-markup.voicetech.yandex.net/markup/0.x/");
        }
        if (StringUtils.isEmpty(System.getProperty("cz.brmlab.yodaqa.speech_kit_key"))) {
            System.setProperty("cz.brmlab.yodaqa.speech_kit_key", "5ddb8271-93c8-418c-94de-0b732da81599");
        }

        if (StringUtils.isEmpty(System.getProperty("cz.brmlab.yodaqa.fuzzy_lookup_url"))) {
            System.setProperty("cz.brmlab.yodaqa.fuzzy_lookup_url", "http://localhost:5000");
        }
        if (StringUtils.isEmpty(System.getProperty("cz.brmlab.yodaqa.cas_dump_dir"))) {
            System.setProperty("cz.brmlab.yodaqa.cas_dump_dir", dir);
        }
        if (StringUtils.isEmpty(System.getProperty("cz.brmlab.yodaqa.pipline_ru"))) {
            System.setProperty("cz.brmlab.yodaqa.pipline_ru", "true");
        }
    }
}
