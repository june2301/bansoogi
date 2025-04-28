# 🧪 제목(ex: UserService 단위 테스트)

## 📌 테스트 대상
- 대상이 되는 서비스 레이어의 클래스명(ex: UserService)
- 테스트 메서드:
  - 테스트를 시행하는 메서드들의 메서드명 작성(ex: findById_Success, findById_Fail_UserNotFound)

## 📝 테스트 시나리오
### 성공 케이스
- [ ] 성공 테스트 케이스 작성(ex: ID로 사용자 조회 성공, 회원가입 성공 등)

### 실패 케이스
- [ ] 실패 테스트 케이스 작성(ex: 존재하지 않는 사용자 ID 조회, 회원가입 실패 등)

## 🔍 테스트 고려사항
- mocking 및 stubbing 하는 객체 설명(ex: UserRepository, PasswordEncoder Mock 객체 설정)
- 어떤 Exception을 검증했는지 설명(ex: UserNotFoundException, DuplicateUsernameException)

## ✅ 완료 조건
- [ ] given-when-then 형식으로 코드 작성
- [ ] Mock 객체 검증 구문 추가
- [ ] 예외 처리 검증
- [ ] 테스트 설명을 위한 @DisplayName 추가

## 🤔 참고사항
- 관련 클래스: 
- 테스트 프레임워크: JUnit 5, Mockito
