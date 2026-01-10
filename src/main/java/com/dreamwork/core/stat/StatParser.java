package com.dreamwork.core.stat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 아이템 Lore/텍스트에서 스탯을 파싱하는 엔진
 * 
 * <p>
 * 정규표현식을 사용하여 "힘: +10", "채굴 속도: +5%" 등의 패턴을 인식합니다.
 * </p>
 * 
 * <h2>지원 패턴:</h2>
 * <ul>
 * <li>{@code "힘: +10"} or {@code "STR: +10"}</li>
 * <li>{@code "공격력: 15"} (부호 없음)</li>
 * <li>{@code "치명타: +5%"} (퍼센트)</li>
 * </ul>
 */
public class StatParser {

    // 정수 패턴: "스탯이름: +숫자" 또는 "스탯이름: 숫자"
    private static final String INT_PATTERN_TEMPLATE = "(?i)%s\\s*[:：]\\s*[+]?(\\d+)";

    // 퍼센트 패턴: "스탯이름: +숫자%"
    private static final String PERCENT_PATTERN_TEMPLATE = "(?i)%s\\s*[:：]\\s*[+]?([\\d.]+)\\s*%%";

    // 스탯 이름 별칭 매핑
    private static final String[][] STAT_ALIASES = {
            { "str", "힘", "strength", "공격력" },
            { "dex", "민첩", "dexterity", "속도" },
            { "con", "체력", "constitution", "생명력", "hp" },
            { "int", "지능", "intelligence", "마력" },
            { "luck", "행운", "럭", "luk" },
            { "crit", "치명타", "critical", "크리티컬" },
            { "mining", "채굴", "mining_speed", "채굴속도" },
            { "fishing", "낚시", "fishing_speed", "낚시속도" }
    };

    /**
     * 텍스트에서 정수 스탯 값을 파싱합니다.
     * 
     * @param line     파싱할 텍스트 라인
     * @param statName 스탯 이름 (대소문자 무시)
     * @return 파싱된 정수 값 (없으면 0)
     */
    public int parseInt(String line, String statName) {
        if (line == null || statName == null)
            return 0;

        String pattern = buildPattern(INT_PATTERN_TEMPLATE, statName);
        Matcher matcher = Pattern.compile(pattern).matcher(stripColorCodes(line));

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 텍스트에서 퍼센트 스탯 값을 파싱합니다.
     * 
     * @param line     파싱할 텍스트 라인
     * @param statName 스탯 이름
     * @return 파싱된 퍼센트 값 (없으면 0.0)
     */
    public double parsePercent(String line, String statName) {
        if (line == null || statName == null)
            return 0.0;

        String pattern = buildPattern(PERCENT_PATTERN_TEMPLATE, statName);
        Matcher matcher = Pattern.compile(pattern).matcher(stripColorCodes(line));

        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * 텍스트가 특정 스탯 패턴과 일치하는지 확인합니다.
     * 
     * @param line     텍스트 라인
     * @param statName 스탯 이름
     * @return 매칭 여부
     */
    public boolean matches(String line, String statName) {
        if (line == null || statName == null)
            return false;

        String intPattern = buildPattern(INT_PATTERN_TEMPLATE, statName);
        String percentPattern = buildPattern(PERCENT_PATTERN_TEMPLATE, statName);
        String cleanLine = stripColorCodes(line);

        return Pattern.compile(intPattern).matcher(cleanLine).find() ||
                Pattern.compile(percentPattern).matcher(cleanLine).find();
    }

    /**
     * 스탯 이름과 모든 별칭을 OR 패턴으로 결합합니다.
     */
    private String buildPattern(String template, String statName) {
        StringBuilder aliasPattern = new StringBuilder();
        aliasPattern.append("(?:");
        aliasPattern.append(Pattern.quote(statName));

        // 별칭 추가
        for (String[] aliases : STAT_ALIASES) {
            for (String alias : aliases) {
                if (alias.equalsIgnoreCase(statName)) {
                    // 이 그룹의 모든 별칭 추가
                    for (String a : aliases) {
                        aliasPattern.append("|").append(Pattern.quote(a));
                    }
                    break;
                }
            }
        }
        aliasPattern.append(")");

        return String.format(template, aliasPattern.toString());
    }

    /**
     * 마인크래프트 색상 코드를 제거합니다.
     */
    private String stripColorCodes(String text) {
        if (text == null)
            return "";
        // §X 및 &X 형식 제거
        return text.replaceAll("[§&][0-9a-fk-orA-FK-OR]", "");
    }

    /**
     * 모든 기본 스탯을 한 번에 파싱합니다.
     * 
     * @param lore 아이템 Lore 전체
     * @return 파싱된 스탯 배열 [str, dex, con, int, luck]
     */
    public int[] parseAllStats(java.util.List<String> lore) {
        int[] stats = new int[5]; // str, dex, con, int, luck

        if (lore == null)
            return stats;

        for (String line : lore) {
            stats[0] += parseInt(line, "str");
            stats[1] += parseInt(line, "dex");
            stats[2] += parseInt(line, "con");
            stats[3] += parseInt(line, "int");
            stats[4] += parseInt(line, "luck");
        }

        return stats;
    }
}
