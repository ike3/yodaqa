package cz.brmlab.yodaqa.analysis;

public enum QuestionWord {

    PERSON("person", "существо"),
    TIME("time", "время"),
    DATE("date", "дата"),
    LOCATION("location", "местоположение"),
    AMOUNT("amount", "количество");

    private String engText;
    private String ruText;

    private QuestionWord(String engText, String ruText) {
        this.engText = engText;
        this.ruText = ruText;
    }

    public static QuestionWord getQuestionWordByText(String text) {
        for(QuestionWord questionWord: QuestionWord.values()) {
            if(questionWord.getEngText().equals(text) || questionWord.getRuText().equals(text)) {
                return questionWord;
            }
        }
        return null;
    }

    public String getEngText() {
        return engText;
    }

    public String getRuText() {
        return ruText;
    }

}
