# 브랜드페이 Android 인증 SDK 사용가이드

#### 버전관리 변경사항 (2024년 11월 27일)
- 일부 생체인증 오류 상황에서 무한 로딩이 지속되는 문제를 수정하였습니다.
- 생체인증 에러 발생 시 표시되는 에러 문구를 명확하게 수정하였습니다.

#### 버전관리 변경사항 (2024년 11월 20일)
- 브랜드페이 생체인증 등록 실패시, 무한 로딩이 발생하는 오류를 해결하였습니다.
- 관련 이슈(무한로딩)가 발생하거나, 오류 토스트가 발생할 경우 문의 바랍니다.

#### 버전관리 변경사항 (2024년 2월 5일)
- 브랜드페이 생체인증 연동 시 생성자에 넣은 activity의 상태가 올바르지 않을 경우 무한 로딩이 발생하는 오류를 해결하였습니다.
- 관련 이슈(무한로딩)가 발생하거나, 오류 토스트가 발생할 경우 문의 바랍니다.

#### 버전관리 변경사항 (2023년 3월 9일)
- 서비스 명칭이 ConnectPay에서 BrandPay로 변경됨에 따라 관련된 클래스명들도 BrandPay로 변경되었습니다.
  - BrandPayAuthWebManager, BrandPayOcrWebManager의 javascript interface 연동 방식이 아래와 같이 변경되었습니다.
    ```
    brandPayOcrWebManager.addJavascriptInterface(WEB_VIEW)
    brandPayAuthWebManager.addJavascriptInterface(WEB_VIEW)
    ```

## OCR 연동 가이드 ver230309

### 라이브러리 추가

- libs/ocr-0.2.0.aar 추가
- OCR 관련 라이브러리 [OcrEngine, ocrview] 추가
    - settings.gradle

        ```groovy
        include ':OcrEngine'
        include ':ocrview'
        ```

- asserts폴더에 OCR 라이선스 파일(fincube_license.flk) 추가

- build.config(:app)

    ```groovy
    dependencies {
        implementation fileTree(dir: 'libs', include: '*.aar')
    
        implementation project(':OcrEngine')
    
        // 2.2.0 이상 권장
        implementation "androidx.lifecycle:lifecycle-runtime-ktx:{version}"
	
        implementation "androidx.appcompat:appcompat:{version}"
        implementation "androidx.core:core-ktx:{version}"
    }
    ```

### Web ↔ App간 Message 처리를 위한 JavaScriptInterface 설정

```kotlin
class BrandPayAuthSampleWebActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    private val brandPayOcrWebManager = BrandPayOcrWebManager(this).apply {
        callback = object : BrandPayOcrWebManager.Callback {
            override fun onPostScript(script: String) {
                webView.loadUrl(script)
            }
        }
    }

    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    private fun initViews() {
        webView = findViewById<WebView>(R.id.web_view).apply {
            settings.run {
                javaScriptEnabled = true
                domStorageEnabled = true
            }

            brandPayOcrWebManager.addJavascriptInterface(this)
            )
        }

        webView.loadUrl(WEB_URL)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 유효한 Result인 경우 Callback.onPost로 리턴
        BrandPayOcrWebManager.handleActivityResult(requestCode, resultCode, data)

        /**
         * 직접 Handling할 경우
        if (requestCode == BrandPayOcrWebManager.REQUEST_CODE_CARD_SCAN) {
        data?.getStringExtra(BrandPayOcrWebManager.EXTRA_CARD_SCAN_RESULT_SCRIPT)?.let { resultScript ->
        webView.loadUrl(resultScript)
        }
        }
         **/
    }
}
```

### Web에서 Message 호출하기

- `window.ConnectPayOcr.postMessage` 호출

### OCR 스캔 결과 핸들링

- onActivityResult에서 handleActivityResult 호출

### OCR 스캐너 실행가능 여부

- 호출 Message

    ```
	{
	  name: 'isOCRAvailable',
	  params: {
	    onSuccess: 성공시 호출할 메서드명,
	    password : 등록할 비밀번호
	  }
	}
    ```

