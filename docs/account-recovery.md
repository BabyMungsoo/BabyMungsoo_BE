# 계정 복구 API

모든 요청은 로그인 없이 `Content-Type: application/json`으로 호출합니다. Swagger: `/swagger-ui.html`.

| POST 경로 | 요청 JSON | 성공 응답 |
| --- | --- | --- |
| `/api/v1/auth/find-id` | `{"name":"홍길동","phone":"01012345678"}` | 200 `{"maskedEmails":["me***@example.com"]}` |
| `/api/v1/auth/password-reset/request` | `{"email":"member@example.com"}` | 202, 본문 없음 |
| `/api/v1/auth/password-reset/confirm` | `{"token":"메일로 받은 토큰","newPassword":"newPassword1"}` | 204, 본문 없음 |

아이디는 가입 이메일입니다. 이름 앞뒤 공백과 전화번호 구분 기호를 제거하여 비교합니다. 전화번호를 등록한 이메일 가입 계정만 찾을 수 있고, 일치하지 않으면 빈 목록을 반환합니다. 마스킹된 이메일은 본인 인증 수단이 아닙니다.

비밀번호 재설정 요청은 미가입·소셜 로그인 계정에도 동일한 202를 반환하지만 메일을 보내지 않습니다. 메일의 토큰을 재설정 화면에 붙여넣고 새 비밀번호와 함께 confirm API로 전송합니다. 토큰은 15분 유효하고 한 번만 사용 가능하며, DB에는 SHA-256 해시만 저장합니다. 재발급 시 이전 토큰은 무효화됩니다. 계정당 60초 이내의 재요청은 메일 발송 없이 202를 반환합니다.

비밀번호는 8~64자이며 BCrypt 제한에 따라 UTF-8 72바이트 이하여야 합니다. 유효하지 않거나 만료·사용된 토큰은 400 `INVALID_PASSWORD_RESET_TOKEN`, 입력 오류는 400, 메일 미설정/발송 실패는 503 `RECOVERY_MAIL_UNAVAILABLE`입니다. 메일 발송 실패 시 토큰 변경을 롤백합니다.

위 오류 코드는 서버 내부 구분이며, 실제 응답은 기존 공통 오류 형식인 `{"success":false,"data":null,"message":"재설정 토큰이 유효하지 않거나 만료되었습니다."}`입니다.

## SMTP 설정

기존 `.env`에 아래 값을 실제 SMTP 서비스 정보로 추가합니다. 비밀번호는 SMTP 서비스가 발급한 인증 정보를 사용합니다.

```dotenv
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-user
MAIL_PASSWORD=your-smtp-password
MAIL_FROM=no-reply@example.com
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
```

SMTP 설정 전에도 서버는 기동할 수 있지만 비밀번호 재설정 메일 요청은 503을 반환합니다. 테스트는 SMTP를 모킹하므로 실제 메일을 보내지 않습니다.

개발 프로필의 `ddl-auto: update`는 `users`에 `password_reset_hash`(varchar 64, unique), `password_reset_expires_at`(timestamp with time zone), `password_reset_requested_at`(timestamp with time zone) 컬럼을 추가합니다. 스키마 자동 변경을 사용하지 않는 환경은 배포 전에 동일한 컬럼/제약을 마이그레이션해야 합니다.

기존 JWT는 비밀번호 변경 후에도 원래 만료 시각까지 유효합니다. 아이디 조회 및 미가입 이메일 요청을 포함한 IP 단위 요청 제한은 API 게이트웨이에서 적용해야 합니다.

검증: `./gradlew test --tests '*AccountRecoveryIntegrationTest'` (외부 DB·SMTP 없이 H2 사용).
