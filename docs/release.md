# راهنمای تولید کلید امضا و انتشار نسخه نهایی (Release)

این سند مراحل تولید فایل کلید امنیتی (Upload Keystore)، تبدیل آن به فرمت Base64، و انتشار نسخه نهایی برنامه اندروید **نوش (Noosh)** را آموزش می‌دهد.

---

## ۱. تولید فایل Keystore امضای برنامه

برای امضای نسخه Release از ابزار استاندارد `keytool` موجود در JDK استفاده کنید. ترمینال را باز کرده و دستور زیر را اجرا نمایید:

```bash
keytool -genkey -v -keystore release.keystore -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

### فیلدهای درخواستی:
1. **Keystore password**: رمزی قوی وارد کنید (این مقدار برابر با `STORE_PASSWORD` و `KEY_PASSWORD` خواهد بود).
2. **First and Last Name**: نام خود یا سازمان را وارد کنید (مثلاً `Noosh Team`).
3. **Organizational Unit / Organization / City / State / Country**: مشخصات را تکمیل یا با زدن Enter عبور کنید.
4. در پایان با تایپ `yes` تایید کنید.

> **نکته بسیار مهم**: فایل `release.keystore` و رمز آن را در مکانی امن نگهداری کنید. گم شدن این کلید مانع از آپدیت برنامه در مارکت‌های اندروید خواهد شد. این فایل نباید در گیت کامیت شود.

---

## ۲. تبدیل Keystore به رشته Base64

برای ذخیره کلید در GitHub Secrets به عنوان `RELEASE_KEYSTORE_BASE64`، دستور متناسب با سیستم‌عامل خود را اجرا کنید:

### لینوکس / مک (Linux / macOS):
```bash
base64 -i release.keystore -o release.keystore.base64
# یا در لینوکس:
base64 -w 0 release.keystore > release.keystore.base64
```

### ویندوز (PowerShell):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File -FilePath release.keystore.base64 -Encoding ascii -NoNewline
```

محتوای فایل تولیدشده `release.keystore.base64` را باز کرده و بدون هیچ فاصله اضافی در GitHub Secret به نام `RELEASE_KEYSTORE_BASE64` قرار دهید.

---

## ۳. بررسی و راستی‌آزمایی Keystore

برای اطمینان از صحت فایل کلید و مشاهده Alias و اثرانگشت (SHA-1 / SHA-256):

```bash
keytool -list -v -keystore release.keystore -alias upload
```

---

## ۴. انتشار نسخه از طریق Tagging در Git

به محض اینکه تگ نسخه جدیدی به گیت‌هاب فرستاده شود، پایپ‌لاین `release-apk.yml` به طور خودکار نسخه جدید را بیلد کرده و در قالب GitHub Release منتشر می‌سازد:

```bash
# ایجاد تگ برای نسخه ۱.۰.۰
git tag -a v1.0.0 -m "انتشار نسخه ۱.۰.۰ نوش"

# ارسال تگ به گیت‌هاب
git push origin v1.0.0
```

---

## ۵. چک‌لیست نهایی قبل از انتشار
- [ ] تست‌های واحد با موفقیت پاس شوند (`./gradlew testDebugUnitTest`)
- [ ] مقدار `versionCode` و `versionName` در `app/build.gradle.kts` بررسی و به‌روزرسانی شود
- [ ] سکرت‌های `RELEASE_KEYSTORE_BASE64`، `STORE_PASSWORD`، `KEY_ALIAS`، `KEY_PASSWORD` در گیت‌هاب تنظیم شده باشند
- [ ] فایل `google-services.json` دارای نام پکیج `com.aistudio.noosh.water` باشد
