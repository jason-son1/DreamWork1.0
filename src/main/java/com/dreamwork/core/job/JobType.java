package com.dreamwork.core.job;

/**
 * 직업 타입 열거형
 * <p>
 * 서버에 존재하는 5가지 직업을 정의합니다.
 * 모든 플레이어는 이 5개 직업을 동시에 가지고 성장합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public enum JobType {

    /**
     * 광부 - 대지의 개척자
     * <p>
     * 광물 채굴, 장비 수리/강화, 건축 자재 가공(합금) 담당
     * </p>
     */
    MINER("광부", "miner", "⛏"),

    /**
     * 농부 - 풍요의 공급자
     * <p>
     * 식량 공급, 버프 요리 생산, 특수 작물 재배, 목축 담당
     * </p>
     */
    FARMER("농부", "farmer", "🌾"),

    /**
     * 어부 - 심해의 탐구자
     * <p>
     * 해양 자원 공급, 특수 버프 음식(회) 생산, 수족관 납품 담당
     * </p>
     */
    FISHER("어부", "fisher", "🎣"),

    /**
     * 사냥꾼 - 야생의 수호자
     * <p>
     * 필드 사냥, 희귀 펫 테이밍, 보스 소환 및 처치, 용병 활동 담당
     * </p>
     */
    HUNTER("사냥꾼", "hunter", "🏹"),

    /**
     * 탐험가 - 지평선의 기록자
     * <p>
     * 맵 탐사, 던전 발견, 특송 배달, 좌표 판매 담당
     * </p>
     */
    EXPLORER("탐험가", "explorer", "🗺");

    private final String displayName;
    private final String configKey;
    private final String icon;

    JobType(String displayName, String configKey, String icon) {
        this.displayName = displayName;
        this.configKey = configKey;
        this.icon = icon;
    }

    /**
     * 직업의 표시 이름을 반환합니다. (한글)
     * 
     * @return 표시 이름
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 설정 파일에서 사용하는 키를 반환합니다.
     * 
     * @return 설정 키 (예: "miner")
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 직업 아이콘을 반환합니다.
     * 
     * @return 이모지 아이콘
     */
    public String getIcon() {
        return icon;
    }

    /**
     * 설정 키로부터 JobType을 찾습니다.
     * 
     * @param key 설정 키 (대소문자 무시)
     * @return JobType 또는 null
     */
    public static JobType fromConfigKey(String key) {
        if (key == null)
            return null;
        for (JobType type : values()) {
            if (type.configKey.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 이름으로부터 JobType을 찾습니다. (enum 이름 또는 한글 이름)
     * 
     * @param name 이름
     * @return JobType 또는 null
     */
    public static JobType fromString(String name) {
        if (name == null)
            return null;

        // enum 이름으로 먼저 시도
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        // 한글 이름으로 시도
        for (JobType type : values()) {
            if (type.displayName.equals(name)) {
                return type;
            }
        }

        // 설정 키로 시도
        return fromConfigKey(name);
    }
}
