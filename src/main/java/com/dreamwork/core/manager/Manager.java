package com.dreamwork.core.manager;

/**
 * 모든 매니저의 상위 추상 클래스
 * 
 * <p>DreamWork Core의 모든 매니저 클래스는 이 클래스를 상속받아
 * 일관된 라이프사이클 관리를 제공합니다.</p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public abstract class Manager {
    
    /** 매니저 활성화 상태 */
    protected boolean enabled = false;
    
    /**
     * 매니저를 활성화합니다.
     * 
     * <p>플러그인이 활성화될 때 호출됩니다.
     * 리소스 초기화, 이벤트 리스너 등록 등을 수행합니다.</p>
     */
    public abstract void onEnable();
    
    /**
     * 매니저를 비활성화합니다.
     * 
     * <p>플러그인이 비활성화될 때 호출됩니다.
     * 리소스 정리, 데이터 저장 등을 수행합니다.</p>
     */
    public abstract void onDisable();
    
    /**
     * 매니저 설정을 리로드합니다.
     * 
     * <p>/dw reload 명령어 실행 시 호출됩니다.
     * 설정 파일을 다시 읽고 적용합니다.</p>
     */
    public abstract void reload();
    
    /**
     * 매니저가 활성화되어 있는지 확인합니다.
     * 
     * @return 활성화 상태
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 매니저 이름을 반환합니다.
     * 
     * <p>로깅 및 디버깅 용도로 사용됩니다.</p>
     * 
     * @return 매니저 이름
     */
    public String getName() {
        return getClass().getSimpleName();
    }
}
