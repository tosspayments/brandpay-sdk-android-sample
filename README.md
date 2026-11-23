# 브랜드페이 Android 인증 SDK 사용가이드

#### 버전관리 변경사항 (2026년 11월 17일)
- 생체인증 SDK 를 0.3.1 로 올립니다. SDK 가 로그에 남기는 웹 연동 값을 줄였습니다. 연동 방법은 바뀌지 않습니다.

#### 버전관리 변경사항 (2026년 6월 9일)
- ⚠️ OCR SDK 연동은 deprecated 되었습니다. Web OCR 사용을 권장합니다.

#### 버전관리 변경사항 (2025년 6월 20일)
- 생체인증 과정에서 내부 에러가 발생한 경우를 콜백으로 받아볼 수 있는 기능을 추가합니다.
- 신규 콜백 연동은 아래 가이드를 참조하세요.

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

## 브랜드페이 생체인증 연동 가이드 ver250620

### 라이브러리 추가

- libs/auth-0.3.1.aar 추가
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
            override fun onErrorOccurred(exception: BrandpayBiometricAuthException) {
                super.onErrorOccurred(exception)
                Log.d("onErrorOccurred", exception.message.toString())
                Log.d("onErrorOccurred", exception.errorCode.toString())
                Log.d("onErrorOccurred", exception.cause?.message.toString())
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

            // 원격 디버깅은 debuggable 빌드에서만 켭니다.
            if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            brandPayAuthWebManager.addJavascriptInterface(this)
        }

        webView.loadUrl(WEB_URL)
    }
}
```

### 필수: 브리지를 붙인 WebView 에 여는 주소 확인

브리지를 붙인 WebView 에는 가맹점이 정한 결제 페이지 주소(위 예시의 `WEB_URL`)만 엽니다. 딥링크, 푸시, Intent extra 처럼 외부에서 받은 URL 은 scheme 이 `https` 이고 host 가 허용 목록에 있는지 확인한 뒤 엽니다.

```kotlin
// 가맹점 결제 페이지의 host 만 둡니다.
private val allowedHosts = setOf("shop.example.com")

private fun isAllowedUrl(url: String): Boolean {
    // 백슬래시가 든 주소는 Android 버전에 따라 Uri 와 WebView 가 host 를 다르게 읽을 수 있어 거부합니다.
    if ('\\' in url) return false
    val uri = Uri.parse(url)
    return uri.scheme?.lowercase() == "https" && uri.host?.lowercase() in allowedHosts
}

// 외부에서 받은 URL 은 확인을 통과했을 때만 엽니다.
if (isAllowedUrl(receivedUrl)) {
    webView.loadUrl(receivedUrl)
}
```

이 확인은 WebView 에 처음 여는 주소에만 적용됩니다. 페이지가 열린 뒤 이동한 페이지가 브리지를 부를 수 있는지는 SDK 0.4.0 부터 허용 출처로 제한합니다.

### 백업에서 생체 인증 저장소 빼기

SDK 는 생체 인증 등록 정보를 SharedPreferences 파일 `tossBrandPayUtilPreference.xml` 에 둡니다. 이 파일은 기기의 Android Keystore 키로 암호화되어 다른 기기에서 풀 수 없으므로 백업에서 뺍니다. 복원한 기기에서는 생체 재등록이 필요할 수 있습니다.

`android:allowBackup` 을 끄지 않고 이 파일만 빼면 앱의 다른 데이터는 그대로 백업됩니다. Android 11(API 30) 이하는 `android:fullBackupContent` 를, Android 12(API 31) 이상은 `android:dataExtractionRules` 를 읽으니 두 규칙에 모두 적습니다. `dataExtractionRules` 를 지정하면 targetSdk 가 31 미만이어도 Android 12 이상 기기에서는 이 규칙을 따릅니다.

```xml
<!-- AndroidManifest.xml -->
<application
    android:fullBackupContent="@xml/backup_rules"
    android:dataExtractionRules="@xml/data_extraction_rules">
</application>

