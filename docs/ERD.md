# 📘 데이터베이스 테이블 명세서

## 📦 User

| 컬럼명 | 타입 | Not Null | 기본값 | 설명 |
|--------|------|----------|--------|------|
| userId | Int | ✅ | 1 |  |
| nickname | String | ✅ |  |  |
| profileBansoogiId | Int | ✅ |  |  |
| birthDate | String | ✅ |  | "yyyy-MM-dd"의 format을 가짐 |
| wakeupTime | String | ✅ | 07:00 | "HH:mm"의 format을 가짐 |
| sleepTime | String | ✅ | 22:00 | "HH:mm"의 format을 가짐 |
| breakfastTime | String | ❌ | 08:00 | "HH:mm"의 format을 가짐 |
| lunchTime | String | ❌ | 12:00 | "HH:mm"의 format을 가짐 |
| dinnerTime | String | ❌ | 18:00 | "HH:mm"의 format을 가짐 |
| notificationDuration | Int | ✅ | 30 | 5이상 60이하의 값을 가짐 |
| createdAt | DateTime | ✅ | RealmInstant.now() |  |
| updatedAt | DateTime | ✅ | RealmInstant.now() |  |

## 📦 setting

| 컬럼명 | 타입 | Not Null | 기본값 | 설명 |
|--------|------|----------|--------|------|
| userId | Int | ✅ | 1 |  |
| notificationsEnabled | Boolean | ✅ |  |  |
| backgroundSoundEnabled | Boolean | ✅ |  |  |
| effectSoundEnabled | Boolean | ✅ |  |  |

## 📦 Bansoogi

| 컬럼명 | 타입 | Not Null | 기본값 | 설명 |
|--------|------|----------|--------|------|
| bansoogiId | Int | ✅ |  |  |
| title | String | ✅ |  |  |
| imageUrl | String | ✅ |  |  |
| silhouetteImageUrl | String | ❌ |  |  |
| gifUrl | String | ✅ |  |  |
| description | String | ✅ |  |  |

## 📦 unlockBansoogiInfo

| 컬럼명 | 타입 | Not Null | 기본값 | 설명 |
|--------|------|----------|--------|------|
| unlockId | Int | ✅ |  | "$infoId-$bansoogiId" 형태로 저장 |
| bansoogiId | Int | ✅ |  |  |
| acquisitionCount | Int | ✅ | 1 |  |
| createdAt | DateTime | ✅ | RealmInstant.now() | 최초 획득일 |
| updatedAt | DateTime | ✅ | RealmInstant.now() | 최근 획득일 |

## 📦 ActivityCode

| 컬럼명 | 타입 | Not Null | 기본값 | 설명 |
|--------|------|----------|--------|------|
| code | String | ✅ |  |  |
| label | String | ✅ |  | 사용자가 확인하는 행동 이름 |
| rewardEnergyPoint | Int | ✅ |  | 기준 충족 시 지급되는 에너지 |
| type | String | ✅ | enum('BEHAVIOR_CHANGE' | 행동 구분 |
| thresholdValue | Int | ❌ |  | 해당 값이 될 때마다 점수를 부여 |
| thresholdUnit | String | ❌ | enum('COUNT' | 행동 단위에 대한 값 |
| maxEnergyPointPerDay | Int |❌  |  |  |

## 📦 ActivityLog

| 컬럼명 | 타입 | Not Null | 기본값 | 설명 |
|--------|------|----------|--------|------|
| logId | Int | ✅ |  |  |
| code | String | ✅ |  |  |
| Field | String | ❌ |  |  |
| reactedAt | DateTime | ✅ | RealmInstant.now() |  |

## 📦 CurrentActivityState

| 컬럼명 | 타입 | Not Null | 기본값 | 설명 |
|--------|------|----------|--------|------|
| stateId | Int | ✅ |  |  |
| currentEnergyPoint | Int | ✅ | 0 |  |
| standupCount | Int | ✅ |  |  |
| stretchCount | Int | ✅ |  |  |
| phoneOffCount | Int | ✅ |  |  |
| lyingTime | Int | ✅ |  |  |
| sittingTime | Int | ✅ |  |  |
| phoneTime | Int | ✅ |  |  |
| sleepTime | Int | ❌ |  |  |
| breakfast | Boolean | ❌ | false |  |
| lunch | Boolean | ❌ | false |  |
| dinner | Boolean | ❌ | false |  |
| interactionCount | Int | ✅ | 0 |  |
| interactionTime | DateTime | ✅ |  |  |
| isColosed | Boolean | ✅ | false |  |
| createdAt | DateTime | ✅ | RealmInstant.now() |  |
| updatedAt | DateTime | ✅ | RealmInstant.now() |  |

## 📦 ActivityReport

| 컬럼명 | 타입 | Not Null | 기본값 | 설명 |
|--------|------|----------|--------|------|
| reportId | Int | ✅ |  |  |
| finalEnergyPoint | Int | ✅ |  |  |
| standupCount | Int | ✅ |  |  |
| stretchCount | Int | ✅ |  |  |
| phoneOffCount | Int | ✅ |  |  |
| lyingTime | Int | ✅ |  |  |
| sittingTime | Int | ✅ |  |  |
| phoneTime | Int | ✅ |  |  |
| sleepTime | Int | ❌ |  |  |
| walkCount | Int | ✅ |  |  |
| runTime | Int | ✅ |  |  |
| exerciseTime | Int | ✅ |  |  |
| stairsClimbed | Int | ✅ |  |  |
| breakfast | Boolean | ❌ | false |  |
| lunch | Boolean | ❌ | false |  |
| dinner | Boolean | ❌ | false |  |
| reportedAt | DateTime | ✅ | RealmInstant.now() |  |
