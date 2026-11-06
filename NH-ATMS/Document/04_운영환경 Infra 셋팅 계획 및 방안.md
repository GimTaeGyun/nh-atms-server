</br>

# 목차
```
1. MSA 와 멀티모듈
2. 세가지 아키텍처 비교
3. 현재 프로젝트 분석
```

> 분석일: 2026-05-20
> 대상: NH-ATMS

</br>

# <font color="#548dd4">1. MSA 와 멀티모듈</font>

</br>

## 멀티모듈(Multi-Module)

### 관점

```
"코드 구조를 어떻게 나눌 것인가?"
```
에 대한 아키텍처 관점

즉:
- 하나의 프로젝트를
- 여러 모듈로 나누어
- 책임과 의존성을 관리

</br>

### 예시

``` 
root
 ├─ common
 ├─ domain
 ├─ infra
 ├─ api
 └─ batch
```

</br>

### 장점

#### 1. 코드 구조화

- 관심사 분리 가능
#### 2. 유지보수성 향상

- 모듈 단위 관리 가능
#### 3. 협업 용이

- 팀별 모듈 담당 가능

---

## MSA (Microservice Architecture)

</br>

### 관점

```
"서비스를 어떻게 분리할 것인가?"
```
에 대한 아키텍처 관점입니다.

즉:
- 기능별로 서비스를 분리하고
- 각각 독립 운영

</br>

### 예시

```
user-service  
order-service  
payment-service  
notification-service
```

각 서비스가

```
독립 JVM
독립 배포
독립 DB(공유 DB가능)
```
를 가질 수 있다.

</br>

### 서비스 간 통신

```
HTTP
gRPC
Kafka
RabbitMQ
```
등 사용

</br>

### 장점

#### 1. 독립 배포 가능
- payment만 재배포 가능

#### 2. 독립 스케일링 가능
- 트래픽 많은 서비스만 증설

#### 3. 장애 격리
- 한 서비스 장애가 전체로 덜 전파

#### 4. 기술 독립성
- 서비스별 기술 선택 가능


</br>

### 단점

#### 1. 운영 복잡도 매우 증가

- Docker
- Kubernetes(선택)
- Gateway
#### 2. 네트워크 비용 발생
- 메서드 호출 → HTTP 호출
#### 3. 분산 트랜잭션 문제

#### 4. 개발 생산성 저하 가능

- 초기 설계 난이도 높음

</br>

---

</br>

#  <font color="#548dd4">2. 세 가지 아키텍처 비교</font>

</br>

## 1️. 단순 모놀리식 (Monolithic)

</br>

### 구조
```
nh-atms-api/
├── src/
│   ├── controller/
│   │   ├── AuthController
│   │   ├── UserController
│   │   ├── JournalController
│   │   └── BatchJobController
│   ├── service/
│   ├── entity/
│   └── mapper/
└── resources/
    └── application.yml    
```

### 장점
- ✅ 개발 단순: 모든 것이 한 프로젝트
- ✅ 배포 쉬움: 단일 JAR 파일만 빌드/배포
- ✅ 디버깅 용이: 로컬에서 한 번에 실행
- ✅ 성능: 모듈 간 네트워크 호출 X (직접 메서드 호출)
- ✅ 트랜잭션: 모든 DB 작업이 하나의 트랜잭션으로 관리

### 단점
- ❌ 확장성 제한: 한 모듈 변경 시 전체 배포 필요
- ❌ 기술 스택 고정: 새로운 기술 도입 어려움
- ❌ scaling: 부하 분산 어려움 (전체만 scale가능)
- ❌ 팀 간 독립성 부족: 다양한 팀이 개발하기 어려움
- ❌ 장애 전파: 한 모듈의 장애 → 전체 시스템 다운
- ❌ 코드 복잡도: 시간이 지날수록 복잡도 증가

 
</br>

## 2.  모놀리식 + 멀티모듈 (Monolithic Modular)

</br>

### 구조
```
nh-atms-api/
├── settings.gradle
├── build.gradle
├── atms-common/
│   ├── security/
│   ├── response/
│   └── exception/
├── atms-web/
│   ├── controller/
│   ├── service/
│   └── entity/
├── atms-batch/
│   ├── scheduler/
│   ├── job/
│   └── entity/
├── atms-journal/
│   ├── controller/
│   ├── service/
│   └── entity/
└── atms-file/
```

