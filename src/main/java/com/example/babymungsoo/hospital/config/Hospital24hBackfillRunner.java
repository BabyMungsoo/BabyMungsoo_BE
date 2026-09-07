package com.example.babymungsoo.hospital.config;

import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import com.example.babymungsoo.hospital.service.HospitalSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 로컬 개발용: 이미 저장된 병원 중 상호명에 24시간 표기가 있는 건을 기동 시 한 번 채운다.
 *
 * <p>시드 코드가 24시간 여부를 판정하도록 바뀌어도 그건 <b>새로 저장되는</b> 병원에만 적용된다.
 * {@link com.example.babymungsoo.hospital.entity.Hospital#updateFromKakao} 가 수기 보정값을
 * 지키려고 {@code is24hour} 를 건드리지 않기 때문에, 시드를 다시 돌려도 기존 행은 그대로다.
 * 그렇다고 각자 SQL 을 돌리게 하면 빠뜨리는 사람이 생겨서, 기동 때 자동으로 채운다.
 *
 * <p>이미 {@code true} 인 행은 조회 대상에서 빠지므로 서버를 몇 번 켜도 결과가 같다(멱등).
 * {@link HospitalSeedRunner} 와의 실행 순서는 상관없다 — 먼저 돌면 채울 게 없고,
 * 나중에 돌면 시드가 이미 올바른 값으로 넣어 둔 뒤라 역시 채울 게 없다.
 *
 * <p>기존 데이터를 한 번 정리하기 위한 코드다. 팀원 로컬이 모두 넘어가면 지워도 된다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Hospital24hBackfillRunner implements CommandLineRunner {

    private final HospitalRepository hospitalRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 판정은 시드와 같은 함수를 쓴다. 규칙이 갈라지면 시드와 백필 결과가 달라진다.
        List<Hospital> targets = hospitalRepository.findByIs24hourFalse().stream()
                .filter(hospital -> HospitalSeedService.looksOpen24Hours(hospital.getHospitalName()))
                .toList();

        if (targets.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        targets.forEach(hospital -> hospital.markOpen24Hours(now));

        log.info("상호명 기준 24시간 병원 {}건을 채웠습니다.", targets.size());
    }
}
