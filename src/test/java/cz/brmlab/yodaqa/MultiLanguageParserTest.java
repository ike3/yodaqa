package cz.brmlab.yodaqa;

import org.junit.*;

import cz.brmlab.yodaqa.analysis.MultiLanguageParser;

public class MultiLanguageParserTest {
    @Test
    public void getLanguageRu() throws Exception {
        String text = "Глава 15 КУЛЬТУРА ПОВЕДЕНИЯ В ТРАНСПОРТЕ Какие же вы после этого наездники, черт бы вас побрал! — воскликнул Гаргантюа. — Вам только на клячах и ездить. Если бы вам предстояло путешествие в Каюзак, что бы вы предпочли: ехать верхом на гусенке или же свинью вести на веревочке? Франсуа Рабле. Гаргантюа и Пантагрюэль Путешествовать — занятие подчас довольно утомительное, хлопотное и тревожное. Отвратительна тряска в троллейбусе или автобусе. Не слишком приятно сутками сидеть в душном купе поезда. Тревожно и неуютно чувствуешь себя в самолете, качает на корабле… А если прибавить к этому наличие шумных, пьяных или «ароматизирующих» соседей, то поездка может стать и вовсе невыносимой. Так что нам с вами, приличным людям, следует свести свои мучения к минимуму и постараться наладить хотя бы временный мир со своими попутчиками.";
        String lang = MultiLanguageParser.getLanguage(text);
        Assert.assertEquals("ru", lang);
    }

    @Test
    public void getLanguageEn() throws Exception {
        String text = "Welcome to the hell! - this is from my book.";
        String lang = MultiLanguageParser.getLanguage(text);
        Assert.assertEquals("en", lang);
    }
}
