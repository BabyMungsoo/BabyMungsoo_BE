package com.example.babymungsoo.media.repository;

import com.example.babymungsoo.media.entity.MediaFile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    Optional<MediaFile> findByIdAndUserId(Long id, Long userId);

    // 같은 미디어에 대한 동시 분석 요청을 직렬화해, MediaAnalysis 중복 생성(유니크 제약 위반)을 막기 위한 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MediaFile> findWithLockByIdAndUserId(Long id, Long userId);

    // 문진 세션 생성 시 여러 사진을 한 번에 연결하면서, 같은 사진이 동시에 다른 세션에도 붙는 걸 막기 위한 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MediaFile> findWithLockByIdInAndUserId(List<Long> ids, Long userId);

    // 정렬을 명시하지 않으면 DB가 돌려주는 순서에 의존해 프롬프트의 "사진 1, 사진 2" 라벨이
    // 호출마다 달라질 수 있다. 업로드 순서(id 오름차순)로 고정한다.
    List<MediaFile> findAllBySessionIdOrderByIdAsc(Long sessionId);
}
