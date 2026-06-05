# Spring Boot Social Login Module

Spring Boot 기반의 OAuth2 소셜 로그인 + JWT 인증 모듈입니다.  
Google, Kakao, Naver, Facebook, GitHub 로그인을 같은 흐름으로 처리하고, 로그인 성공 후 우리 서비스의 JWT Access Token과 Refresh Token을 발급합니다.

## 아키텍처 구조

이 모듈은 `domain`(비즈니스 영역)과 `global`(공통 인프라)로 나뉩니다. provider별 차이는 `global/oauth` 안쪽에 가두고, 바깥 레이어(Controller, Service)는 provider를 몰라도 되게 설계했습니다.

```text
com.socialogin.module
├── domain                      # 실제 비즈니스 영역
│   ├── auth                    # 소셜 로그인 유스케이스
│   │   ├── controller          # OAuthController        : HTTP API (얇게 유지)
│   │   ├── service             # OAuthService           : 로그인 흐름 전체 오케스트레이션
│   │   ├── dto                 # 요청/응답 record
│   │   ├── entity              # RefreshToken
│   │   └── repository          # RefreshTokenRepository
│   └── user                    # 회원 영역
│       ├── entity              # User, UserRole
│       └── repository          # UserRepository
└── global                      # 공통 인프라 (provider 차이를 여기 가둠)
    ├── oauth
    │   ├── OAuthProvider        # 지원 provider enum + path 매핑
    │   ├── OAuthProperties      # application.yml 바인딩
    │   ├── client              # OAuthClient(계약) + Abstract + provider별 구현 + Factory
    │   └── userinfo            # OAuthUserInfo(계약) + provider별 응답 변환
    ├── jwt                     # JwtTokenProvider        : 자체 JWT 발급/검증
    ├── security               # SecurityConfig, JwtAuthenticationFilter
    ├── config                 # OpenApiConfig (Swagger)
    ├── entity                 # GlobalEntity, GlobalEntityListener (생성/수정 시각)
    ├── exception              # ErrorCode, GlobalExceptionHandler, 도메인 예외
    └── rsdata                 # RsData (공통 응답 wrapper)
```

핵심 실행 흐름은 한 방향으로 흐릅니다.

```text
HTTP 요청
  → OAuthController          (HTTP만 담당)
  → OAuthService             (로그인 유스케이스 조립)
  → OAuthClientFactory       (provider에 맞는 Client 선택)
  → AbstractOAuthClient + {Provider}OAuthClient
                             (authorize URL 생성 / code→token 교환 / user-info 조회)
  → {Provider}UserInfo       (provider 응답 → 공통 OAuthUserInfo 변환)
  → UserRepository           (provider+providerId로 로그인 또는 신규 가입)
  → JwtTokenProvider         (우리 서비스 AT/RT 발급)
  → TokenResponse            (RsData로 감싸 반환)
```

