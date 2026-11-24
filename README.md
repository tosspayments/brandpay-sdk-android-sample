# 브랜드페이 Android 인증 SDK 사용가이드

#### 버전관리 변경사항 (2026년 11월 24일)
- 생체인증 SDK 와 OCR SDK 를 0.4.0 으로 올립니다. `androidx.biometric:biometric` 1.1.0, `androidx.webkit:webkit` 1.4.0 이 필수입니다. 아래 "필수 의존성 최소 버전" 을 보세요.
- 브리지를 부를 수 있는 페이지 출처를 정하는 `addJavascriptInterface(webView, allowedOriginRules)` 를 추가합니다. 옛 `addJavascriptInterface(webView)` 는 deprecated 되었고 기본 출처 검사가 들어갔습니다. 아래 "허용 출처 정하기" 를 보세요.
- 생체 인증은 강한 생체(지문 등)만 받습니다. R8 을 켠 앱도 SDK 용 keep 규칙을 따로 넣지 않습니다.

#### 버전관리 변경사항 (2026년 11월 17일)
- 생체인증 SDK 를 0.3.1 로 올립니다. SDK 가 로그에 남기는 웹 연동 값을 줄였습니다. 연동 방법은 바뀌지 않습니다.

#### 버전관리 변경사항 (2026년 6월 9일)
- ⚠️ OCR SDK 연동은 deprecated 되었습니다. Web OCR 사용을 권장합니다. 옮기는 방법은 아래 "OCR SDK 에서 Web OCR 로 옮기기" 를 보세요.

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

- libs/auth-0.4.0.aar 추가
- build.gradle(:app) 에 아래 의존성을 그대로 추가합니다. aar 은 의존성을 함께 가져오지 않으므로 앱에 직접 적어야 합니다.

    ```groovy
    dependencies {
        implementation fileTree(dir: 'libs', include: '*.aar')

        // 필수: 생체 인증
        implementation "androidx.biometric:biometric:1.1.0"

        // 필수: 브리지 출처 검사(옛 메서드의 기본 출처 검사, 새 메서드의 허용 출처)
        implementation "androidx.webkit:webkit:1.4.0"

        implementation "androidx.security:security-crypto:1.1.0-alpha03"
        implementation "androidx.appcompat:appcompat:1.4.0"
        implementation "androidx.core:core-ktx:1.2.0"

        // 2.2.0 이상 권장
        implementation "androidx.lifecycle:lifecycle-runtime-ktx:{version}"

        // 2.6.0 이상 권장
        implementation 'com.google.code.gson:gson:{version}'
    }
    ```

### 필수 의존성 최소 버전

표의 버전보다 낮으면 해당 기능이 동작하지 않습니다. 더 높은 버전은 그대로 써도 됩니다.

| 라이브러리 | 최소 버전 | 필요한 기능 |
|---|---|---|
| `androidx.biometric:biometric` | 1.1.0 | 생체 인증 등록과 인증. 0.4.0 부터 필수입니다. |
| `androidx.webkit:webkit` | 1.4.0 | 브리지 출처 검사: 옛 `addJavascriptInterface(webView)` 의 기본 출처 검사와 `addJavascriptInterface(webView, allowedOriginRules)` 의 출처 제한 (생체 인증, 카드 스캔) |
| `androidx.security:security-crypto` | 1.1.0-alpha03 | 생체 인증 정보 저장 |
| `androidx.appcompat:appcompat` | 1.4.0 | 생체 인증, 카드 스캔 화면 |
| `androidx.core:core-ktx` | 1.2.0 | 생체 인증, 카드 스캔 |

0.4.0 부터 생체 인증은 강한 생체(지문 등)만 받습니다. 얼굴 인식만 등록한 기기(약한 생체)는 생체 결제를 쓸 수 없고, 지문을 등록하거나 비밀번호로 결제합니다. 지문 센서 없이 얼굴 인식만 있는 일부 태블릿이 여기에 해당합니다.

### R8(minify) 을 켠 앱

앱에서 R8(`minifyEnabled true`)을 켜도 SDK 용 keep 규칙을 따로 넣지 않습니다. 0.4.0 aar 에 든 규칙이 브리지 메서드, 웹에 보내는 응답 필드, 카드 스캔 엔진(`io.fincube`) 클래스를 지킵니다.