- Return
    - onSuccess : Boolean
        - true : 실행 가능
        - false : 실행 불가능

### OCR 스캐너 실행

- 호출 Message

    ```
	{
	  name: 'ocrScan',
	  params: {
	    onSuccess: 성공시 호출할 메서드명,
	    onError: 에러 발생시 호출할 메서드명,
	    license : 포지큐브에서 발급받은 OCR License
	  }
	}
    ```

- Return
    - onSuccess : Json String

    ```
	{
	  cardNo1 : 카드번호 첫번 째 4자리,
	  cardNo2 : 카드번호 두번 째 4자리,
	  cardNo3 : 카드번호 세번 째 4자리,
	  cardNo4 : 앞 12자리를 제외한 카드번호,
	  expiryDate : 카드 유효기간 (MMYY)
	}
    ```

    - onError : Error 메세지

## 브랜드페이 생체인증 연동 가이드 ver230309

### 라이브러리 추가

- libs/auth-0.2.1.aar 추가
- build.config(:app)

    ```groovy
    dependencies {
        implementation fileTree(dir: 'libs', include: '*.aar')
	
        // 2.2.0 이상 권장
        implementation "androidx.lifecycle:lifecycle-runtime-ktx:{version}"
    
        // 2.6.0 이상 권장
        implementation 'com.google.code.gson:gson:{version}'
	
        // 1.1.0 이상 권장
        implementation "androidx.biometric:biometric:{version}"
	
        implementation "androidx.security:security-crypto:1.1.0-alpha03"
	
        implementation "androidx.appcompat:appcompat:{version}"
        implementation "androidx.core:core-ktx:{version}"
    }
    ```

### Web ↔ App간 Message 처리를 위한 JavaScriptInterface bind

```kotlin
class BrandPayAuthSampleWebActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    private val brandPayAuthWebManager = BrandPayAuthWebManager(this).apply {
        callback = object : BrandPayAuthWebManager.Callback {
            override fun onPostScript(script: String) {
                webView.loadUrl(script)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brandpay_auth_web_sample)

        initViews()
    }

    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    private fun initViews() {
        webView = findViewById<WebView>(R.id.web_view).apply {
            settings.run {
                javaScriptEnabled = true
                domStorageEnabled = true
            }

            brandPayAuthWebManager.addJavascriptInterface(this)
        }

        webView.loadUrl(WEB_URL)
    }
}
```

### Web에서 Message 호출하기

- `window.ConnectPayAuth.postMessage` 호출

### 지원 생체인증수단 조회

- 호출 Message

    ```
    {
      name: 'getBiometricAuthMethods',
      params: {
        onSuccess: 성공시 호출할 메서드명,
        onError: 에러 발생시 호출할 메서드명
      }
    }
    ```

- Return
    - onSuccess : String 배열
        - 지문 [FINGERPRINT]
        - 얼굴인식 [FACE]
        - 모두 지원 [FINGERPRINT, FACE]
        - 모두 지원하지 않는 경우는 빈 배열
    - onError : Error 메세지

### 생체인증 비밀번호 저장 여부 조회

- 호출 Message

    ```
    {
      name: 'hasBiometricAuth',
      params: {
        onSuccess: 성공시 호출할 메서드명
      }
    }
    ```

- Return
    - onSuccess : Boolean
        - true : 설정한 비밀번호 존재
        - false : 설정한 비밀번호 부재

### 생체인증 비밀번호 등록

- 호출 Message

    ```
    {
      name: 'registerBiometricAuth',
      params: {
        onSuccess: 성공시 호출할 메서드명,
        onError: 에러 발생시 호출할 메서드명,
        password : 등록할 비밀번호
      }
    }
    ```

- Return
    - onSuccess : Boolean
        - true : 등록 완료
        - false : 등록 실패
    - onError : Error 메세지

### 생체인증 요청

- 호출 Message

    ```
    {
      name: 'verifyBiometricAuth',
      params: {
        onSuccess: 성공시 호출할 메서드명,
        onError: 에러 발생시 호출할 메서드명
      }
    }
    ```

- Return
    - onSuccess : 기존에 등록된 비밀번호
    - onError : Error 메세지