레이어를 이렇게 나눈 이유는 새 provider를 추가할 때 `global/oauth` 안쪽(enum, 설정, Client, UserInfo)만 손대면 되고, `OAuthService`와 Controller는 그대로 두기 위해서입니다. 파일 단위 책임은 아래 [파일별 역할](#파일별-역할)에 정리되어 있습니다.

## 먼저 읽는 순서

소셜 로그인 방식은 이미 알고 있고 코드를 공부하려는 목적이라면 아래 순서로 읽는 것이 가장 좋습니다.

1. `src/main/resources/application.yml`
2. `global/oauth/OAuthProvider.java`
3. `global/oauth/OAuthProperties.java`
4. `global/oauth/client/OAuthClient.java`
5. `global/oauth/client/AbstractOAuthClient.java`
6. `global/oauth/client/*OAuthClient.java`
7. `global/oauth/userinfo/*UserInfo.java`
8. `domain/auth/service/OAuthService.java`
9. `domain/auth/controller/OAuthController.java`
10. `global/jwt/JwtTokenProvider.java`
11. `global/security/JwtAuthenticationFilter.java`
12. `global/security/SecurityConfig.java`

이 순서가 좋은 이유는 `설정 -> provider 구분 -> 외부 API 호출 -> 응답 변환 -> 로그인 비즈니스 로직 -> HTTP API -> JWT/Security` 순서로 실제 실행 흐름과 거의 같기 때문입니다.

## 소셜 로그인 시퀀스 다이어그램

![소셜 로그인 시퀀스 다이어그램](docs/social-login-sequence.svg)

아래 흐름은 프론트엔드가 provider callback에서 `code`를 받은 뒤 백엔드 `POST /api/auth/oauth/{provider}/login`으로 전달하는 SPA 방식입니다.

기본 redirect URI는 `http://localhost:3000/oauth/callback/{provider}`입니다. provider callback 화면에는 provider가 발급한 `code`와 `state`만 남고, 우리 서비스 JWT는 callback URL에서 직접 노출되지 않습니다.

예시:

```properties
GOOGLE_REDIRECT_URI=http://localhost:3000/oauth/callback/google
```

### 단계별 설명

위 다이어그램을 글로 풀면 다음과 같습니다. 실제 코드 위치도 함께 적었습니다.

1. **로그인 버튼 클릭** — 사용자가 "Google로 로그인" 같은 버튼을 누릅니다.
2. **`GET /login-url`** — 프론트엔드가 백엔드에 provider 로그인 URL을 요청합니다. (`OAuthController.getLoginUrl`)
3. **authorize URL 생성** — 백엔드가 `client_id`, `redirect_uri`, `scope`, `state`, `response_type=code`를 붙여 provider 로그인 URL을 만듭니다. (`OAuthService.getLoginUrl` → `OAuthClient.generateLoginUrl`)
4. **loginUrl + state 수신** — 프론트엔드가 응답의 `loginUrl`로 브라우저를 이동시킵니다.
5. **provider 로그인 / 동의** — 사용자가 소셜 계정으로 로그인하고 권한에 동의합니다.
6. **code + state 수신** — provider가 프론트엔드 callback URL(`/oauth/callback/{provider}?code=...&state=...`)로 돌려보냅니다.
7. **`POST /login`** — 프론트엔드가 필요 시 `state`를 검증한 뒤 `code`만 백엔드로 보냅니다. (`OAuthController.login`)
8. **code → token 교환** — 백엔드가 `code`를 provider access token으로 교환합니다. (`OAuthClient.requestAccessToken`)
9. **사용자 정보 조회 / 가입 처리** — provider access token으로 user-info를 조회하고, `provider + providerId`로 기존 회원 로그인 또는 신규 가입한 뒤 우리 서비스 AT/RT를 발급합니다. (`OAuthService.login` → `findOrCreateUser` → `JwtTokenProvider`)
10. **Access Token / Refresh Token 수신** — 프론트엔드가 JWT를 저장하고 이후 API 호출의 `Authorization` 헤더에 사용합니다.

### 왜 2단계(code 수신 → POST /login)로 나눴는가

토큰 발급은 오직 `POST /api/auth/oauth/{provider}/login`에서만 합니다. provider가 직접 호출하는 backend callback(`GET /api/auth/oauth/{provider}/callback`)은 provider 값만 검증하고 **JWT를 발급하지 않은 채 빈 200 응답**만 반환합니다. (`OAuthController.callback`)

이렇게 나눈 이유:

- JWT가 callback URL이나 브라우저 주소창/히스토리에 직접 노출되는 것을 막기 위해서입니다. `code`만 URL에 남고, JWT는 별도 `POST` 요청의 JSON 응답으로만 전달됩니다.
- backend callback이 `204`가 아니라 `200` 빈 본문을 반환하는 이유는, 브라우저가 최상위 navigation 중 `204`를 받으면 이전(provider 동의) 화면에 머물러 주소창의 `code`를 볼 수 없기 때문입니다.

## OOP / SOLID 적용 방향

이 프로젝트는 아래 기준으로 역할을 나눴습니다.

| 원칙 | 코드에 반영된 방식 |
| --- | --- |
| SRP | Controller는 HTTP, Service는 로그인 흐름, Client는 provider 통신, UserInfo는 응답 변환만 담당합니다. |
| OCP | provider 추가 시 `OAuthProvider`, 설정, `OAuthClient`, `UserInfo`만 추가하고 `OAuthService`는 거의 건드리지 않습니다. |
| LSP | 모든 provider Client는 `OAuthClient` 계약대로 URL 생성, token 교환, user-info 조회를 수행합니다. |
| ISP | OAuth Client 계약은 로그인에 필요한 3가지 기능만 갖습니다. |
| DIP | `OAuthService`는 `GoogleOAuthClient` 같은 구체 클래스가 아니라 `OAuthClientFactory`와 `OAuthClient`에 의존합니다. |

Lombok 사용 기준:

- 단순 getter/setter는 `@Getter`, `@Setter`를 사용합니다.
- DTO는 Java `record`를 사용합니다.
- Entity 생성은 `@SuperBuilder`와 정적 팩토리 메서드로 읽기 쉽게 만듭니다.
- 직접 작성한 getter/setter는 특별한 로직이 있을 때만 남깁니다.

## 공식 문서 기준 OAuth 설정

아래 값은 provider 공식 개발자 문서 기준으로 맞춘 기본값입니다. 특별한 이유가 없으면 `.env`에서 endpoint를 바꾸지 않아도 됩니다.

### Authorization URL과 Token URL의 역할

`Authorization URL`은 사용자를 소셜 로그인 화면으로 보내는 주소입니다. 백엔드는 이 주소에 `client_id`, `redirect_uri`, `scope`, `state`, `response_type=code`를 붙여 로그인 URL을 만듭니다. 사용자는 이 URL에서 로그인하고 동의합니다. 성공하면 소셜 로그인 서버가 `redirect_uri`로 `code`를 돌려줍니다.

`Token URL`은 방금 받은 `code`를 provider access token으로 바꾸는 주소입니다. 이 요청은 사용자의 브라우저가 아니라 백엔드 서버가 보냅니다. 보통 `client_id`, `client_secret`, `redirect_uri`, `code`, `grant_type=authorization_code`를 함께 보냅니다.

`User Info URI`는 provider access token으로 사용자 정보를 조회하는 주소입니다. 백엔드는 여기서 받은 이메일, 이름, provider ID를 우리 서비스의 `User`로 변환합니다.

정리하면 `Authorization URL`은 “사용자 로그인 화면으로 보내는 주소”, `Token URL`은 “인가코드를 토큰으로 교환하는 주소”, `User Info URI`는 “로그인한 사용자가 누구인지 확인하는 주소”입니다.

| Provider | Authorization URI | Token URI | User Info URI | Scope/Permission |
| --- | --- | --- | --- | --- |
| Google | `https://accounts.google.com/o/oauth2/v2/auth` | `https://oauth2.googleapis.com/token` | `https://openidconnect.googleapis.com/v1/userinfo` | `openid profile email` |
| Kakao | `https://kauth.kakao.com/oauth/authorize` | `https://kauth.kakao.com/oauth/token` | `https://kapi.kakao.com/v2/user/me` | `account_email,profile_nickname` |
| Naver | `https://nid.naver.com/oauth2.0/authorize` | `https://nid.naver.com/oauth2.0/token` | `https://openapi.naver.com/v1/nid/me` | 별도 scope 없음 |
| Facebook | `https://www.facebook.com/{version}/dialog/oauth` | `https://graph.facebook.com/{version}/oauth/access_token` | `https://graph.facebook.com/{version}/me?fields=id,name,email` | `email,public_profile` |
| GitHub | `https://github.com/login/oauth/authorize` | `https://github.com/login/oauth/access_token` | `https://api.github.com/user`, `https://api.github.com/user/emails` | `read:user user:email` |

공식 문서에서 코드에 반영한 세부 차이:

- Google은 로그인 식별 목적에 맞게 OpenID Connect의 `openid profile email` scope와 `userinfo` endpoint를 사용합니다.
- Kakao는 추가 동의 scope 예시에 맞춰 scope를 comma-delimited 형식으로 전송합니다.
- Naver는 token 요청에도 callback에서 받은 `state`를 함께 전달합니다.
- Facebook은 Graph API 버전을 환경변수 `FACEBOOK_GRAPH_API_VERSION`으로 고정할 수 있게 했고, scope를 comma-delimited 형식으로 전송합니다.
- GitHub는 이메일이 비공개일 수 있어 `/user` 조회 후 email이 없으면 `/user/emails`를 추가 호출합니다.
- GitHub REST API 호출에는 `Accept: application/vnd.github+json`와 `X-GitHub-Api-Version` 헤더를 함께 전송합니다.

공식 문서 링크:

- [Google OAuth 2.0 Web Server Applications](https://developers.google.com/identity/protocols/oauth2/web-server)
- [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect)
- [Kakao Login REST API](https://developers.kakao.com/docs/latest/en/kakaologin/rest-api)
- [Naver Login API](https://developers.naver.com/docs/login/api/api.md)
- [Naver Profile API](https://developers.naver.com/docs/login/profile/profile.md)
- [Meta Graph API Versions](https://developers.facebook.com/docs/graph-api/changelog/versions)
- [GitHub OAuth Apps](https://docs.github.com/apps/building-oauth-apps/authorizing-oauth-apps)
- [GitHub Users API](https://docs.github.com/en/rest/users/users)
- [GitHub Emails API](https://docs.github.com/en/rest/users/emails)

## 환경 변수

프로젝트 루트의 `.env.example`을 참고해서 `.env` 파일을 만듭니다. 실제 secret이 들어가는 `.env`는 커밋하지 않습니다.

```properties
APP_BASE_URL=http://localhost:8080
APP_FRONTEND_BASE_URL=http://localhost:3000
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080

DB_URL=jdbc:mysql://localhost:3306/loginmodule?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=비밀번호

JWT_SECRET=최소_32바이트_이상의_시크릿
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...

KAKAO_CLIENT_ID=...
KAKAO_CLIENT_SECRET=

NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...

FACEBOOK_CLIENT_ID=...
FACEBOOK_CLIENT_SECRET=...
FACEBOOK_GRAPH_API_VERSION=v21.0

GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
GITHUB_API_VERSION=2026-03-10
```

Kakao는 `KAKAO_CLIENT_ID`에 JavaScript 키가 아니라 REST API 키를 넣어야 합니다. 이 프로젝트는 Kakao REST API 방식으로 로그인 URL과 token 요청을 처리합니다.

## Redirect URI

기본 redirect/callback URI는 `APP_FRONTEND_BASE_URL` 기준으로 아래처럼 생성됩니다.

| Provider | 개발자 콘솔에 등록할 Redirect/Callback URI |
| --- | --- |
| Google | `http://localhost:3000/oauth/callback/google` |
| Kakao | `http://localhost:3000/oauth/callback/kakao` |
| Naver | `http://localhost:3000/oauth/callback/naver` |
| Facebook | `http://localhost:3000/oauth/callback/facebook` |
| GitHub | `http://localhost:3000/oauth/callback/github` |

현재 백엔드가 실제로 생성하는 로그인 URL의 `redirect_uri`도 아래와 같습니다.

| Provider | 실제 요청에 들어가는 redirect_uri |
| --- | --- |
| Google | `http://localhost:3000/oauth/callback/google` |
| Kakao | `http://localhost:3000/oauth/callback/kakao` |
| Naver | `http://localhost:3000/oauth/callback/naver` |
| Facebook | `http://localhost:3000/oauth/callback/facebook` |
| GitHub | `http://localhost:3000/oauth/callback/github` |

따라서 redirect URI mismatch가 나면 백엔드 코드가 만든 값과 provider 개발자 콘솔에 등록된 값이 정확히 같은지 먼저 봐야 합니다.

### 개발자 콘솔 입력 예시

| Provider | 개발자 콘솔에서 넣을 값 |
| --- | --- |
| Google | Authorized JavaScript origins: `http://localhost:3000`, Authorized redirect URIs: `http://localhost:3000/oauth/callback/google` |
| Kakao | Web platform Site domain: `http://localhost:3000`, Kakao Login Redirect URI: `http://localhost:3000/oauth/callback/kakao` |
| Naver | 서비스 URL: `http://localhost:3000`, Callback URL: `http://localhost:3000/oauth/callback/naver` |
| Facebook | Valid OAuth Redirect URIs: `http://localhost:3000/oauth/callback/facebook` |
| GitHub | Homepage URL: `http://localhost:3000`, Authorization callback URL: `http://localhost:3000/oauth/callback/github` |

Naver는 스크린샷처럼 작성하면 맞습니다. 단, Callback URL 입력칸 오른쪽의 `+` 버튼을 눌러 목록에 추가한 뒤 저장해야 합니다. 입력칸에 글자만 남아 있고 추가/저장이 안 되어 있으면 Naver 입장에서는 등록되지 않은 Callback URL입니다.

### 실제 요청 redirect_uri 확인 명령

서버가 실제로 어떤 `redirect_uri`를 provider에 보내는지 확인하고 싶으면 PowerShell에서 아래 명령을 실행합니다.

```powershell
$providers = @('google','kakao','naver','facebook','github')
foreach ($p in $providers) {
  $body = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/oauth/$p/login-url" -Method GET
  $loginUrl = [uri]$body.loginUrl
  $query = [System.Web.HttpUtility]::ParseQueryString($loginUrl.Query)
  [pscustomobject]@{
    provider = $p
    redirect_uri = $query['redirect_uri']
    scope = $query['scope']
  }
}
```

출력된 `redirect_uri`를 개발자 콘솔에 등록된 값과 그대로 비교하면 됩니다.

자주 틀리는 부분:

- `localhost`와 `127.0.0.1`은 다른 주소입니다.
- `http`와 `https`는 다른 주소입니다.
- `8080` 포트가 빠지면 다른 주소입니다.
- 끝의 `/` 유무도 provider에 따라 다르게 볼 수 있습니다.
- 개발자 콘솔에서 값을 입력만 하고 추가/저장하지 않은 경우도 많습니다.
- `.env`를 수정했는데 서버를 재시작하지 않으면 이전 redirect URI로 계속 요청합니다.
- Google은 같은 프로젝트 안에서도 OAuth Client ID가 여러 개일 수 있습니다. `.env`의 `GOOGLE_CLIENT_ID`와 redirect URI를 등록한 OAuth Client가 같은지 확인해야 합니다.
- Kakao는 Redirect URI 등록 위치가 Kakao Login 설정 쪽인지 확인해야 합니다. REST API 키를 쓰는 앱과 `.env`의 `KAKAO_CLIENT_ID`가 같은 앱이어야 합니다.
- Naver는 애플리케이션 API 설정의 Callback URL과 요청의 `redirect_uri`가 같아야 합니다.
- Facebook은 Facebook Login 설정의 Valid OAuth Redirect URIs에 같은 URI가 들어 있어야 합니다.
- GitHub는 OAuth App의 Authorization callback URL과 요청의 `redirect_uri`가 같아야 합니다.

운영 배포 주소가 생기면 `APP_FRONTEND_BASE_URL`을 프론트엔드 운영 도메인으로 바꾸거나, provider별로 아래 환경변수를 직접 지정합니다.

```properties
GOOGLE_REDIRECT_URI=https://your-frontend-domain.com/oauth/callback/google
KAKAO_REDIRECT_URI=https://your-frontend-domain.com/oauth/callback/kakao
NAVER_REDIRECT_URI=https://your-frontend-domain.com/oauth/callback/naver
FACEBOOK_REDIRECT_URI=https://your-frontend-domain.com/oauth/callback/facebook
GITHUB_REDIRECT_URI=https://your-frontend-domain.com/oauth/callback/github
```

## 실행

Windows PowerShell 기준:

```powershell
.\gradlew.bat bootRun
```

서버 기본 주소:

```text
http://localhost:8080
```

## Swagger 접속

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## 소셜 로그인 페이지 접속

브라우저 주소창에 아래 URL을 입력하면 provider 로그인 페이지로 바로 이동합니다.

| Provider | 바로 접속 URL |
| --- | --- |
| Google | `http://localhost:8080/api/auth/oauth/google/authorize` |
| Kakao | `http://localhost:8080/api/auth/oauth/kakao/authorize` |
| Naver | `http://localhost:8080/api/auth/oauth/naver/authorize` |
| Facebook | `http://localhost:8080/api/auth/oauth/facebook/authorize` |
| GitHub | `http://localhost:8080/api/auth/oauth/github/authorize` |

프론트엔드에서 직접 redirect를 제어하려면 로그인 URL 조회 API를 먼저 호출합니다.

```text
GET http://localhost:8080/api/auth/oauth/{provider}/login-url
```

응답의 `loginUrl`로 브라우저를 이동시키면 됩니다.

```javascript
window.location.href = response.loginUrl;
```

## 프론트엔드에서 테스트하는 방법

React/Vue/Next 같은 프론트엔드 앱에서 쓰는 표준 방식입니다. provider callback 화면에는 `code`와 `state`가 존재할 수 있고, JWT는 프론트엔드가 `POST /login`에 `code`만 전달한 뒤 JSON 응답으로 받습니다. `state`는 프론트엔드 callback에서 요청 위조 방지용으로 검증하고 백엔드 request body에는 넣지 않습니다.

1. provider 개발자 콘솔에 프론트엔드 callback URI를 등록합니다.

```text
http://localhost:3000/oauth/callback/google
```

2. `.env`에도 같은 redirect URI를 넣습니다.

```properties
GOOGLE_REDIRECT_URI=http://localhost:3000/oauth/callback/google
```

3. 로그인 버튼에서 백엔드에 로그인 URL을 요청합니다.

```javascript
async function startGoogleLogin() {
  const response = await fetch("http://localhost:8080/api/auth/oauth/google/login-url");
  const body = await response.json();

  window.location.href = body.loginUrl;
}
```

4. 프론트엔드 callback 페이지에서 `code`, `state`를 꺼냅니다.

```javascript
const params = new URLSearchParams(window.location.search);
const code = params.get("code");
const state = params.get("state");
```

5. 프론트엔드에서 필요한 경우 `state`를 검증한 뒤, `code`만 백엔드로 보냅니다.

```javascript
async function finishGoogleLogin(code) {
  const response = await fetch("http://localhost:8080/api/auth/oauth/google/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ code })
  });

  const body = await response.json();

  localStorage.setItem("accessToken", body.accessToken);
  localStorage.setItem("refreshToken", body.refreshToken);
}
```

6. 이후 API 요청에는 Access Token을 붙입니다.

```javascript
await fetch("http://localhost:8080/api/some-protected-api", {
  headers: {
    Authorization: `Bearer ${localStorage.getItem("accessToken")}`
  }
});
```

프론트엔드 테스트에서 가장 많이 틀리는 부분은 provider 개발자 콘솔의 redirect URI와 `.env`의 `{PROVIDER}_REDIRECT_URI`가 서로 다른 경우입니다. 두 값은 반드시 같아야 합니다.

기존 백엔드 callback 주소인 `GET /api/auth/oauth/{provider}/callback`은 호환용 안전장치로만 남겨두었습니다. 이 endpoint는 JWT를 발급하지 않고 빈 응답을 반환합니다.

### token 교환 실패가 나는 경우

`POST /api/auth/oauth/{provider}/login`에서 token 교환 실패가 나면 대부분 아래 중 하나입니다.

- 인가코드를 이미 한 번 사용했습니다. OAuth authorization code는 일회용입니다.
- 인가코드를 받은 지 너무 오래 지나 만료되었습니다.
- 로그인 URL 생성 때 provider에 보낸 `redirect_uri`와 token 교환 때 백엔드가 보내는 `redirect_uri`가 다릅니다.
- `.env`의 `{PROVIDER}_CLIENT_ID`, `{PROVIDER}_CLIENT_SECRET`, `{PROVIDER}_REDIRECT_URI`와 provider 개발자 콘솔 설정이 다릅니다.
- `.env`를 수정한 뒤 서버를 재시작하지 않았습니다.

Facebook에서 특히 자주 나는 케이스는 `redirect_uri` 불일치입니다. `GET /api/auth/oauth/facebook/login-url` 응답의 `loginUrl` 안에 들어 있는 `redirect_uri`와 Facebook 개발자 콘솔의 Valid OAuth Redirect URIs, 그리고 실제 callback으로 받은 URL이 완전히 같아야 합니다.

## 파일별 역할

| 파일 | 역할 |
| --- | --- |
| `ModuleApplication.java` | Spring Boot 애플리케이션 시작점입니다. |
| `application.yml` | DB, JWT, OAuth provider endpoint, scope, redirect URI 기본값을 설정합니다. |
| `.env.example` | 실제 `.env`에 어떤 환경변수가 필요한지 보여주는 샘플입니다. |
| `OpenApiConfig.java` | Swagger/OpenAPI 제목, 서버 주소, JWT 보안 스키마를 설정합니다. |
| `SecurityConfig.java` | 공개 API와 인증 필요 API를 나누고, JWT 필터와 CORS를 등록합니다. |
| `JwtAuthenticationFilter.java` | `Authorization: Bearer {token}` 헤더를 읽어 Spring Security 인증 객체로 변환합니다. |
| `JwtTokenProvider.java` | 우리 서비스 JWT Access Token/Refresh Token 생성, 검증, claim 추출을 담당합니다. |
| `OAuthProvider.java` | 지원 provider 목록과 URL path 문자열을 enum으로 관리합니다. |
| `OAuthProperties.java` | `application.yml`의 `oauth.providers.*` 설정을 Java 객체로 바인딩합니다. |
| `OAuthClient.java` | 모든 provider client가 지켜야 하는 공통 인터페이스입니다. |
| `AbstractOAuthClient.java` | authorize URL 생성, token 교환, user-info 호출의 공통 로직을 담당합니다. |
| `GoogleOAuthClient.java` | Google 응답을 GoogleUserInfo로 변환하는 Google 전용 Client입니다. |
| `KakaoOAuthClient.java` | Kakao scope 구분자와 Kakao 응답 변환을 담당합니다. |
| `NaverOAuthClient.java` | Naver token 요청에 `state`를 추가하고 Naver 응답을 변환합니다. |
| `FacebookOAuthClient.java` | Facebook scope 구분자와 Graph API 응답 변환을 담당합니다. |
| `GithubOAuthClient.java` | GitHub `/user`, `/user/emails` 호출과 REST API 헤더를 담당합니다. |
| `OAuthClientFactory.java` | `OAuthProvider`에 맞는 `OAuthClient` 구현체를 찾아줍니다. |
| `OAuthUserInfo.java` | provider별 사용자 정보를 공통 형태로 다루기 위한 인터페이스입니다. |
| `OAuthAttributes.java` | provider 응답 Map에서 문자열/중첩 Map을 안전하게 꺼내는 유틸입니다. |
| `GoogleUserInfo.java` | Google `sub`, `email`, `name`을 공통 사용자 정보로 변환합니다. |
| `KakaoUserInfo.java` | Kakao `id`, `kakao_account.email`, `profile.nickname`을 변환합니다. |
| `NaverUserInfo.java` | Naver `response.id`, `response.email`, `response.name`을 변환합니다. |
| `FacebookUserInfo.java` | Facebook `id`, `email`, `name`을 변환합니다. |
| `GithubUserInfo.java` | GitHub `id`, `email`, `name/login`을 변환합니다. |
| `OAuthService.java` | 로그인 URL 생성, code 교환, 사용자 조회/가입, JWT 발급을 연결하는 핵심 비즈니스 로직입니다. |
| `OAuthController.java` | `/authorize`, `/login-url`, `/login`, legacy `/callback` HTTP API를 제공합니다. |
| `OAuthLoginRequest.java` | 프론트엔드 callback 페이지가 전달하는 `code`를 담는 DTO입니다. |
| `OAuthLoginUrlResponse.java` | provider 로그인 URL과 state를 내려주는 DTO입니다. |
| `TokenResponse.java` | 로그인 성공 후 JWT 토큰 응답을 담는 DTO입니다. |
| `User.java` | 우리 서비스 회원 Entity입니다. email/password/name/provider/providerId/role을 저장합니다. |
| `UserRole.java` | 서비스 내부 권한 enum입니다. |
| `UserRepository.java` | provider/providerId, email 기준 User 조회를 담당합니다. |
| `RefreshToken.java` | Refresh Token DB 저장 Entity입니다. |
| `RefreshTokenRepository.java` | Refresh Token 조회와 사용자별 기존 토큰 삭제를 담당합니다. |
| `GlobalEntity.java` | 생성/수정/삭제 시각 필드를 공통으로 제공합니다. |
| `GlobalEntityListener.java` | Entity 저장/수정 전에 시간 필드를 자동 세팅합니다. |
| `ErrorCode.java` | 표준 에러 코드, HTTP 상태, 메시지를 enum으로 관리합니다. |
| `OAuthLoginException.java` | OAuth 로그인 과정의 도메인 예외입니다. |
| `InvalidOAuthProviderException.java` | 지원하지 않는 provider path 요청에 대한 예외입니다. |
| `ErrorResponse.java` | 예외 응답의 `status`, `code`, `message`를 담습니다. |
| `GlobalExceptionHandler.java` | 예외를 표준 실패 응답으로 변환합니다. |
| `RsData.java` | 공통 응답 wrapper가 필요한 API에서 재사용할 수 있는 DTO입니다. |

## 처음부터 작성한다면 좋은 순서

1. `OAuthProvider`를 먼저 작성합니다.  
   지원 provider를 enum으로 고정하면 이후 코드가 문자열 비교에 덜 흔들립니다.

2. `application.yml`과 `.env.example`을 작성합니다.  
   공식 문서의 endpoint, client id/secret, redirect URI, scope를 설정으로 분리합니다.

3. `OAuthProperties`를 작성합니다.  
   설정을 Java 객체로 읽어오면 Client 코드가 yml 구조를 직접 알 필요가 없습니다.

4. `OAuthUserInfo`와 provider별 `*UserInfo`를 작성합니다.  
   provider 응답 구조가 다르기 때문에 먼저 공통 사용자 모델을 정해야 Service가 단순해집니다.

5. `OAuthClient` 인터페이스를 작성합니다.  
   로그인 URL 생성, access token 교환, user-info 조회라는 공통 계약을 정합니다.

6. `AbstractOAuthClient`를 작성합니다.  
   provider 대부분이 공유하는 OAuth2 Authorization Code Flow 로직을 한 곳에 둡니다.

7. provider별 `*OAuthClient`를 작성합니다.  
   Kakao/Facebook scope delimiter, Naver state, GitHub email 보정처럼 provider별 차이만 여기에 둡니다.

8. `OAuthClientFactory`를 작성합니다.  
   Service에서 `if provider == google` 같은 분기를 없애기 위해 provider별 Client를 Map으로 관리합니다.

9. `User`, `RefreshToken`, Repository를 작성합니다.  
   provider 사용자 정보를 우리 서비스 회원과 토큰 저장 구조로 매핑합니다.

10. `JwtTokenProvider`를 작성합니다.  
    소셜 provider access token이 아니라 우리 서비스 자체 JWT를 발급하는 책임입니다.

11. `OAuthService`를 작성합니다.  
    URL 생성, code 교환, user-info 조회, 가입/로그인, JWT 발급을 하나의 유스케이스로 연결합니다.

12. `OAuthController`를 작성합니다.  
    HTTP API는 최대한 얇게 두고, 실제 로직은 Service에 위임합니다.

13. `SecurityConfig`와 `JwtAuthenticationFilter`를 작성합니다.  
    OAuth 시작/login/legacy callback/Swagger는 공개하고, 나머지 API는 JWT 인증을 요구하게 만듭니다.

14. `GlobalExceptionHandler`, `ErrorCode`, `ErrorResponse`를 작성합니다.  
    실패 응답 형식을 통일해 프론트엔드 처리가 쉬워지게 합니다.

15. Swagger 어노테이션과 README를 작성합니다.  
    다른 사람이 API를 실행해보고 코드를 공부할 수 있게 문서화합니다.

## provider 추가 체크리스트

새 provider를 추가할 때는 아래 순서대로 진행합니다.

1. `OAuthProvider`에 enum 값을 추가합니다.
2. `application.yml`에 `oauth.providers.{provider}` 설정을 추가합니다.
3. `{Provider}UserInfo`를 만들어 provider 응답을 `OAuthUserInfo`로 변환합니다.
4. `{Provider}OAuthClient`를 만들고 `@Component`로 등록합니다.
5. provider 개발자 콘솔에 redirect URI를 등록합니다.
6. Swagger/README에 provider 접속 URL을 추가합니다.
7. `compileJava`, `test`를 실행합니다.

## 테스트 / 결과 확인

### 1. 컴파일 + 자동 테스트

Windows PowerShell 기준입니다.

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
```

현재 자동 테스트는 `src/test/java/com/socialogin/module/ModuleApplicationTests.java`의 `contextLoads()` 한 개입니다. Spring 컨텍스트가 정상적으로 떠야 통과하므로, Bean 설정·`application.yml` 바인딩·의존성 주입이 깨지면 이 테스트에서 바로 실패합니다.

테스트 결과는 두 가지로 확인합니다.

- **콘솔 요약**: 명령 실행 직후 `BUILD SUCCESSFUL` 또는 `BUILD FAILED`와 실패한 테스트 이름이 출력됩니다.
- **HTML 리포트**: 아래 파일을 브라우저로 열면 테스트별 성공/실패와 로그를 볼 수 있습니다.

```text
build/reports/tests/test/index.html
```

```powershell
# 리포트를 바로 브라우저로 열기
Start-Process .\build\reports\tests\test\index.html
```

> `contextLoads` 테스트도 실제 Spring 컨텍스트를 띄우므로, `.env`(또는 환경변수)에 DB 접속 정보와 OAuth 설정 값이 채워져 있어야 합니다. 값이 없으면 컨텍스트 로딩 단계에서 실패할 수 있습니다.

### 2. 실제 로그인 동작(end-to-end) 확인

provider 연동까지 직접 확인하려면 서버를 띄운 뒤 아래 순서로 봅니다.

1. 서버 실행: `.\gradlew.bat bootRun`
2. Swagger(`http://localhost:8080/swagger-ui.html`)에서 `GET /api/auth/oauth/{provider}/login-url` 호출 → 응답 `loginUrl`을 브라우저에 붙여넣어 provider 로그인
3. provider가 돌려준 callback URL에서 `code` 추출
4. Swagger의 `POST /api/auth/oauth/{provider}/login`에 `{ "code": "..." }` 전송
5. 응답으로 `accessToken`, `refreshToken`이 내려오면 로그인 성공입니다.

실제로 서버가 provider에 보내는 `redirect_uri`를 확인하려면 위 [실제 요청 redirect_uri 확인 명령](#실제-요청-redirect_uri-확인-명령)의 PowerShell 스크립트를 사용합니다. 로그인 실패 시 응답 JSON의 `error.code`(예: `INVALID_AUTHORIZATION_CODE`, `DUPLICATED_EMAIL_WITH_DIFFERENT_PROVIDER`)로 원인을 좁힐 수 있습니다.
