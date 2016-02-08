package cz.brmlab.yodaqa.provider;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang.time.DateUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.slf4j.*;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class SpeechKit {
    final Logger logger = LoggerFactory.getLogger(SpeechKit.class);

    public static class SpeechKitResponse implements Serializable {
        public static class Tokens {
            int Begin, End;

            public int getBegin() {
                return Begin;
            }

            public int getEnd() {
                return End;
            }

        }
        public static class Date {
            int Day, Month, Year;
            Tokens Tokens;

            public int getDay() {
                return Day;
            }

            public int getMonth() {
                return Month;
            }

            public int getYear() {
                return Year;
            }

            public Tokens getTokens() {
                return Tokens;
            }

            @Override
            public String toString() {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DATE, Day);
                cal.set(Calendar.MONTH, Month - 1);
                cal.set(Calendar.YEAR, Year);
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM YYYY", Locale.ENGLISH);
                return sdf.format(DateUtils.truncate(cal.getTime(), Calendar.DATE));
            }
        }
        Date[] Date = new Date[0];

        public static class GeoAddr {
            public static class Fields {
                String Name;

                public String getName() {
                    return Name;
                }

            }
            Fields[] Fields = new Fields[0];
            Tokens Tokens;
            public Fields[] getFields() {
                return Fields;
            }
            public Tokens getTokens() {
                return Tokens;
            }

        }
        GeoAddr[] GeoAddr = new GeoAddr[0];

        public static class Fio {
            String FirstName;
            String LastName;
            Tokens Tokens;

            public String getFirstName() {
                return FirstName;
            }

            public String getLastName() {
                return LastName;
            }

            public Tokens getTokens() {
                return Tokens;
            }

            @Override
            public String toString() {
                return FirstName + " " + LastName;
            }

        }

        Fio[] Fio = new Fio[0];

        public Date[] getDate() {
            return Date;
        }

        public GeoAddr[] getGeoAddr() {
            return GeoAddr;
        }

        public Fio[] getFio() {
            return Fio;
        }


    }

    public SpeechKitResponse query(String documentText) throws AnalysisEngineProcessException {
        try {
            String encodedText = URLEncoder.encode(documentText, "UTF-8").replace("+", "%20");
            String requestURL = String.format("%s?text=%s&key=%s",
                    System.getProperty("cz.brmlab.yodaqa.speech_kit_endpoint"),
                    encodedText,
                    System.getProperty("cz.brmlab.yodaqa.speech_kit_key"));
            logger.debug("{}: Sending request to {}", documentText, requestURL);
            URL request = new URL(requestURL);
            URLConnection connection = request.openConnection();
            Gson gson = new Gson();
            JsonReader jr = new JsonReader(new InputStreamReader(connection.getInputStream()));
            return gson.fromJson(jr, SpeechKitResponse.class);
        } catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }

    }


}
