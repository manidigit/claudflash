# FlashLearn

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

CI (`.github/workflows/`) دقیقاً همین دو دستور را روی هر push/PR اجرا
می‌کند — بدون نیاز به Emulator، چون تست‌های یکپارچگی روی Room واقعی هم
از طریق Robolectric به‌صورت تست Unit معمولی اجرا می‌شوند (جزئیات کامل
در «جزئیات فاز ۲۹» در تراکر).

نیازمندی‌ها: JDK 17، Gradle 8.2+، Android SDK (compileSdk 34،
minSdk 26).

## وضعیت فعلی

هر ۳۰ فاز برنامه‌ریزی‌شده **Done** اعلام شده‌اند (جدول کامل در
`docs/PROGRESS_TRACKER.md`). این یعنی:

- هر دو الگوریتم اصلی (Learning Transition، Difficulty Calculation) پیاده و تست‌شده‌اند.
- مسیر Create/Submit اتمیک است و با تست یکپارچگی روی Room واقعی تأیید شده (نه فقط Fake Repository).
- تمام ۷ صفحه (Home، Review-Flashcard، Review-Quiz، AddWord، Progress، Settings، Categories، About) واقعی‌اند — هیچ Placeholder‌ای باقی نمانده.
- Backup/Restore به‌صورت منطقی (در حافظه، بین دو UseCase) کامل و با تست دو-دیتابیسه تأیید شده.
- Parser واژگان فقط بخش P0 (طبق اولویت‌بندی خود سند در Algorithms §93) پیاده شده.

## شکاف‌های شناخته‌شده (عمداً حل‌نشده)

این‌ها تصمیم‌های آگاهانه‌اند، نه فراموشی — هرکدام در تراکر با دلیل کامل
ثبت شده‌اند:

1. **UI برای Backup/Restore وجود ندارد.** منطق UseCase کامل است، اما
   هیچ فاز مشخصی در برنامه ۳۰ فاز برای «کجا این فایل نوشته/خوانده شود»
   وجود نداشت.
2. **انتخاب Category هنگام افزودن کلمه وایر نشده.** فرم AddWord فیلد
   Category ندارد؛ صفحه مدیریت دسته‌بندی‌ها کاملاً مستقل ساخته شده.
3. **بررسی Achievement فقط با باز کردن صفحه Progress اجرا می‌شود**، نه
   بعد از هر پاسخ Review — سند هر دو نقطه را مجاز می‌داند، فقط یکی
   سیم‌کشی شد.
4. **هیچ Consumer واقعی هنوز از `LanguagePairRepository` نمی‌خواند** —
   خودِ Seed انجام شد (فاز ۳۱: `EnsureDefaultLanguagePairUseCase`، حالا
   یک ردیف واقعی `es→fa` در `language_pairs` وجود دارد)، اما
   Quiz/Flashcard هنوز از `defaultV1LanguagePair()` هاردکد استفاده
   می‌کنند نه از `getActive()`. وصل‌کردن این دو (تغییر Constructor چند
   UseCase موجود) قدم بعدی است.
5. **Import/Export چندفرمتی (CSV/XLSX/SQLite طبق Algorithms §9) پیاده
   نشده** — این بازنویسی فقط Paste-Text Parser (P0) و Backup/Restore
   منطقی (نه فایل) را در برنامه ۳۰ فاز داشت؛ فرمت‌های فایلی مجزا از
   ابتدا در دامنه این کار نبودند.
6. **موارد Backlog صریح خودِ سند** (Quiz Difficulty مستقل از Vocabulary
   Difficulty، Category-aware Distractor Selection، رمزنگاری Backup،
   چند LanguagePair هم‌زمان) — طبق §19 سند، این‌ها از ابتدا «توسعه
   آینده» اعلام شده بودند، نه بخشی از V1.

## مستندات

- `docs/Algorithms_v4.20.docx` — مرجع الگوریتم‌ها (منبع اصلی هر رفتار محاسباتی).
- `docs/Descriptions_v4.20.docx` — مرجع توضیحات، مدل داده، قوانین UI، Audit Reportها.
- `docs/PROGRESS_TRACKER.md` — تنها منبع معتبر وضعیت لحظه‌ای پروژه: جدول ۳۰ فاز، جزئیات کامل هر فاز، هر تصمیم آگاهانه، و فهرست کامل شکاف‌های باز.