### 장점
- ✅ 구조화: 모듈별 명확한 책임 분리
- ✅ 코드 재사용: atms-common에서 공통 로직 관리
- ✅ 개발 생산성: 팀별로 모듈 담당 가능
- ✅ 향후 확장: MSA로 쉽게 전환 가능
- ✅ 테스트: 모듈별 단위 테스트 용이
- ✅ **배포 유연성: 원하면 단일 JAR/분할 배포 모두 가능**
- ✅ **모놀리식의 장점 유지: 트랜잭션, 성능 유지**

### 단점
- ❌ 빌드 복잡도: Gradle multi-module 이해 필요
- ❌ 디버깅: 모듈 간 호출 추적 복잡
- ❌ 배포 정책 미정: 단일/분할 배포 정책 혼재 가능
- ❌ 과설계 위험: 실제로 필요 이상으로 복잡할 수 있음

 
</br>

## 3. MSA + 멀티모듈 (Microservices Architecture)

### 구조
```
nh-atms-infra/
├── docker-compose.yml
├── kubernetes/
│   ├── atms-web-deployment.yaml
│   ├── atms-batch-deployment.yaml
│   └── atms-journal-deployment.yaml
├── atms-api/
│   ├── atms-web-service/
│   ├── atms-batch-service/
│   ├── atms-journal-service/
│   └── atms-common/
```

## 각 서비스별 독립 레포지토리 (선택)

```
atms-web-repo/
atms-batch-repo/
atms-journal-repo/
```

### 장점
- ✅ 무제한 확장: 각 서비스 독립 scaling
- ✅ 배포 자유: 서비스별 독립 배포 (CI/CD 최적화)
- ✅ **기술 다양성: 각 서비스마다 다른 tech stack 사용 가능**
- ✅ **장애 격리: 한 서비스 장애가 다른 서비스에 영향 없음**
- ✅ 팀 독립성: 각 팀이 독립적으로 개발/배포
- ✅ **모니터링 정밀함: 각 서비스별 상세 모니터링**
- ✅ 성능 최적화: 필요한 서비스만 scale/업글

### 단점
- ❌ 높은 복잡도: 분산 시스템 문제들 (네트워크, 일관성, 타이밍)
- ❌ 인프라 비용: 각 서비스마다 리소스 필요 (최소 효율 필요)
- ❌ 운영 난이도: DevOps 전문성 필요 (K8s, Docker, CI/CD)
- ❌ 디버깅 어려움: 분산 추적(Distributed Tracing) 필수
- ❌ 네트워크 지연: 서비스 간 HTTP 호출 오버헤드
- ❌ 데이터베이스 관리: DB 분산으로 인한 트랜잭션 복잡성
- ❌ 버전 관리: API 버전 관리 복잡

</br>

---

</br>

#  <font color="#548dd4">3. 현재 프로젝트 분석</font>

> MG새마을 ATMS 를 기준으로 ‘단순 모놀리식에 가까운 멀티모듈 프로젝트’ 로 보인다.

근거
- atms/pom.xml (packaging = war)
- atmr/pom.xml (packaging = war)
- 위 두 개의 폼 파일만 존재하며, 공통 properties, dependencyManagement, 플러그인 설정 등을 중앙화하고 있지 않다.

현재의 프로젝트는 각 모듈별 영향도와 의존도가 크고, **프로젝트 사양 변경시 각 모듈의 재사용에도 불편함**이 있다.
이에 따라, 멀티모듈/MSA 적용을 목표로 하고있다.
## 정리
1. 관리하는 모듈 단위가 6개 이상이므로, 멀티모듈은 적용 불가피
2. 나아가 `atms-file` / `atms-web` / `atms-batch` 등 메인 모듈을 MSA를 적용하여 멀티 컨테이너로 운영한다면 얻는 장점이 클 것으로 보임
	- 장애 모니터링 및 장애시 타 모듈 영향 최소화
	- 차후 프로젝트시 모듈 재활용성 극대화(메인 서비스에 의존성 없음)
	- 국소적 버전업 가능
3. 위 사항들과 프로젝트의 규모, 일정, 인원을 고려하여 최적의 아키텍처 설계 방향 선택 필요

</br>