0.3.1 이하를 R8 로 빌드하면 카드 스캔 화면을 열 때 앱이 죽습니다. R8 이 엔진 클래스 이름을 바꿔 native 코드가 찾지 못하기 때문입니다. 0.3.1 을 계속 쓰면 앱 `proguard-rules.pro` 에 아래 keep 을 넣습니다. 마지막 줄은 0.3.1 을 R8 로 빌드할 때 웹 오류 응답이 `{}` 로 나가는 것도 막습니다.

```
-keep class io.fincube.** { *; }
-keepclasseswithmembernames class io.fincube.** {
    native <methods>;
}
-keepclassmembers class com.tosspayments.android.** {
    @com.google.gson.annotations.SerializedName <fields>;
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

            // 결제 SDK 를 띄우는 가맹점 페이지 출처를 넣습니다. 아래 "허용 출처 정하기" 를 참고하세요.
            brandPayAuthWebManager.addJavascriptInterface(this, setOf("https://shop.example.com"))
        }

        webView.loadUrl(WEB_URL)
    }
}
```

### 허용 출처 정하기

`addJavascriptInterface(webView, allowedOriginRules)` 는 0.4.0 부터 쓸 수 있습니다. `allowedOriginRules` 에 맞는 출처의 페이지만 `window.ConnectPayAuth`, `window.ConnectPayOcr` 를 부를 수 있습니다.

```kotlin
// 생체 인증
brandPayAuthWebManager.addJavascriptInterface(webView, setOf("https://shop.example.com"))

// 카드 스캔
brandPayOcrWebManager.addJavascriptInterface(webView, setOf("https://shop.example.com"))

webView.loadUrl("https://shop.example.com/order")
```

- WebView 를 만든 UI 스레드에서, 페이지를 불러오기 전에 부릅니다.
- 결제 SDK 를 띄우는 가맹점 페이지 출처를 넣습니다. 브랜드페이 도메인은 넣지 않습니다.
- 결제 SDK 를 iframe 에서 띄우는 구성은 그 iframe 이 최상위 페이지와 같은 출처일 때만 동작합니다. 웹 SDK 가 응답을 최상위 문서에서 첫 iframe 으로 넘기기 때문입니다. 같은 출처라 최상위 페이지 출처만 넣으면 됩니다.
- 규칙은 `https://host` 또는 `https://host:port` 형식입니다. host 앞에 `*.` 를 붙이면 하위 도메인만 허용합니다(`https://*.example.com` 은 `example.com` 을 포함하지 않습니다).
- https 가 아닌 규칙이나 빈 집합을 넘기면 `IllegalArgumentException` 이 발생합니다.
- 허용 출처가 빠지면 오류 없이 생체 인증과 카드 스캔이 보이지 않습니다. 붙인 뒤 실제 결제 페이지에서 두 기능이 뜨는지 확인하세요.
- 웹에서 부르는 방식과 `Callback.onPostScript` 로 받는 응답은 바뀌지 않습니다.

> ⚠️ 옛 `addJavascriptInterface(webView)` 는 deprecated 되었고, 0.4.0 부터 기본 출처 검사가 들어갔습니다. 허용 출처를 정하려면 `addJavascriptInterface(webView, allowedOriginRules)` 로 바꿔 주세요.

옛 메서드의 기본 출처 검사는 아래와 같습니다.

- 브리지는 그때 떠 있는 최상위 페이지와 같은 출처의 프레임만 부를 수 있습니다. 최상위 페이지(메인 프레임)는 출처와 상관없이 부를 수 있습니다.
- 출처가 다른 iframe 이나 불투명 출처(sandbox iframe, data:, file:)의 iframe 이 부르면 버리고 logcat 에 안내를 남깁니다.
- 최상위 페이지가 https 가 아니어도 동작하지만 logcat 에 경고를 남깁니다. https 만 받으려면 새 메서드로 허용 출처를 정합니다.
- androidx.webkit 이 없거나 WebView 가 지원하지 않으면 예전처럼 출처를 확인하지 않고 붙이고, logcat 에 안내를 남깁니다.
- 같은 WebView 에 옛 메서드와 새 메서드를 함께 부르거나 같은 메서드를 다시 부르면 마지막 호출이 이깁니다.

