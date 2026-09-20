# Claudemani

اپلیکیشن Android آفلاین یادگیری واژگان اسپانیایی↔فارسی با فلش‌کارت،
مرور چهارگزینه‌ای (Quiz) و Spaced Repetition. کاملاً Offline-First —
بدون هوش مصنوعی، بدون سرویس ابری، بدون تبلیغ.

بازنویسی کامل پروژه از صفر مطابق مشخصات `docs/Algorithms_v4.20.docx` و
`docs/Descriptions_v4.20.docx`. هر ۳۰ فاز برنامه‌ریزی‌شده تکمیل شده‌اند؛
جزئیات کامل هر فاز، تصمیم‌های آگاهانه گرفته‌شده، و فهرست دقیق شکاف‌های
شناخته‌شده در `docs/PROGRESS_TRACKER.md` نگهداری می‌شود — آن فایل، نه این
README، مرجع دقیق و به‌روز وضعیت پروژه است.

## معماری

Clean Architecture + MVVM (Jetpack Compose)، ۵ ماژول Gradle:

| ماژول | نقش |
|---|---|
| `domain` | مدل‌ها، الگوریتم‌های Pure (Learning Transition، Difficulty، Quiz Generation، ...)، رابط‌های Repository، UseCaseها. ماژول Kotlin خالص — بدون هیچ وابستگی به Android. |
| `database` | Room Entities، DAOها، Migrations، TypeConverters. |
| `data` | پیاده‌سازی Repositoryها (رابط‌های `domain` را با DAOهای `database` وصل می‌کند)، Mapperها، Hilt Modules. |
| `core` | ابزارهای مشترک بین ماژول‌ها. |
| `app` | Jetpack Compose UI، Navigation، ViewModelها، Hilt wiring، نقطه ورود (`MainActivity`). |

جریان داده: `Compose UI → ViewModel → UseCase → Repository (interface در domain) ← پیاده‌سازی (data) → DAO → Room`.

## Build و تست

```
gradle clean assembleDebug
gradle test
```

این پروژه عمداً **بدون Gradle Wrapper** (`gradlew`) commit شده — تصمیم
ثبت‌شده در تراکر (۲۰۲۶-۰۹-۱۳): تولید باینری واقعی `gradle-wrapper.jar` در
محیطی بدون Android Studio ممکن نبود. برای اجرای محلی:
- یا Gradle **8.2** را سراسری نصب کنید (همون نسخه‌ای که CI هم پین کرده)،
- یا یک‌بار `gradle wrapper --gradle-version 8.2` را اجرا کنید تا خودتان Wrapper را بسازید و کامیت کنید.

CI (`.github/workflows/android-ci.yml`) دقیقاً همین دو دستور را روی هر
push/PR اجرا می‌کند، از طریق `gradle/actions/setup-gradle` (بدون نیاز
به Wrapper) — بدون نیاز به Emulator، چون تست‌های یکپارچگی روی Room واقعی
هم از طریق Robolectric به‌صورت تست Unit معمولی اجرا می‌شوند (جزئیات
کامل در «جزئیات فاز ۲۹» در تراکر).

نیازمندی‌ها: JDK 17، Gradle 8.2 (سراسری، تا وقتی Wrapper ساخته نشده)،
Android SDK (compileSdk 34، minSdk 26).

## وضعیت فعلی

هر ۳۰ فاز برنامه‌ریزی‌شده **Done** اعلام شده‌اند (جدول کامل در
`docs/PROGRESS_TRACKER.md`). این یعنی:

- هر دو الگوریتم اصلی (Learning Transition، Difficulty Calculation) پیاده و تست‌شده‌اند.
- مسیر Create/Submit اتمیک است و با تست یکپارچگی روی Room واقعی تأیید شده (نه فقط Fake Repository).
- تمام ۷ صفحه (Home، Review-Flashcard، Review-Quiz، AddWord، Progress، Settings، Categories، About) واقعی‌اند — هیچ Placeholder‌ای باقی نمانده.
- Backup/Restore به‌صورت منطقی (در حافظه، بین دو UseCase) کامل و با تست دو-دیتابیسه تأیید شده.
- Parser واژگان فقط بخش P0 (طبق اولویت‌بندی خود سند در Algorithms §93) پیاده شده.

## شکاف‌های شناخته‌شده

موارد ۱ تا ۴ در فاز ۳۶ رفع شدند. موارد ۵ و ۶ هنوز باز هستند:

1. ~~UI برای Backup/Restore وجود ندارد~~ **رفع شد** — صفحه Settings حالا دکمه پشتیبان‌گیری/بازیابی دارد (فرمت JSON، از طریق Storage Access Framework؛ جزئیات در «فاز ۳۶» تراکر).
2. ~~انتخاب Category هنگام افزودن کلمه وایر نشده~~ **رفع شد** — فرم AddWord حالا یک Dropdown دسته‌بندی دارد.
3. ~~بررسی Achievement فقط با باز کردن صفحه Progress~~ **رفع شد** — حالا در پایان هر Review Session هم بررسی می‌شود (علاوه بر Progress، نه به‌جای آن).
4. ~~هیچ Consumer واقعی از LanguagePairRepository نمی‌خواند~~ **رفع شد** — `ReviewViewModel` حالا از `GetActiveLanguagePairUseCase` واقعی می‌خواند.
5. ~~Import/Export چندفرمتی (CSV/XLSX/SQLite طبق Algorithms §9) پیاده
   نشده~~ **بخش CSV رفع شد** (فاز ۳۷؛ صفحه Settings دکمه Import/Export CSV دارد) — XLSX و SQLite همچنان پیاده نشده‌اند (دلیل در «فاز ۳۷» تراکر: کتابخانه سنگین/غیرقابل‌تست برای XLSX، Schema ناشناس برای SQLite).
6. **موارد Backlog صریح خودِ سند:**
   - ~~Quiz Difficulty مستقل از Vocabulary Difficulty~~ + ~~Category-aware Distractor Selection~~ **رفع شد** (فاز ۳۸؛ سه‌سطحی EASY/MEDIUM/HARD در Settings).
   - ~~رمزنگاری Backup~~ **رفع شد** (فاز ۳۸؛ AES/GCM با کلید مشتق از PIN کاربر — نه Android Keystore، به دلیلی که در تراکر توضیح داده شده).
   - **چند LanguagePair هم‌زمان همچنان پیاده نشده** — نیازمند Migration واقعی Schema دیتابیس (افزودن `languagePairId` به `LearningState`)، عمداً به‌عنوان یک فاز جداگانه در آینده گذاشته شد، نه قاطی این فاز.

## مستندات

- `docs/Algorithms_v4.20.docx` — مرجع الگوریتم‌ها (منبع اصلی هر رفتار محاسباتی).
- `docs/Descriptions_v4.20.docx` — مرجع توضیحات، مدل داده، قوانین UI، Audit Reportها.
- `docs/PROGRESS_TRACKER.md` — تنها منبع معتبر وضعیت لحظه‌ای پروژه: جدول ۳۰ فاز، جزئیات کامل هر فاز، هر تصمیم آگاهانه، و فهرست کامل شکاف‌های باز.
