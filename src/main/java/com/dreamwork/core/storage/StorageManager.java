package com.dreamwork.core.storage;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.manager.Manager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * JSON 기반 파일 저장소 매니저
 * 
 * <p>
 * 유저 데이터를 JSON 파일로 저장하고 로드합니다.
 * 모든 파일 I/O는 비동기로 처리되어 메인 스레드 멈춤을 방지합니다.
 * </p>
 * 
 * <h2>사용 예시:</h2>
 * 
 * <pre>{@code
 * StorageManager storage = DreamWorkCore.getInstance().getStorageManager();
 * 
 * // 비동기 저장
 * PlayerData data = new PlayerData(uuid, "Player1");
 * storage.saveUserJsonAsync(uuid, data).thenRun(() -> {
 *     System.out.println("저장 완료!");
 * });
 * 
 * // 비동기 로드
 * storage.loadUserJsonAsync(uuid, PlayerData.class).thenAccept(optData -> {
 *     optData.ifPresent(d -> System.out.println("로드됨: " + d.getName()));
 * });
 * }</pre>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class StorageManager extends Manager {

    private final DreamWorkCore plugin;

    /** Gson 인스턴스 (Pretty Print 활성화) */
    private final Gson gson;

    /** 비동기 작업용 ExecutorService */
    private ExecutorService executor;

    /** 유저 데이터 폴더 */
    private File userdataFolder;

    /**
     * StorageManager 생성자
     * 
     * @param plugin 플러그인 인스턴스
     */
    public StorageManager(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .disableHtmlEscaping()
                .create();
    }

    @Override
    public void onEnable() {
        // 비동기 작업용 스레드 풀 생성
        executor = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, "DreamWork-Storage");
            thread.setDaemon(true);
            return thread;
        });

        // 유저 데이터 폴더 생성
        String folderName = plugin.getConfig().getString("storage.userdata-folder", "userdata");
        userdataFolder = new File(plugin.getDataFolder(), folderName);
        if (!userdataFolder.exists()) {
            userdataFolder.mkdirs();
            plugin.getLogger().info("유저 데이터 폴더 생성: " + userdataFolder.getAbsolutePath());
        }

        enabled = true;
        plugin.getLogger().info("StorageManager 활성화 완료 (JSON 파일 저장소)");
    }

    @Override
    public void onDisable() {
        enabled = false;

        // 남은 작업 완료 대기 후 종료
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    plugin.getLogger().warning("일부 저장 작업이 강제 종료되었습니다.");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        plugin.getLogger().info("StorageManager 비활성화 완료");
    }

    @Override
    public void reload() {
        // 폴더 설정 리로드
        String folderName = plugin.getConfig().getString("storage.userdata-folder", "userdata");
        userdataFolder = new File(plugin.getDataFolder(), folderName);
        if (!userdataFolder.exists()) {
            userdataFolder.mkdirs();
        }
    }

    /**
     * 유저 데이터를 비동기로 저장합니다.
     * 
     * <p>
     * 데이터는 {@code userdata/{uuid}.json} 형태로 저장됩니다.
     * </p>
     * 
     * @param uuid 유저 UUID
     * @param data 저장할 데이터 객체
     * @return 저장 완료 시 완료되는 CompletableFuture
     */
    public CompletableFuture<Void> saveUserJsonAsync(UUID uuid, Object data) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveUserJsonSync(uuid, data);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                        "유저 데이터 저장 실패 (UUID: " + uuid + ")", e);
            }
        }, executor);
    }

    /**
     * 유저 데이터를 동기적으로 저장합니다.
     * 
     * <p>
     * <b>주의:</b> 메인 스레드에서 호출하지 마세요!
     * </p>
     * 
     * @param uuid 유저 UUID
     * @param data 저장할 데이터 객체
     * @throws IOException 파일 저장 실패 시
     */
    public void saveUserJsonSync(UUID uuid, Object data) throws IOException {
        File file = getUserDataFile(uuid);
        String json = gson.toJson(data);

        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(json);
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] 유저 데이터 저장: " + file.getName());
        }
    }

    /**
     * 유저 데이터를 비동기로 로드합니다.
     * 
     * @param <T>   데이터 타입
     * @param uuid  유저 UUID
     * @param clazz 데이터 클래스
     * @return 로드된 데이터를 담은 Optional (파일이 없으면 empty)
     */
    public <T> CompletableFuture<Optional<T>> loadUserJsonAsync(UUID uuid, Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadUserJsonSync(uuid, clazz);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                        "유저 데이터 로드 실패 (UUID: " + uuid + ")", e);
                return Optional.empty();
            }
        }, executor);
    }

    /**
     * 유저 데이터를 동기적으로 로드합니다.
     * 
     * <p>
     * <b>주의:</b> 메인 스레드에서 호출하지 마세요!
     * </p>
     * 
     * @param <T>   데이터 타입
     * @param uuid  유저 UUID
     * @param clazz 데이터 클래스
     * @return 로드된 데이터를 담은 Optional (파일이 없으면 empty)
     * @throws IOException 파일 읽기 실패 시
     */
    public <T> Optional<T> loadUserJsonSync(UUID uuid, Class<T> clazz) throws IOException {
        File file = getUserDataFile(uuid);

        if (!file.exists()) {
            return Optional.empty();
        }

        try (Reader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            T data = gson.fromJson(reader, clazz);

            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] 유저 데이터 로드: " + file.getName());
            }

            return Optional.ofNullable(data);
        }
    }

    /**
     * 유저 데이터 파일이 존재하는지 확인합니다.
     * 
     * @param uuid 유저 UUID
     * @return 파일 존재 여부
     */
    public boolean hasUserData(UUID uuid) {
        return getUserDataFile(uuid).exists();
    }

    /**
     * 유저 데이터 파일을 비동기로 삭제합니다.
     * 
     * @param uuid 유저 UUID
     * @return 삭제 성공 여부를 담은 CompletableFuture
     */
    public CompletableFuture<Boolean> deleteUserDataAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path path = getUserDataFile(uuid).toPath();
                return Files.deleteIfExists(path);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING,
                        "유저 데이터 삭제 실패 (UUID: " + uuid + ")", e);
                return false;
            }
        }, executor);
    }

    /**
     * 일반 객체를 JSON 문자열로 변환합니다.
     * 
     * @param object 변환할 객체
     * @return JSON 문자열
     */
    public String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * JSON 문자열을 객체로 변환합니다.
     * 
     * @param <T>   대상 타입
     * @param json  JSON 문자열
     * @param clazz 대상 클래스
     * @return 변환된 객체
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /**
     * 유저 데이터 파일 객체를 반환합니다.
     * 
     * @param uuid 유저 UUID
     * @return 유저 데이터 파일
     */
    private File getUserDataFile(UUID uuid) {
        return new File(userdataFolder, uuid.toString() + ".json");
    }

    /**
     * Gson 인스턴스를 반환합니다.
     * 
     * @return Gson 인스턴스
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * 유저 데이터 폴더를 반환합니다.
     * 
     * @return 유저 데이터 폴더
     */
    public File getUserdataFolder() {
        return userdataFolder;
    }
}