이 샘플은 결제 페이지 주소를 `https://merchant.example.com/brandpay`, 허용 출처를 `https://merchant.example.com` 자리 표시로 둡니다. 자기 결제 페이지 주소와 출처로 바꿔 씁니다(`MainActivity` 의 `PAYMENT_PAGE_URL`, `WebActivity` 의 `ALLOWED_ORIGINS` 와 기본 주소). debug 빌드의 URL 입력칸으로 다른 출처의 페이지를 열면 브리지가 응답하지 않습니다.

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

이 확인은 WebView 에 처음 여는 주소에만 적용됩니다. 페이지가 열린 뒤 이동한 페이지가 브리지를 부를 수 있는지는 SDK 0.4.0 부터 [허용 출처](#허용-출처-정하기)로 제한합니다.

### 증상과 해결

logcat 에서 태그 `BrandPayAuth`, `BrandPayOcr` 를 확인합니다.

```
adb logcat -s BrandPayAuth BrandPayOcr
```

| 증상 | logcat 태그 | logcat 문구 | 해결 |
|---|---|---|---|
| 생체 결제 수단이 보이지 않거나, 등록이나 인증이 오류로 끝남 | `BrandPayAuth` | `androidx.biometric:biometric 1.1.0 이상이 필요합니다. 앱 build.gradle 에 implementation "androidx.biometric:biometric:1.1.0" 을 추가하세요.` | `implementation "androidx.biometric:biometric:1.1.0"` 추가 |
| `addJavascriptInterface(webView, allowedOriginRules)` 를 썼는데 webkit 이 없음 | `BrandPayAuth`, `BrandPayOcr` | `androidx.webkit:webkit 1.4.0 이상이 필요합니다. 앱 build.gradle 에 implementation "androidx.webkit:webkit:1.4.0" 을 추가하세요. 지금은 메인 프레임 주소로만 출처를 확인합니다.` | `implementation "androidx.webkit:webkit:1.4.0"` 추가 |
| 옛 `addJavascriptInterface(webView)` 를 썼고 iframe 에서 띄운 결제 SDK 의 생체 인증이나 카드 스캔이 보이지 않음 | `BrandPayAuth`, `BrandPayOcr` | `최상위 페이지와 출처가 다른 프레임의 브리지 호출을 버렸습니다. 프레임 출처: <frame>, 최상위 출처: <top>. 다른 출처에서 부르려면 addJavascriptInterface(webView, allowedOriginRules) 로 허용 출처를 정하세요.` | 결제 SDK 를 최상위 페이지와 같은 출처에서 띄웁니다. 다른 출처 iframe 은 웹 SDK 가 응답을 넘기지 못해 새 메서드로도 동작하지 않습니다. |
| 옛 `addJavascriptInterface(webView)` 를 썼고 페이지가 바뀌는 사이에 iframe 이 브리지를 부름 | `BrandPayAuth`, `BrandPayOcr` | `페이지가 바뀌는 중이라 프레임의 브리지 호출을 버렸습니다. 프레임 출처: <frame>. 페이지가 다 바뀐 뒤 부른 호출은 처리됩니다.` | 조치는 필요 없습니다. 페이지가 다 바뀐 뒤 부른 호출은 처리됩니다. |
| 옛 `addJavascriptInterface(webView)` 를 썼고 sandbox iframe 처럼 출처가 없는 프레임이 브리지를 부름 | `BrandPayAuth`, `BrandPayOcr` | `출처가 없는 프레임(sandbox iframe 등)의 브리지 호출을 버렸습니다. 최상위 출처: <top>.` | 결제 SDK 를 sandbox 가 없는, 최상위 페이지와 같은 출처의 프레임에서 띄웁니다. |
| 옛 `addJavascriptInterface(webView)` 를 썼고 file: 같은 출처 없는 최상위 페이지 안의 iframe 이 브리지를 부름 | `BrandPayAuth`, `BrandPayOcr` | `최상위 페이지의 출처를 알 수 없어(file: 등) 프레임의 브리지 호출을 버렸습니다. 프레임 출처: <frame>.` | 결제 SDK 를 최상위 페이지에서 띄웁니다. 최상위 페이지의 호출은 받습니다. |
| 페이지가 브리지에 문자열이 아닌 값(ArrayBuffer 등)을 보냄 | `BrandPayAuth`, `BrandPayOcr` | `문자열이 아닌 브리지 메시지를 버렸습니다. 브리지에는 JSON 문자열을 보내세요.` | 브랜드페이 JS SDK 는 JSON 문자열만 보냅니다. 페이지의 다른 스크립트가 같은 이름의 객체를 부르는지 확인합니다. |
| OCR 브리지를 붙였음(동작은 그대로) | `BrandPayOcr` | `BrandPay 앱 카드 OCR(brandpay-ocr)은 deprecated 되었습니다. 기능은 지금처럼 동작하지만, 가급적 Web OCR 로 옮기는 것을 권장합니다. Web OCR 도 앱 OCR 처럼 별도 계약이 필요하니 담당 매니저나 토스페이먼츠 개발자 커뮤니티(https://developers.tosspayments.com/go/techchat)로 문의하세요.` | 지금 앱은 그대로 동작합니다. 옮기려면 아래 "OCR SDK 에서 Web OCR 로 옮기기" 를 따릅니다. |
| 앱의 `Callback` 구현(`onErrorOccurred` 등)이 예외를 던짐 | `BrandPayAuth`, `BrandPayOcr` | `브리지 메시지를 처리하다 예외가 나서 버렸습니다. 예외: <class>. Callback 구현에서 예외를 던지지 않는지 확인하세요.` | `Callback` 구현에서 예외를 잡습니다. SDK 는 예외를 삼키고 앱을 끝내지 않습니다. `Callback` 은 UI 스레드 밖(처리 스레드 또는 WebView 의 JavaBridge 스레드)에서 불릴 수 있습니다. |
| 옛 `addJavascriptInterface(webView)` 를 썼고 최상위 페이지가 https 가 아님 | `BrandPayAuth`, `BrandPayOcr` | `최상위 페이지가 https 가 아닙니다(<top>). 결제 페이지는 https 로 띄우세요. https 만 허용하려면 addJavascriptInterface(webView, allowedOriginRules) 를 쓰세요.` | 결제 페이지를 https 로 띄웁니다. 동작은 막지 않습니다. |
| 옛 `addJavascriptInterface(webView)` 를 썼는데 webkit 이 없음 | `BrandPayAuth`, `BrandPayOcr` | `androidx.webkit:webkit 1.4.0 이상이 필요합니다. 앱 build.gradle 에 implementation "androidx.webkit:webkit:1.4.0" 을 추가하세요. 지금은 페이지 출처를 확인하지 않습니다.` | `implementation "androidx.webkit:webkit:1.4.0"` 추가 |
| 생체 인증이나 카드 스캔이 보이지 않고 logcat 에 위 문구가 없음 | 없음 | 없음 | `allowedOriginRules` 에 가맹점 페이지 출처가 들어 있는지 확인. iframe 에서 띄우는 구성은 최상위 페이지와 같은 출처일 때만 동작하고, 최상위 페이지 출처만 넣으면 됩니다. |

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

## OCR SDK 에서 Web OCR 로 옮기기

앱 카드 OCR(brandpay-ocr)은 Web OCR 로 옮겨야 합니다. 지금 SDK 는 앱 OCR 을 계속 동작시키지만 더 이상 권장하지 않습니다.

왜 옮기나: Web OCR 은 앱에 OCR 엔진을 넣지 않아 앱 크기가 줄고(앱 OCR 엔진 기준 약 4MB, 라이브러리 파일 기준 추정이며 앱 빌드로 잰 값이 아닙니다), 카드 스캔을 열 때 엔진을 내려받아 OCR 엔진 갱신을 앱 배포 없이 토스페이먼츠 웹 배포로 받습니다.

무엇이 필요하나: 앱 OCR 과 Web OCR 모두 사용 계약(라이선스)이 필요합니다. Web OCR 은 담당 매니저나 [토스페이먼츠 개발자 커뮤니티](https://developers.tosspayments.com/go/techchat)로 문의해 사용을 연 뒤 옮깁니다.

### Web OCR 이 뜨는 조건

결제 화면의 카드 등록에서 아래가 모두 맞을 때 Web OCR 이 뜹니다.

- 앱이 OCR 브리지(`window.ConnectPayOcr`)를 붙이지 않았습니다. 브리지가 있으면 결제 화면은 앱 카드 스캔을 먼저 씁니다.
- 가맹점 페이지가 JS SDK 를 띄울 때 `features.cardOCR.active` 를 `true` 로 넘겼습니다. 기본값은 `false` 입니다. 결제위젯은 `brandpay.features.cardOCR.active` 입니다.
- 가맹점이 Web OCR 사용 계약을 마쳐 토스페이먼츠가 사용을 열었습니다. 가맹점이 직접 켤 수는 없습니다.

브리지가 없는데 나머지 조건이 맞지 않으면 스캔이 없는 기본 카드 입력 화면이 뜹니다. 카드 추가와 결제는 그대로 됩니다.

### 옮기는 순서

1. 가맹점 페이지에서 JS SDK 옵션 `features.cardOCR.active` 를 `true` 로 켭니다. `autoStart: true` 를 함께 넘기면 카드 등록 화면이 열리자마자 스캔을 시작합니다.
2. 담당 매니저나 [토스페이먼츠 개발자 커뮤니티](https://developers.tosspayments.com/go/techchat)로 Web OCR 사용을 문의합니다. 문의할 때 쓸 클라이언트 키(`test_ck_`, `live_ck_` 로 시작)를 알려 주고, 시크릿 키는 적지 않습니다. 사용이 확정된 뒤 다음 단계로 갑니다.
3. 테스트 앱에서 아래 [지울 것](#지울-것)의 필수 항목(OCR 브리지 등록)을 지우고, [카메라 권한](#카메라-권한)을 넣은 빌드로 카드 등록 화면에 Web OCR 이 뜨는지 확인합니다.
4. 배포합니다.

1, 2 가 끝나기 전에 OCR 브리지 등록을 먼저 지우면 스캔이 없는 기본 카드 입력 화면이 뜹니다. 카드 추가와 결제는 그대로 됩니다.

```js
// 1. 브랜드페이 JS SDK v1
const brandpay = BrandPay(clientKey, customerKey, {
  redirectUrl,
  features: { cardOCR: { active: true } }, // autoStart: true 를 더하면 화면이 열리자마자 스캔합니다.
});

// 1. 토스페이먼츠 JS SDK v2 브랜드페이
const brandpayV2 = TossPayments(clientKey).brandpay({
  customerKey,
  redirectUrl,
  features: { cardOCR: { active: true } },
});

// 1. 결제위젯
const widgets = TossPayments(clientKey).widgets({
  customerKey,
  brandpay: { redirectUrl, features: { cardOCR: { active: true } } },
});
```

### 지울 것

#### 필수: OCR 브리지 등록

OCR 브리지가 붙어 있으면 결제 화면이 `window.ConnectPayOcr` 를 보고 앱 카드 스캔을 쓰므로 Web OCR 이 뜨지 않습니다. 아래를 지웁니다. 생체 인증(`BrandPayAuthWebManager`)은 그대로 둡니다.

- `BrandPayOcrWebManager` 를 만드는 코드
- OCR 브리지를 붙이는 `addJavascriptInterface` 호출
- `onActivityResult` 의 `handleActivityResult` 호출
- 앱이 카드 스캔 화면을 직접 열었다면 `BrandPayOcrManager.requestCardScan` 호출

```kotlin
class PaymentActivity : AppCompatActivity() {
    // 생체 인증 브리지는 그대로 둡니다.
    private val authWebManager = BrandPayAuthWebManager(this).apply {
        callback = object : BrandPayAuthWebManager.Callback {
            override fun onPostScript(script: String) {
                webView.loadUrl(script)
            }
        }
    }

    // 아래 OCR 연동을 모두 지웁니다.
    // private val ocrWebManager = BrandPayOcrWebManager(this).apply {
    //     callback = object : BrandPayOcrWebManager.Callback {
    //         override fun onPostScript(script: String) {
    //             webView.loadUrl(script)
    //         }
    //     }
    // }

    private fun attachBridges(webView: WebView) {
        authWebManager.addJavascriptInterface(webView, setOf("https://shop.example.com"))
        // ocrWebManager.addJavascriptInterface(webView, setOf("https://shop.example.com"))  // 지웁니다.
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // ocrWebManager.handleActivityResult(requestCode, resultCode, data)  // 지웁니다.
    }
}
```

#### 선택: 라이브러리와 라이선스 파일 정리

아래는 남겨도 Web OCR 동작에는 영향이 없습니다. 지우면 앱 크기가 줄고, 공개되는 앱 안에 OCR 라이선스 파일이 남지 않습니다.

```
- libs 의 OCR aar(ocr-<버전>.aar) 을 지웁니다. 생체 인증 aar(auth-<버전>.aar)은 둡니다
- settings.gradle 의 include ':OcrEngine' 와 그 모듈 폴더, build.gradle(:app) 의 implementation project(':OcrEngine') 를 지웁니다
- assets 폴더의 OCR 라이선스 파일(.flk)을 지웁니다
- androidx.webkit, androidx.biometric 등 생체 인증과 브리지 출처 검사에 쓰는 의존성은 둡니다
```

OCR aar 을 지우면 그 매니페스트의 카메라 권한이 더 이상 합쳐지지 않습니다. 앱 매니페스트에 `android.permission.CAMERA` 를 직접 적는다(아래 [카메라 권한](#카메라-권한)).

지운 뒤에도 빌드는 그대로 됩니다. 카드 등록 화면은 조건이 맞으면 Web OCR 을, 아니면 스캔이 없는 기본 카드 입력 화면을 띄웁니다.

### 카메라 권한

Web OCR 은 WebView 안의 결제 화면이 카메라를 엽니다. Android WebView 에서는 앱이 두 가지를 해 줘야 카메라가 열립니다.

- 앱의 카메라 권한: OCR aar 을 남기면 그 매니페스트의 `android.permission.CAMERA` 가 앱에 합쳐집니다. OCR aar 을 지웠다면 앱 매니페스트에 `<uses-permission android:name="android.permission.CAMERA" />` 를 직접 적습니다. 어느 쪽이든 실행 중 권한은 받습니다. 앱 카드 스캔은 SDK 화면이 권한을 물었지만, Web OCR 은 앱이 묻습니다.
- WebView 의 권한 요청 허용: 결제 화면의 카메라 요청은 `WebChromeClient.onPermissionRequest` 로 옵니다. 이 메서드를 구현하지 않거나 `WebChromeClient` 를 설정하지 않으면 WebView 가 요청을 거부해, 카메라가 열리지 않습니다. `WebChromeClient` 를 이미 쓰고 있다면 그 클래스에 `onPermissionRequest` 만 더합니다.
- 결제 화면을 담는 iframe 의 카메라 허용(`allow="camera"`)은 JS SDK 가 옵션이 켜졌을 때 넣습니다. 가맹점이 할 일은 없습니다.

```kotlin
private var pendingCameraRequest: PermissionRequest? = null

private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    pendingCameraRequest?.let { if (granted) it.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) else it.deny() }
    pendingCameraRequest = null
}

webView.webChromeClient = object : WebChromeClient() {
    override fun onPermissionRequest(request: PermissionRequest) {
        // 카메라를 켠 프레임이 결제 화면(connect)일 때 카메라만 허용합니다.
        val camera = PermissionRequest.RESOURCE_VIDEO_CAPTURE
        val fromPaymentScreen = request.origin.scheme == "https" && request.origin.host == "connect.tosspayments.com"
        if (camera !in request.resources || !fromPaymentScreen) {
            // 그 밖의 요청은 거부합니다. 앱이 다른 페이지에서 카메라나 마이크를 쓰면 그 처리를 이 자리에 둡니다.
            request.deny()
            return
        }

        if (ContextCompat.checkSelfPermission(this@PaymentActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            request.grant(arrayOf(camera))
        } else {
            pendingCameraRequest = request
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
}
```

### 스캔이 안 되는 기기

- WebView 가 카메라를 여는 `navigator.mediaDevices.getUserMedia` 는 Chromium 53 부터 있습니다. Android 5.0, 6.0 에서 WebView 를 업데이트하지 않은 기기는 그보다 낮을 수 있고, 그 기기에서는 스캔이 실패해 카드 번호를 직접 넣어야 합니다.
- 사용자가 카메라 권한을 거부하면 결제 화면이 카메라 권한이 필요하다는 안내를 띄웁니다. 카드 번호를 직접 넣는 것은 그대로 됩니다.
- 앱 카드 스캔에서 옮기면 이런 기기의 사용자는 스캔을 쓰지 못합니다. 그 사용자 비중을 보고 옮길 시점과 `autoStart` 를 정합니다.

### 옮긴 뒤 확인

- logcat 에 아래 OCR 안내가 남지 않습니다. 0.4.0 은 OCR 브리지를 붙일 때 이 문구를 한 번 남기므로, 문구가 나오면 OCR 연동이 남아 있는 것입니다. 0.3.1 이하는 이 문구를 남기지 않습니다.

```
BrandPay 앱 카드 OCR(brandpay-ocr)은 deprecated 되었습니다. 기능은 지금처럼 동작하지만, 가급적 Web OCR 로 옮기는 것을 권장합니다. Web OCR 도 앱 OCR 처럼 별도 계약이 필요하니 담당 매니저나 토스페이먼츠 개발자 커뮤니티(https://developers.tosspayments.com/go/techchat)로 문의하세요.
```

- 카드 등록 화면에서 Web OCR 스캔이 열리고 카드 번호가 채워집니다.
- 카메라 권한을 거부했을 때 안내가 뜨고, 카드 번호를 직접 넣어 등록할 수 있습니다.

### 문의

Web OCR 사용 계약과 OCR 관련 문의는 담당 매니저나 [토스페이먼츠 개발자 커뮤니티](https://developers.tosspayments.com/go/techchat)로 합니다.

## ⚠️ [Deprecated] OCR 연동 가이드 ver230309

> ### ⚠️ Deprecated
> **이 SDK 기반 OCR 연동은 더 이상 권장되지 않습니다.**  
> Web OCR 도입으로 신규 연동 시에는 Web OCR을 사용해주세요.  
> 관련 문의는 [디스코드](https://developers.tosspayments.com/go/techchat)로 부탁드립니다.

### OCR 라이선스 파일

OCR 라이선스 파일(`.flk`)은 이 저장소에 없습니다. 라이선스는 가맹점마다 발급받으니 담당 매니저나 [토스페이먼츠 개발자 커뮤니티](https://developers.tosspayments.com/go/techchat)로 문의합니다.

- 받은 파일은 `ui/src/main/assets/fincube_license.flk` 로 넣습니다. 라이선스 파일은 저장소에 올리지 않으며, 이 샘플의 `.gitignore` 는 `*.flk` 를 뺍니다.
- 파일이 없으면 샘플의 OCR 카드 스캔은 동작하지 않습니다. 생체 인증은 그대로 동작합니다.

옮기는 방법은 위 [OCR SDK 에서 Web OCR 로 옮기기](#ocr-sdk-에서-web-ocr-로-옮기기) 를 봅니다.

<details>
<summary>기존 연동 가이드 보기 (유지보수 목적)</summary>

#### 버전 변경사항 (2025년 9월 26일)
- 구버전 라이센스 키 호환 이슈 수정

#### 버전 변경사항 (2025년 9월 23일)
- 16KB 페이지 크기를 지원하도록 수정 

### 라이브러리 추가

- libs/ocr-0.4.0.aar 추가 
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

        // 필수: 브리지 출처 검사(옛 메서드의 기본 출처 검사, 새 메서드의 허용 출처)
        implementation "androidx.webkit:webkit:1.4.0"
    
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

            // 허용 출처는 위 "허용 출처 정하기" 를 참고하세요.
            brandPayOcrWebManager.addJavascriptInterface(this, setOf("https://shop.example.com"))
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