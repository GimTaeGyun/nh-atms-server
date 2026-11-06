
</br>

# 소스 형상 및 브랜치 관리
> 분석일: 2026-05-19
> 
> 대상: NH-ATMS


---

</br>

# Frontend 서버 빌드

</br>

0. `PS C:\Users\User\Desktop\NH-ATMS\Frontend> cd .\nh-atms-web\`
	- 먼저 Frontend > nh-atms-web 디렉토리로 이동

1. `npm install`
```bash
PS C:\Users\User\Desktop\NH-ATMS\Frontend> cd .\nh-atms-web\
PS C:\Users\User\Desktop\NH-ATMS\Frontend\nh-atms-web> npm install
npm WARN EBADENGINE Unsupported engine {
...
npm WARN EBADENGINE   current: { node: 'v18.17.0', npm: '9.6.7' }
npm notice This endpoint is being retired. Use the bulk advisory endpoint instead. See the following docs for more info: https://api-docs.npmjs.com/#tag/Audit

added 283 packages in 2m

56 packages are looking for funding
  run `npm fund` for details
```

2. `npm run dev` (개발 서버 실행)

> [!danger] 다음과 같은 오류 발생시
> 
> You are using Node.js 18.17.0. Vite requires Node.js version 20.19+ or 22.12+. Please upgrade your Node.js version.
> 
> - Node.js 버전 업 필요

로컬 서버 구동 완료

![Pasted image 20260519175715.png](file/Pasted%20image%2020260519175715.png)

![Pasted image 20260519175811.png](file/Pasted%20image%2020260519175811.png)

</br>

---

</br>

# Backend 서버 빌드

</br>

1. Gradle 빌드
 
![Pasted image 20260519180215.png](file/Pasted%20image%2020260519180215.png)

</br>

2. AtmsWebApplication 실행

![Pasted image 20260519180342.png](file/Pasted%20image%2020260519180342.png)

![Pasted image 20260519180424.png](file/Pasted%20image%2020260519180424.png)