<!-- res/xml/backup_rules.xml (Android 11 이하) -->
<full-backup-content>
    <exclude domain="sharedpref" path="tossBrandPayUtilPreference.xml" />
</full-backup-content>

<!-- res/xml/data_extraction_rules.xml (Android 12 이상) -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="tossBrandPayUtilPreference.xml" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="tossBrandPayUtilPreference.xml" />
    </device-transfer>
</data-extraction-rules>
```

### onErrorOccurred 에서 받을 수 있는 에러 클래스

```kotlin
/**
 * 브랜드페이 생체인증 SDK 내부에서 발생한 에러입니다.
 *
 * @param errorCode 에러 종류
 * @param traceId 에러 추적을 위한 고유값
 * @param message 에러 메시지
 * @param cause 원본 예외
 */
class BrandpayBiometricAuthException(
    val errorCode: AuthErrorCode,
    val traceId: String? = null,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    enum class AuthErrorCode {
        INVALID_CONTEXT,
        SECURE_STORAGE_INIT_FAILED,
        SECURE_STORAGE_IO_FAILED,
        CRYPTO_OPERATION_FAILED,
        BIOMETRIC_API_FAILED,
        CALLBACK_NOT_REGISTERED
    }
}
```

## ⚠️ [Deprecated] OCR 연동 가이드 ver230309

> ### ⚠️ Deprecated
> **이 SDK 기반 OCR 연동은 더 이상 권장되지 않습니다.**  
> Web OCR 도입으로 신규 연동 시에는 Web OCR을 사용해주세요.  
> 관련 문의는 [디스코드](https://developers.tosspayments.com/go/techchat)로 부탁드립니다.

### OCR 라이선스 파일

OCR 라이선스 파일(`.flk`)은 이 저장소에 없습니다. 라이선스는 가맹점마다 발급받으니 담당 매니저나 [토스페이먼츠 개발자 커뮤니티](https://developers.tosspayments.com/go/techchat)로 문의합니다.

- 받은 파일은 `ui/src/main/assets/fincube_license.flk` 로 넣습니다. 라이선스 파일은 저장소에 올리지 않으며, 이 샘플의 `.gitignore` 는 `*.flk` 를 뺍니다.
- 파일이 없으면 샘플의 OCR 카드 스캔은 동작하지 않습니다. 생체 인증은 그대로 동작합니다.

### OCR SDK 에서 Web OCR 로 옮기기

OCR SDK 는 더 이상 권장하지 않습니다. 새로 연동한다면 Web OCR 을 씁니다. 결제 화면의 카드 등록에서 아래가 모두 맞을 때 Web OCR 이 뜹니다.

- 앱이 OCR 브리지(`window.ConnectPayOcr`)를 붙이지 않았습니다. 브리지가 있으면 결제 화면은 앱 카드 스캔을 먼저 씁니다.
- 가맹점 페이지가 JS SDK 를 띄울 때 `features.cardOCR.active` 를 `true` 로 넘겼습니다.

Web OCR 은 라이선스 계약이 필요한 기능이라 가맹점이 직접 켤 수 없습니다. 담당 매니저나 [토스페이먼츠 개발자 커뮤니티](https://developers.tosspayments.com/go/techchat)로 문의합니다.

<details>
<summary>기존 연동 가이드 보기 (유지보수 목적)</summary>

#### 버전 변경사항 (2025년 9월 26일)
- 구버전 라이센스 키 호환 이슈 수정

#### 버전 변경사항 (2025년 9월 23일)
- 16KB 페이지 크기를 지원하도록 수정 

### 라이브러리 추가

- libs/ocr-0.3.1.aar 추가 
- OCR 관련 라이브러리 [OcrEngine] 추가
    - settings.gradle

        ```groovy
        include ':OcrEngine'
        ```

- assets 폴더에 OCR 라이선스 파일(fincube_license.flk) 추가. 파일은 저장소에 없으니 [OCR 라이선스 파일](#ocr-라이선스-파일) 을 봅니다.

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

</details>