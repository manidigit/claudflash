# FlashLearn — Progress Tracker (بازنویسی کامل v5.0)

**آخرین به‌روزرسانی:** 2026-09-18 (فاز ۳۵ — رفع ۳ Failure واقعی تست)
**مرجع مشخصات:** Algorithms v4.20 + Descriptions v4.20
**نوع کار:** بازنویسی کامل از صفر (نه ادامه کد قبلی v4.36)
**تقسیم‌بندی:** ۳۰ مرحله؛ هر مرحله یک zip کامل و قابل build

---

## قانون این سند

هر مرحله بعد از تکمیل باید در جدول زیر با تاریخ و توضیح کوتاه ثبت شود.
هیچ مرحله‌ای «Done» اعلام نمی‌شود مگر اینکه در zip همان مرحله واقعاً
موجود و (در صورت لزوم) به بقیه لایه‌ها متصل باشد.

---

## جدول ۳۰ مرحله

| # | عنوان | وضعیت | تاریخ |
|---|-------|--------|-------|
| 1 | اسکلت پروژه: Gradle، ۵ ماژول، CI خام | **Done** | 2026-09-13 |
| 2 | Domain Models/Enums | **Done** | 2026-09-13 |
| 3 | Learning Transition Algorithm + Test | **Done** | 2026-09-13 |
| 4 | Difficulty Calculation Algorithm + Test | **Done** | 2026-09-13 |
| 5 | Repository Interfaces + FlashLearnDatabase | **Done** | 2026-09-13 |
| 6 | Room Entities + TypeConverters | **Done** | 2026-09-13 |
| 7 | Room DAOها | **Done** | 2026-09-13 |
| 8 | Room Database + Migrations | **Done** | 2026-09-13 |
| 9 | Repository پیاده‌سازی (Data) + Mapperها | **Done** | 2026-09-13 |
| 10 | CreateConceptUseCase (اتمیک) | **Done** | 2026-09-13 |
| 11 | SubmitReviewAnswerUseCase (اتمیک) | **Done** | 2026-09-13 |
| 12 | SelectReviewQueue + Session UseCaseها | **Done** | 2026-09-13 |
| 13 | Category (کامل) | **Done** | 2026-09-13 |
| 14 | GenerateQuizQuestion Algorithm | **Done** | 2026-09-14 |
| 15 | Statistics/Progress/Streak/Achievement | **Done** | 2026-09-14 |
| 16 | Backup/Restore — مدل + Validator | **Done** | 2026-09-14 |
| 17 | CreateBackup/RestoreBackup اجرایی | **Done** | 2026-09-14 |
| 18 | Vocabulary Parser P0 (State Machine) | **Done** | 2026-09-14 |
| 19 | RefreshDataUseCase + dataVersion | **Done** | 2026-09-14 |
| 20 | Settings UseCaseها + AppSetting Repo | **Done** | 2026-09-14 |
| 21 | Hilt Modules کامل | **Done** | 2026-09-14 |
| 22 | Navigation + Routes + AppViewModel | **Done** | 2026-09-14 |
| 23 | Home Screen | **Done** | 2026-09-16 |
| 24 | AddWord Screen (+ Parser) | **Done** | 2026-09-16 |
| 25 | Review Screen — Flashcard | **Done** | 2026-09-17 |
| 26 | Review Screen — Quiz + انتخاب نوع مرور | **Done** | 2026-09-17 |
| 27 | Progress Screen + Achievements UI | **Done** | 2026-09-17 |
| 28 | Settings + About + Category UI | **Done** | 2026-09-18 |
| 29 | تست یکپارچگی + رفع خطا + تکمیل CI | **Done** | 2026-09-18 |
| 30 | پالیش نهایی + README + zip نهایی | **Done** | 2026-09-18 |

---

## جزئیات فاز ۱ (تکمیل‌شده)

- ساختار ۵ ماژول: `app`, `core`, `domain`, `database`, `data`
- `settings.gradle.kts` + root `build.gradle.kts` با نسخه‌های پلاگین پین‌شده:
  AGP 8.2.2، Kotlin 1.9.20، KSP 1.9.20-1.0.14، Hilt 2.48
- `domain`: ماژول Kotlin خالص (java-library + kotlin.jvm)، بدون وابستگی Android — طبق اصل معماری
- `database`: Room 2.6.1 با KSP (`room.generateKotlin=true`, نه kapt)
- `data`: Hilt wiring آماده (هنوز Module واقعی ندارد — فاز ۲۱)
- `app`: Compose (BOM 2024.02.00) + Navigation Compose 2.7.6 + Hilt + `hilt-navigation-compose`
- `MainActivity`/`FlashLearnApplication` فقط Skeleton — منطق واقعی در فاز ۲۲ به بعد
- `.gitignore`: local.properties، keystore، google-services.json، build/، schemas/ پوشش داده شده
- CI: `.github/workflows/android-ci.yml` — از `gradle/actions/setup-gradle` استفاده می‌کند
  (بدون نیاز به commit کردن Gradle wrapper باینری)؛ marshalling APK به `dist/` مطابق تصمیم قبلی پروژه
- min API 26، compileSdk/targetSdk 34، JDK 17 — مطابق Spec بخش ۱۸.۱

## جزئیات فاز ۲ (تکمیل‌شده)

فایل‌های جدید در `domain/src/main/kotlin/com/flashlearn/domain/`:

- `model/Enums.kt` — Stage، VocabularyDifficulty (ordinal مهم است)، ReviewType، EntryType، AchievementType
- `model/Concept.kt`, `Content.kt` (+ `computeCanonicalKey`), `LearningState.kt` (بدون فیلد difficulty)، `DifficultyState.kt`
- `model/ReviewModels.kt` (ReviewSession + ReviewHistory)، `Category.kt` (+Tag+ConceptTag)، `Language.kt` (+LanguagePair)، `AppSetting.kt` (+SettingKeys)، `Achievement.kt`، `ProgressSummary.kt`
- `algorithm/TransitionResult.kt` — فقط data class؛ خود تابع محاسبه در فاز ۳
- `exception/FlashLearnExceptions.kt` — DataIntegrityException، DuplicateReviewAttemptException، ReviewNotDueException

تست‌های جدید: `ContentTest` (canonicalKey/accent)، `EnumsTest` (ordinal guard).

نکات:
- `LanguagePair` بدون `languagePairId` روی LearningState — طبق تصمیم V1 (فقط یک جفت‌زبان فعال).
- `QuizDifficulty` enum عمداً اضافه نشد — Backlog Item بدون Pseudocode تأییدشده (O.3)؛ در فاز طراحی مربوطه اگر تأیید شود اضافه می‌شود.
- `dataVersion` روی Concept/Content برای Refresh (فاز ۱۹) از حالا موجود است.

## جزئیات فاز ۳ (تکمیل‌شده)

- `domain/algorithm/LearningTransitionAlgorithm.kt` — `calculateLearningTransition()` پیاده‌سازی کامل جدول بخش ۵ (Pure، بدون خواندن ساعت سیستم؛ `reviewedAt` تنها منبع «الان»)، + `startOfNextCalendarDay()`.
- تست‌ها (`LearningTransitionAlgorithmTest`): تمام ۶ حالت جدول Transition + invariant‌های hasPathFailure (هرگز Reset نمی‌شود)، monthlyWrongCount (تجمعی)، و دو تست timezone (UTC و +۳:۳۰ شبیه تهران) برای `startOfNextCalendarDay`.
- هیچ تغییری در Difficulty انجام نمی‌شود — طبق مرزبندی صریح Algorithm (بخش ۷ سند).

## جزئیات فاز ۴ (تکمیل‌شده)

- `domain/algorithm/DifficultyCalculationAlgorithm.kt` — `calculateDifficulty()` کامل: Forced Update برای WEEKLY/MONTHLY (اولویت بالاتر)، مسیر عادی شمارنده متوالی با threshold پیش‌فرض ۳، و `hasReachedVeryHard` monotonic.
- تست‌ها (۱۸ مورد): Forced Update (WEEKLY/MONTHLY، شامل عدم اعمال روی RANDOM/LEARNED)، مسیر عادی، کف EASY/سقف VERY_HARD، شکستن زنجیره، threshold سفارشی، invariant دو شمارنده هرگز هم‌زمان >0 نیستند، Determinism.
- **نکته قرارداد مهم برای فاز ۱۱ (SubmitReviewAnswerUseCase):** این تابع نباید وقتی `learningState.stage == LEARNED` صدا زده شود؛ و `monthlyWrongCountBefore` باید مقدار *قبل* از اجرای Transition باشد.

## جزئیات فاز ۵ (تکمیل‌شده)

- `domain/repository/` — ۱۲ Interface: `FlashLearnDatabase` (transaction bridge)، `ConceptRepository` (+`findAnyById` برای Restore روی رکورد soft-deleted)، `ContentRepository`، `LearningStateRepository`، `DifficultyStateRepository`، `ReviewHistoryRepository`، `ReviewSessionRepository`، `CategoryRepository`/`TagRepository`/`ConceptTagRepository`، `LanguageRepository`/`LanguagePairRepository`، `SettingsRepository`، `AchievementRepository`.
- نام متدها دقیقاً مطابق اصلاح Phase 1 v4 (`get` نه `getByConceptId`، `upsert` برای Content، `insert(ConceptTag(...))` نه `addTagToConcept`).
- **تصمیم معماری:** چون `id: UUID` مستقیماً Primary Key است (نه Long داخلی + UUID جدا)، `findByUuid` معادل `getById`/`findAnyById` است — پیچیدگی نگاشت ID اضافه نشد.
- تست‌ها: `FakeRepositories.kt` (پیاده‌سازی in-memory، فقط زیر `src/test`) + `RepositoryContractRehearsalTest` که کل مسیر CreateConcept→SubmitReviewAnswer را با این Interfaceها و الگوریتم‌های فاز ۳/۴ به‌صورت end-to-end (بدون Room) تمرین می‌کند — تأیید می‌کند Interfaceها برای فاز ۱۰/۱۱ کافی‌اند.
- وابستگی تست جدید در `domain/build.gradle.kts`: `kotlinx-coroutines-core` + `kotlinx-coroutines-test` (فقط testImplementation).
- متدهای Aggregate خیلی خاص (مثل شمارش‌های Statistics/Achievement) عمداً اضافه نشدند؛ در فاز ۱۵/۱۶ که UseCase واقعی نوشته می‌شود اضافه خواهند شد (طبق اصل «اختراع نکردن جلوتر از نیاز»).

## جزئیات فاز ۶ (تکمیل‌شده)

- `database/Converters.kt` — UUID↔String، Instant↔Long (epoch millis). Enumها به‌صورت `.name` String مستقیم ذخیره می‌شوند (بدون Converter)، مطابق دقیق Code v4.10.
- ۱۳ Entity در `database/entity/`: `ConceptEntity`, `ContentEntity`, `LearningStateEntity` (بدون فیلد difficulty)، `DifficultyStateEntity`، `TagEntity`+`ConceptTagEntity`، `ReviewSessionEntity`+`ReviewHistoryEntity`، `SettingsEntity`، `AchievementEntity`، `CategoryEntity`، `LanguageEntity`+`LanguagePairEntity`.
- تست: `ConvertersTest` (round-trip UUID/Instant، شامل null).

**تصمیم آگاهانه ثبت‌شده:** سند Code v4.10 در یک قطعه نمونه فقط ۹ Entity هسته‌ای را نشان می‌دهد («all nine entities are registered»)، اما جدول Entity رسمی بخش ۴ Descriptions و Repository Interfaceهای فاز ۵ به‌وضوح Category/Language/LanguagePair/Achievement را هم به‌عنوان موجودیت پایدار می‌خواهند. این ۴ Entity اضافه شدند (جمعاً ۱۳) — یک «اصلاح یکپارچه‌سازی» بین دو بخش ناسازگار همان اسناد، نه اختراع جدید.

**تصمیم آگاهانه دوم:** Unique Index شرطی روی `language_pairs.isActive` (`WHERE isActive=1`) در Room `@Entity indices` قابل تعریف نیست (Room از Partial Index پشتیبانی declarative ندارد). این قید در فاز ۸ به‌صورت SQL خام در callback ساخت دیتابیس اضافه می‌شود؛ فعلاً فقط Index معمولی (غیر-Unique) روی `isActive` گذاشته شد و قاعده «فقط یک جفت‌زبان فعال» در UseCase فاز ۲۰ اعمال می‌شود.

## جزئیات فاز ۷ (تکمیل‌شده)

- ۱۳ DAO در `database/dao/`، هر کدام دقیقاً منطبق با نام متدهای Repository Interface فاز ۵: `ConceptDao` (+`getByIdAny` برای Restore روی رکورد soft-deleted)، `ContentDao`، `LearningStateDao` (شامل `getDueByStage`، `getAllDueNonLearned` برای Random Review)، `DifficultyStateDao`، `TagDao`+`ConceptTagDao` (Insert IGNORE برای Idempotent)، `ReviewSessionDao`+`ReviewHistoryDao` (append-only، بدون Update/Delete)، `SettingsDao`+`AchievementDao`، `CategoryDao`، `LanguageDao`+`LanguagePairDao`.
- برای عملیات upsert واقعی (Content/LearningState/DifficultyState/Settings/Achievement) از annotation بومی `@Upsert` روم ۲.۶ استفاده شد (نه `@Insert(onConflict=REPLACE)` قدیمی) — واقعاً UPDATE می‌کند نه delete+insert.
- ستون‌های `key` و `value` در `SettingsDao` چون کلمات رزرو شده SQL هستند، در Query خام با backtick کوئوت شدند.

**نکته مهم برای فاز ۸:** این DAOها هنوز به هیچ کلاس Database واقعی وصل نیستند (چون Room نیاز به کلاس `@Database` دارد) — پس در این فاز قابل تست اجرایی (instrumented) نیستند؛ اتصال در فاز ۸ انجام می‌شود.
**تصمیم آگاهانه:** فعلاً تست Instrumented (نیازمند Emulator در CI) اضافه نشد؛ CI فعلی فقط `assembleDebug`+`test` (Unit) دارد. اگر لازم شد، در فاز ۲۹ (تست یکپارچگی) بازبینی می‌شود. **[بازبینی‌شده در فاز ۲۹: به‌جای Emulator از Robolectric برای تست واقعی Room استفاده شد — جزئیات در «جزئیات فاز ۲۹».]**

## جزئیات فاز ۸ (تکمیل‌شده)

- `FlashLearnRoomDatabase.kt` — کلاس `@Database` با هر ۱۳ Entity + ۱۳ DAO abstract fun، `FLASHLEARN_SCHEMA_VERSION = 1`، `exportSchema = true`.
- `migration/DatabaseMigrations.kt` — `ALL_MIGRATIONS` فعلاً خالی (نسخه ۱ = اولین انتشار)؛ رویه دقیق افزودن Migration بعدی در KDoc مستند شد.
- `FlashLearnDatabaseCallback.kt` — Partial Unique Index روی `language_pairs(isActive) WHERE isActive=1` از طریق `RoomDatabase.Callback.onCreate` (طبق تصمیم فاز ۶).
- تست: `FlashLearnRoomDatabaseTest` (schema version=1، ALL_MIGRATIONS خالی، DATABASE_NAME).

**نکته مهم برای فاز ۲۱ (Hilt Modules):** ساخت واقعی نمونه Room (`Room.databaseBuilder(...).addMigrations(*ALL_MIGRATIONS).addCallback(FlashLearnDatabaseCallback).build()`) در `data` module انجام می‌شود، نه اینجا — این ماژول فقط Schema را تعریف می‌کند.

## جزئیات فاز ۹ (تکمیل‌شده)

- `data/mapper/` — ۱۰ فایل Mapper (object با `toDomain`/`toEntity`) برای هر ۱۳ Entity؛ enumها همه‌جا با `.name`/`.valueOf` تبدیل می‌شوند.
- `data/repository/` — پیاده‌سازی کامل هر ۱۲ Repository Interface فاز ۵ + `FlashLearnDatabaseImpl` (روی `roomDb.withTransaction`). همه با `@Singleton @Inject constructor` — آماده Binding در فاز ۲۱ (هنوز خود Hilt Module نوشته نشده).
- `SettingsRepositoryImpl.getInt/getBoolean` مقدار پیش‌فرض را در نبود کلید یا parse ناموفق برمی‌گردانند.
- تست: `MapperRoundTripTest` — تمام Mapperهای دارای enum (Concept/EntryType، LearningState/Stage، DifficultyState/VocabularyDifficulty، ReviewHistory+ReviewSession/ReviewType، Achievement/AchievementType) برای **هر مقدار enum** round-trip می‌شوند تا typo بین `.name` و `valueOf` فوراً مشخص شود.

**نکته برای فاز ۲۱:** این پیاده‌سازی‌ها هنوز به هیچ Hilt Module بایند نشده‌اند (`@Binds`) و Room instance واقعی هم ساخته نشده — یعنی اپ هنوز کامپایل/اجرا نمی‌شود به‌طور کامل تا فاز ۲۱، اما لایه‌های domain/database/data هرکدام مستقل قابل تست و کامپایل‌اند.

## جزئیات فاز ۱۰ (تکمیل‌شده)

- `domain/usecase/CreateConceptUseCase.kt` — `CreateConceptCommand` (پیش‌فرض es→fa) + UseCase اتمیک: Concept + دو Content (source/target) + LearningState(DAILY) + DifficultyState(EASY) + Tagهای اختیاری، همه داخل یک `database.withTransaction`.
- اعتبارسنجی Edge Case §8 («حداقل یک ترجمه لازم است») با Exception اختصاصی `InvalidConceptInputException` (نه IllegalArgumentException عمومی).
- `FakeRepositories.kt` (فاز ۵) گسترش یافت: `FakeContentRepository`، `FakeConceptTagRepository`.
- تست‌ها (`CreateConceptUseCaseTest`، ۶ مورد): ساخت کامل با تمام Stateهای اولیه صحیح، محاسبه canonicalKey، اتصال Tag، خطای ورودی خالی (source/target)، override جفت‌زبان پیش‌فرض.
- **عمداً اضافه نشده:** بررسی Duplicate/Concept-Matching (ResolveConceptForParsedEntry) — این منطق مخصوص مسیر Import/Parser است (فاز ۱۸)، نه AddWord دستی؛ طبق روش «بدون گسترش دامنه بدون بحث».

## جزئیات فاز ۱۱ (تکمیل‌شده) — مهم‌ترین UseCase پروژه

- `domain/usecase/SubmitReviewAnswerUseCase.kt` — مسیر مشترک ثبت پاسخ Flashcard/Quiz، اتمیک، با ترتیب دقیق اجباری v4.10:
  ۱) بارگذاری LearningState+DifficultyState (نبودشان → `DataIntegrityException`) ۲) بررسی Duplicate روی (sessionId, reviewAttemptId) — **قبل از** Due Validation ۳) Due Validation (بجز LEARNED) ۴) Learning Transition (pure) ۵) Difficulty Calculation (pure، فقط اگر stage≠LEARNED، با `monthlyWrongCountBefore` = مقدار *قبل* از Transition) ۶-۸) ذخیره LearningState، DifficultyState، افزودن ReviewHistory.
- `threshold_difficulty` از `SettingsRepository` خوانده می‌شود (پیش‌فرض ۳ در نبود مقدار).
- `FakeRepositories.kt` گسترش یافت: `FakeSettingsRepository`.
- تست‌ها (`SubmitReviewAnswerUseCaseTest`، ۱۲ مورد): مسیر موفق، دو حالت DATA_INTEGRITY_ERROR، **اثبات ترتیب Duplicate-before-Due** (سناریویی که هر دو خطا ممکن بود رخ دهد ولی Duplicate باید اول تشخیص داده شود)، Due Validation مستقل، LEARNED (همیشه مجاز + Difficulty دست‌نخورده)، Forced Update برای WEEKLY/MONTHLY (شامل تشخیص اولین/بعدی MONTHLY)، threshold پیش‌فرض و سفارشی از Settings، صحت ثبت ReviewHistory، صحت totalCorrect/totalWrong/hasPathFailure.

## جزئیات فاز ۱۲ (تکمیل‌شده)

- `domain/usecase/SelectReviewQueueUseCase.kt` — پیاده‌سازی کامل الگوریتم SelectReviewQueue: DAILY/WEEKLY/MONTHLY (due + ordered nextReviewAt ASC, conceptId ASC)، LEARNED (بدون فیلتر nextReviewAt، Shuffle)، RANDOM (فقط due DAILY+WEEKLY+MONTHLY طبق اصلاح Appendix O.1، Shuffle). فیلترهای difficulty/category/tag با AND ترکیب می‌شوند.
- **تصمیم آگاهانه:** فیلتر `languagePair` در `ReviewQueueFilters` نگه داشته شد ولی فعلاً no-op است — چون Concept/LearningState در V1 فیلد `languagePairId` ندارند (طبق تصمیم V1: فقط یک جفت‌زبان فعال، بخش ۱۹.۱). وقتی چند جفت‌زبان همزمان اضافه شود (Future)، این فیلتر فعال می‌شود.
- **تصمیم آگاهانه دوم:** بر خلاف سکوت الگوریتم اصلی درباره‌ی نبود DifficultyState، این پیاده‌سازی طبق اصل کلی پروژه (Active Concept بدون DifficultyState = DATA_INTEGRITY_ERROR) در این حالت Exception پرتاب می‌کند، نه فیلتر خاموش.
- `domain/usecase/ReviewSessionUseCases.kt` — `StartReviewSessionUseCase`، `EndReviewSessionUseCase` (خطا اگر sessionId ناموجود باشد).
- `FakeRepositories.kt` گسترش یافت: `FakeReviewSessionRepository`.
- تست‌ها: `SelectReviewQueueUseCaseTest` (۱۰ مورد: ترتیب DAILY، رفتار مستقل LEARNED، محدوده RANDOM، هر فیلتر به‌تنهایی + ترکیبی، soft-delete، DATA_INTEGRITY_ERROR، خروجی خالی)، `ReviewSessionUseCasesTest` (۳ مورد).

## جزئیات فاز ۱۳ (تکمیل‌شده)

- Entity/DAO/Repository Category از قبل (فازهای ۶/۷/۹) کامل بودند؛ این فاز فقط لایه UseCase را اضافه کرد.
- `domain/usecase/CategoryUseCases.kt` — `CreateCategoryUseCase` (trim + رد نام خالی)، `UpdateCategoryUseCase` (rename، خطا اگر id ناموجود)، `GetAllCategoriesUseCase` (مرتب بر اساس نام)، `GetCategoryByIdUseCase`.
- Exception جدید: `InvalidCategoryInputException`.
- `FakeRepositories.kt` گسترش یافت: `FakeCategoryRepository`.
- تست‌ها (`CategoryUseCasesTest`، ۶ مورد).

**تصمیم باز (عمداً پیاده‌سازی نشد):** حذف Category. نه Soft-delete بودنش طبق اصل کلی بخش ۲ (که صراحتاً فقط درباره Concept صحبت می‌کند) و نه رفتار Conceptهای متصل به یک Category حذف‌شده (null شدن categoryId یا جلوگیری از حذف) در هیچ‌جای Descriptions/Algorithms v4.20 مشخص نشده. باید قبل از پیاده‌سازی صریحاً بحث و تصمیم‌گیری شود (طبق روش کاری discuss→conclude→apply). فعلاً هیچ DeleteCategoryUseCase وجود ندارد.

## جزئیات فاز ۱۴ (تکمیل‌شده)

- `domain/usecase/GenerateQuizQuestionUseCase.kt` — پیاده‌سازی کامل الگوریتم Quiz Question Generation (Algorithms v4.20 §12.4): کاملاً Read-only، خروجی `QuizGenerationResult` (`QuizQuestion` یا `FlashcardFallback`).
  - مرحله ۱: پیدا کردن promptContent (sourceLanguage) و correctContent (targetLanguage) روی Concept؛ نبود هرکدام → Fallback فوری.
  - مرحله ۲ (Pool سازی Distractor، سه پاس تجمعی بدون حذف پاس قبلی): ۲.۱) هم‌سطح Difficulty فعلی ۲.۲) + یک سطح بالاتر/پایین‌تر (در صورت وجود؛ حدود EASY/VERY_HARD رعایت می‌شود) ۲.۳) + کل بانک بدون فیلتر Difficulty. یکتاسازی هر پاس با `normalize(text)` (بازاستفاده از `computeCanonicalKey`).
  - قواعد قطعی رعایت‌شده: Distractor هرگز از همان Concept نیست (فیلتر `conceptId != concept.id` قبل از هر فیلتر دیگر)، هرگز هم‌ارز نرمال‌شده با پاسخ صحیح نیست، Concept غیرفعال (Soft-deleted) نادیده گرفته می‌شود، کمتر از ۳ Distractor معتبر حتی با کل بانک → `FlashcardFallback`، خروجی موفق دقیقاً ۴ گزینه Shuffle‌شده دارد.
- **افزونه‌ی زیرساختی لازم:** `ContentRepository.getAllByLanguageCode(languageCode)` اضافه شد (+ `ContentDao` Query متناظر، `ContentRepositoryImpl`، `FakeContentRepository`) — چون این الگوریتم برخلاف بقیه UseCaseهای قبلی، نیاز به جست‌وجوی Content در سطح کل دیتابیس (نه فقط یک Concept) دارد.
- **تصمیم آگاهانه:** سند صراحتاً نمی‌گوید Concept صاحب Distractor باید Active باشد، اما طبق اصل کلی پروژه (Soft-delete یعنی نامرئی‌بودن در مسیرهای عادی) از `conceptRepository.getById` استفاده شد که فقط Concept فعال برمی‌گرداند؛ در نتیجه Contentهای متعلق به Conceptهای Soft-delete شده خودکار از Pool کنار می‌روند.
- **تصمیم آگاهانه دوم:** فیلتر `activeLanguagePair` جدا از بررسی صریح languagePairId (که در V1 روی Concept/Content وجود ندارد، طبق تصمیم بخش ۱۹.۱) عملاً از طریق پارامترهای `sourceLanguage`/`targetLanguage` خود Pair اعمال می‌شود — چون V1 فقط یک جفت‌زبان فعال دارد، این با قرارداد الگوریتم («متعلق به همان activeLanguagePair باشد») سازگار است بدون نیاز به فیلد اضافه روی مدل.
- تست‌ها (`GenerateQuizQuestionUseCaseTest`، ۹ مورد): نبود promptContent/correctContent، Fallback با کل بانک ناکافی، ساخت سوال معتبر ۴گزینه‌ای، Widen شدن به سطوح مجاور (و عدم ورود سطح غیرمجاور)، Fallback به کل بانک وقتی سطوح مجاور هم کافی نیست، عدم استفاده از ترجمه دوم همان Concept به‌عنوان Distractor، حذف Distractor هم‌ارزِ نرمال‌شده با پاسخ صحیح، حذف Distractorهای متعلق به Concept غیرفعال.
- **عمداً اضافه نشده:** Quiz Difficulty (سختی خودِ گزینه‌های Quiz، مستقل از Vocabulary Difficulty) — طبق Backlog Item بدون Pseudocode تأییدشده (Appendix O.3)؛ همان‌طور که در فاز ۲ ثبت شده، تا تصمیم صریح در این مورد گرفته نشود اضافه نمی‌شود.

## جزئیات فاز ۱۵ (تکمیل‌شده)

چهار UseCase طبق Algorithms v4.20 §۱۱ (Statistics/Progress/Streak/Gamification)، همگی Read-only:

- `usecase/StatisticsUseCases.kt` — `GetBasicStatisticsUseCase` (§۱۱.۱): totalActiveWords/practicedWords/unpracticedWords/learnedWords، همه فقط روی Conceptهای Active محاسبه می‌شوند.
- `usecase/ProgressUseCases.kt` — دو UseCase مستقل از هم:
  - `GetProgressSummaryUseCase` → مدل از‌قبل‌موجود `ProgressSummary` (activeConceptCount/learnedCount/dueCount/totalCorrect/totalWrong/accuracyPercentage) را پر می‌کند؛ این از سند Phase 6 Descriptions (v4.10) می‌آید، نه از Algorithms §۱۱.۲.
  - `CalculateProgressPercentageUseCase` (§۱۱.۲) — فرمول امتیازدهی مرحله‌ای (LEARNED=۱۰۰، MONTHLY=۸۰، WEEKLY=۶۰، DAILY=۳۵، تمرین‌شده‌بدون‌State=۱۵، غیره=۰) روی میانگین کل Conceptهای Active. این دو UseCase دو مفهوم متفاوت‌اند (خلاصه‌ی صفحه Progress در برابر درصد پیشرفت کلی) و مکمل هم هستند، نه رقیب.
- `usecase/StreakUseCase.kt` — `CalculateStreakUseCase` (§۱۱.۳): روزهای تقویمی محلی یکتا از ReviewHistory، قانون ملایم («اگر امروز هنوز Review نداشته ولی دیروز داشته، Streak نمی‌شکند»)، `now`/`zoneId` از بیرون تزریق می‌شوند (طبق الگوی بقیه الگوریتم‌های این ماژول، خواندن مستقیم ساعت سیستم انجام نمی‌شود).
- `usecase/AchievementUseCases.kt` — `CheckAndUnlockAchievementsUseCase` (§۱۱.۴): هر ۷ Achievement دقیقاً طبق جدول سند (بدون اختراع threshold جدید)؛ `GetBasicStatisticsUseCase` و `CalculateStreakUseCase` را Compose می‌کند و برای HARD_MODE_MASTER/LONG_TERM_MEMORY مستقیماً از DifficultyState/ReviewHistory می‌خواند. فقط Achievementهایی که **در همین اجرا** برای اولین‌بار Unlock شده‌اند برگردانده می‌شوند (idempotent روی اجراهای بعدی).
- **افزونه‌ی زیرساختی لازم:** `LearningStateRepository.getAll()` و `ReviewHistoryRepository.getAll()` اضافه شدند (+ DAO Query متناظر، Impl، Fake) — این چهار UseCase برخلاف UseCaseهای قبلی نیاز به اسکن کامل دارند، نه فقط Query روی یک Concept/Stage خاص.
- **تصمیم آگاهانه:** در `CalculateProgressPercentageUseCase`، شاخه‌ی «LearningState وجود ندارد» دقیقاً طبق متن صریح الگوریتم (امتیاز ۱۵ یا ۰ بر اساس وجود ReviewHistory) پیاده شد، نه با پرتاب `DataIntegrityException` مثل UseCaseهای دیگر (SelectReviewQueue/SubmitReviewAnswer) — چون این رفتار مشخص برای همین الگوریتم خاص در سند تصریح شده و یک صفحه Statistics/Progress نباید با یک رکورد ناقص کرش کند.
- `FakeAchievementRepository` به `FakeRepositories.kt` اضافه شد.
- تست‌ها: `GetBasicStatisticsUseCaseTest` (۳)، `ProgressUseCasesTest` (۶)، `CalculateStreakUseCaseTest` (۶)، `CheckAndUnlockAchievementsUseCaseTest` (۹) — جمعاً ۲۴ مورد، شامل هر ۷ Achievement، قانون ملایم Streak، حذف Soft-deleted از همه‌ی متریک‌ها، و شمارش بر اساس Concept یکتا (نه تعداد Review) برای LONG_TERM_MEMORY.

## جزئیات فاز ۱۶ (تکمیل‌شده)

طبق تراکر، این فاز فقط «مدل + Validator» است؛ اجرای واقعی CreateBackup/RestoreBackup (شامل مراحل ۳.۱ تا ۳.۱۳ الگوریتم، Transaction، Safety Backup و ...) عمداً به فاز ۱۷ موکول شد.

- `domain/model/ExportData.kt` — کانتینر داده‌ی Backup/Restore (Algorithms v4.20، بخش «Backup & Restore»): تمام جداول Vocabulary + Progress را به‌صورت لیست نگه می‌دارد؛ `conceptReferences` به‌صورت **محاسبه‌شونده** (نه فیلد ذخیره‌شده) از روی `reviewHistory`/`learningStates`/`difficultyStates` ساخته می‌شود تا هرگز Out-of-sync نشود.
- `domain/model/Enums.kt` — افزودن `BackupType { VOCABULARY, PROGRESS, FULL }`.
- `domain/algorithm/BackupValidationAlgorithm.kt` — تابع Pure `validateBackup(data, currentSchemaVersion)` → `BackupValidationResult` (`Valid` یا `Invalid(errors: List<String>)`). چون این فاز فقط Validator است، تابع هیچ Repository‌ای صدا نمی‌زند و فقط خودِ `ExportData` را بررسی می‌کند؛ اعتبارسنجی در برابر دیتابیس مقصد واقعی (مثلاً چک کردن اینکه Conceptهای ارجاع‌شده در یک Backup نوع PROGRESS واقعاً در مقصد وجود دارند) کار RestoreBackup در فاز ۱۷ است، نه این Validator.
- **تصمیم آگاهانه‌ی مهم (ساده‌سازی معماری):** سند اصلی بین «UUID» و «Database ID داخلی» تمایز می‌گذارد و یک نگاشت `conceptIdMap`/`languageIdMap`/... بین آن دو در Restore پیشنهاد می‌دهد. در این پروژه چون همه‌ی Entityها از ابتدا (فاز ۳) روی `@PrimaryKey val id: UUID` ساخته شده‌اند، اصلاً ID داخلی جدا از UUID وجود ندارد — بنابراین این نگاشت یک No-op است و در فاز ۱۷ فقط کافی است با همان `id` مستقیماً رکورد مقصد را جست‌وجو کرد. این ساده‌سازی معماری است، نه انحراف از هدف سند (که همان «شناسه پایدار بین دیتابیس‌ها» است و همان‌طور که هست توسط `id` تأمین می‌شود).
- **تصمیم آگاهانه‌ی دوم:** سند می‌گوید ConceptReferences از «ReviewSessions, ReviewHistory, LearningStates» ساخته می‌شود، اما مدل `ReviewSession` در این پروژه اصلاً فیلد `conceptId` ندارد (یک Session شامل چند Concept است، فقط از طریق `ReviewHistory.sessionId` مرتبط می‌شود) — بنابراین `conceptReferences` فقط از `reviewHistory` + `learningStates` + `difficultyStates` محاسبه می‌شود؛ این ناسازگاری جزئی در سند در کامنت کد مستند شده.
- ۸ چک الگوریتم `ValidateBackup` (بخش ۹، «مرحله ۱») به این صورت پوشش داده شدند: چک‌های ۱/۳/۴ («داده وجود دارد»/«ساختار کامل است»/«UUIDها معتبرند») توسط type-system کاتلین به‌صورت خودکار تضمین می‌شوند (چون `ExportData` یک data class تایپ‌شده با فیلدهای `UUID` غیرقابل‌null است) و نیازی به کد اضافه ندارند؛ چک‌های ۲ (بازه schemaVersion)، ۵ (عدم تکرار کلید در هر جدول)، ۶ (ارجاع‌های داخلی بین جداول همان Backup — با استثنای صریح ارجاع Concept در Backup نوع PROGRESS که خارجی است)، ۷ (خالی‌نبودن فیلدهای اجباری) و ۸ (تطابق جدول‌های پرشده با نوع Backup) پیاده‌سازی شدند.
- تست‌ها (`BackupValidationAlgorithmTest`، ۱۸ مورد): هر سه نوع Backup معتبر، خارج از بازه schemaVersion، ناسازگاری Type/جدول‌های پرشده، تکرار کلید (Concept.id، Content.(conceptId,languageCode)، LearningState.conceptId)، ارجاع نامعتبر (Content→Concept، ConceptTag→Tag، LanguagePair→Language، ReviewHistory→Session، LearningState→Concept فقط در FULL)، عدم گزارش اشتباهِ ارجاع خارجی مجاز در PROGRESS، فیلد خالی، تجمعِ چند خطا هم‌زمان، و صحت محاسبه‌ی `conceptReferences`.

## جزئیات فاز ۱۷ (تکمیل‌شده)

- **افزونه‌ی زیرساختی (۶ متد `getAll()`):** `ConceptRepository`، `ContentRepository`، `ConceptTagRepository`، `ReviewSessionRepository`، `DifficultyStateRepository`، `SettingsRepository` — همگی + DAO Query متناظر + Impl + Fake + (برای Concept: `getAll()` شامل رکوردهای Soft-deleted هم می‌شود، برخلاف `getAllActive()`). همچنین `FakeLanguageRepository`، `FakeLanguagePairRepository`، `FakeTagRepository` که قبلاً اصلاً وجود نداشتند به `FakeRepositories.kt` اضافه شدند.
- `usecase/CreateBackupUseCase.kt` — فقط ساخت شیء `ExportData` در حافظه از روی وضعیت فعلی Repositoryها؛ VOCABULARY فقط جداول Vocabulary را پر می‌کند، PROGRESS فقط Progress را، FULL هر دو را (و شامل Conceptهای Soft-deleted هم می‌شود، چون یک Backup کامل باید بی‌کم‌وکاست باشد).
- `usecase/RestoreBackupUseCase.kt` — دقیقاً طبق ترتیب سند (Languages→Categories→Tags→LanguagePairs→Concepts→Contents→ConceptTags→ReviewSessions→ReviewHistory→LearningStates→DifficultyStates→Settings→Achievements)، همه داخل یک `FlashLearnDatabase.withTransaction`. خروجی: `RestoreResult` (`Success(newCount,mergedCount)` / `AbortedByUser` / `Error(message)`).
- **تصمیم آگاهانه‌ی مهم (I/O خارج از دامنه):** «Safety Backup قبل از Restore» طبق سند نیازمند ذخیره‌کردن یک فایل امن و پرسیدن از کاربر در صورت شکست است — هیچ‌کدام کار لایه Domain نیست. `RestoreBackupUseCase` این تصمیم را با دو پارامتر تابعی می‌گیرد: `persistSafetyBackup: suspend (ExportData) -> Boolean` و `confirmProceedWithoutSafetyBackup: suspend () -> Boolean`؛ منطق تصمیم‌گیری (بساز → تلاش برای ذخیره → اگر شکست خورد بپرس → لغو یا ادامه) همچنان داخل Domain است، فقط مکانیزم I/O و UI به لایه‌های بیرونی (فازی هنوز شماره‌گذاری‌نشده در تراکر) موکول شده.
- **تصمیم آگاهانه (ReviewHistory در Restore):** سند برای Merge رکورد تکراری ReviewHistory دستور «Update» می‌دهد، اما `ReviewHistoryRepository` از فاز ۹ عمداً هیچ متد Update ندارد (Append-only). چون فیلدهای ReviewHistory هرگز نباید بعد از ثبت تغییر کنند، رکورد یافت‌شده با همان id عملاً «همان رویداد قبلاً ثبت‌شده» است — پس این حالت به‌عنوان یک Skip بی‌اثر پیاده شد (نه new، نه merged)، دقیقاً مثل حالت «متن یکسان» در Merge محتوا؛ قرارداد Append-only فاز ۹ حفظ شد.
- **تصمیم آگاهانه (سادگی ناشی از فاز ۱۶):** چون `id` همان شناسه پایدار بین Backup/Restore است (نه یک ID داخلی جدا)، Restore برای اکثر جداول فقط «پیدا کن با همان id → اگر بود Update وگرنه Insert» است؛ فقط Content (طبق Appendix M: اول UUID سپس conceptId+languageCode) و LearningState/DifficultyState (کلید Merge واقعی‌شان `conceptId` است، نه `id`) رفتار Merge دقیق‌تری دارند.
- **قانون ۵ (Progress Backup) پیاده‌شده:** برای یک Backup نوع PROGRESS، اگر Concept ارجاع‌داده‌شده توسط LearningState/DifficultyState/ReviewHistory در دیتابیس مقصد وجود نداشته باشد (چون در خودِ این Backup موجود نیست — طبق `conceptReferences`)، آن رکورد بی‌صدا Skip می‌شود، نه Error.
- تست‌ها: `CreateBackupUseCaseTest` (۴ مورد) و `RestoreBackupUseCaseTest` (۱۲ مورد) — شامل Backup نامعتبر، تازه/تکراری بودن Restore (new در برابر merged)، سناریوی Safety Backup (شکست+لغو کاربر / شکست+تأیید کاربر)، Skip شدن رکورد Progress با Concept غائب، بازیابی رکورد Progress وقتی Concept از قبل موجود است، هر سه حالت Merge محتوا (شناسه یکسان، متن یکسان با شناسه متفاوت، متن متفاوت با شناسه متفاوت)، شمارش صحیح ConceptTag Idempotent، و تبدیل Exception به `Error` به‌جای Propagate شدن.
- **محدودیت شناخته‌شده (ثبت‌شده برای بعد):** `FakeFlashLearnDatabase.withTransaction` واقعاً Rollback نمی‌کند (طبق KDoc خودش از فاز ۹)؛ بنابراین رفتار واقعی Rollback در برابر خطای وسط Transaction فقط با یک Room Instrumentation Test واقعی (نه Fake) قابل تأیید قطعی است — دقیقاً همان محدودیتی که فاز ۴ هم برای SubmitReviewAnswer ثبت کرده بود.

## جزئیات فاز ۱۸ (تکمیل‌شده)

- `domain/parser/VocabularyParser.kt` — پیاده‌سازی State Machine طبق Algorithms v4.20، **فقط محدوده P0** (§93): Normalization، Language Detection، Line Classification، Entry Boundary، Translation Detection، Breakdown Detection، Note Detection، Duplicate Detection. خروجی: `ParseResult(entries, comments, orphanLines, duplicates)`.
- **عمداً پیاده‌سازی نشده (طبق تقسیم‌بندی P1/P2 خودِ سند در §93):** Confidence/Evidence، Gender Variants (مثل `el científico; la científica` یک sourceText واحد باقی می‌ماند، Variant جدا نمی‌شود)، Entry Type Detection (خروجی هیچ فیلد entryType ندارد؛ Caller باید پیش‌فرض مثل `EntryType.WORD` بگذارد)، Relationship System (DERIVED_FROM و ...)، Possible Correction، Import Report/Manual Review ساختاریافته. خطوط DERIVATIVE/RELATION صرفاً به Note/Orphan عمومی می‌افتند، نه یک گراف رابطه.
- **تصمیم‌های آگاهانه (تفسیر مهندسی جاهایی که سند فقط Signal/مثال می‌دهد نه فرمول دقیق):**
  - تشخیص زبان: ترکیبی از حروف فارسی، Accentهای اسپانیایی/¿¡، فهرست کوتاهی از Function Wordهای اسپانیایی، و fallback به «هر حرف لاتین» (چون لیست Function Word به‌تنهایی برای Test Case 10 با کلمه اشتباه‌نوشته‌شده «melhor» کافی نیست).
  - تشخیص COMMENT: یک خط فارسیِ خالص با ≥۶ کلمه که هیچ Entry بازی منتظرش نیست (طبق مثال Test Case 8)؛ این یک آستانه ابداعی است، نه یک فرمول مستند.
  - خط «Spanish: Persian» (§۲۲/۲۶): اگر یک Entry با ترجمه‌ی کامل باز باشد → Breakdown همان Entry؛ در غیر این صورت → یک Entry کامل جدید (Hâlat B). این دقیقاً منطق Test Case 6 (Breakdown وقتی Context موجود است) را با Hâlat B بخش ۲۲ (وقتی Context موجود نیست) آشتی می‌دهد.
  - Test Case 7 (شکاف شماره‌گذاری): چون در متن نمونه‌ی سند ترجمه‌ای برای هیچ‌کدام از سه ورودی داده نشده، خروجی واقعی این پیاده‌سازی هر سه را به‌عنوان `ORPHAN_SOURCE` (نه `entries`) برمی‌گرداند؛ نکته‌ی اصلی تست (سه Boundary مجزا، بدون ساختن Entryهای ۲۵ تا ۲۸) حفظ شده، فقط bucket خروجی (orphan به‌جای entries) چون ورودی نمونه ترجمه نداشت متفاوت است.
  - Duplicate Detection: فقط دو نوع با تعریف قطعی سند پیاده شدند: `EXACT_DUPLICATE` و `SAME_SOURCE_DIFFERENT_TRANSLATION`؛ `POSSIBLE_DUPLICATE` و `RELATED_FORM` نیاز به Fuzzy Matching دارند و طبق تقسیم‌بندی P1 موکول شدند.
- **هنوز وصل نشده به بقیه‌ی اپ:** این فاز فقط خودِ الگوریتم Parser را می‌سازد؛ اتصال آن به یک `ParseVocabularyTextUseCase` که خروجی را از طریق `ResolveConceptForParsedEntry` (که در Algorithms موجود است ولی هنوز در این پروژه UseCase نشده) و `CreateConceptUseCase` به دیتابیس بنویسد، جزو فاز ۲۴ (AddWord Screen + Parser) است، نه این فاز.
- تست‌ها (`VocabularyParserTest`، ۱۶ مورد): Test Caseهای ۱، ۳، ۴، ۵، ۶، ۷، ۸، ۹، ۱۰ سند + پوشش صریح §۲۱ (Entry چندخطی)، §۲۲ Hâlat B، §۲۳ (فارسی قبل از اسپانیایی)، هر دو نوع Duplicate + حالت بدون Duplicate، و نویز شماره‌گذاری/خط خالی.

## جزئیات فاز ۱۹ (تکمیل‌شده)

- `domain/usecase/RefreshDataUseCase.kt` — پیاده‌سازی کامل §۷.X (Refresh/Data Migration Algorithm): برای هر Concept فعال + هر Content متعلق به آن، به‌صورت پله‌پله (`dataVersion+1` در هر مرحله) Migrationهای ثبت‌شده را اعمال می‌کند تا به نسخه فعلی برسد؛ Concept و Content کاملاً مستقل از هم پیش می‌روند (طبق مثال سند: Concept.dataVersion=4 و Content.dataVersion=2 با CURRENT متفاوت). خروجی: `Success(updatedCount)` / `NoChange` / `Error(message)`.
- کل عملیات داخل یک `FlashLearnDatabase.withTransaction` است؛ Migration گم‌شده به‌جای بازگشت مستقیم Error، یک Exception داخلی پرتاب می‌کند تا Room واقعاً کل این اجرا را Rollback کند (نه اینکه فقط پیام خطا برگردد ولی رکوردهای قبلی همان اجرا نیمه‌Migrate بمانند) — همان الگویی که در فاز ۱۷ برای RestoreBackup هم استفاده شد.
- **تصمیم آگاهانه (پارامتری‌کردن به‌جای ثابت‌های کامپایل‌تایم):** سند `CURRENT_CONCEPT_VERSION`/`CURRENT_CONTENT_VERSION` و لیست Migrationها را «ثابت سراسری» توصیف می‌کند، اما اینجا به‌صورت پارامتر Constructor (با مقدار پیش‌فرض ۰ / نگاشت خالی) پیاده شدند، نه `const val` هاردکد — چون: (۱) هنوز هیچ تغییر داده‌ای در V1 شِیپ نشده، پس عملاً مقدار فعلی همان ۰/خالی است؛ (۲) این‌طوری در فاز ۲۱ (Hilt) به‌سادگی با `@Provides` جایگزین می‌شوند وقتی نسخه بعدی برنامه واقعاً یک Migration اضافه کرد، بدون دست‌زدن به منطق خودِ UseCase؛ (۳) تست‌پذیری کامل بدون نیاز به تغییر کد اصلی برای شبیه‌سازی سناریوهای چندمرحله‌ای/گم‌شدن Migration.
- محدوده نوشتن داده دقیقاً طبق سند: فقط Concept و Content؛ این UseCase اصلاً به `LearningStateRepository`/`ReviewHistoryRepository` وابسته نیست (حتی به‌صورت ساختاری امکان دست‌زدن به آن‌ها را ندارد).
- تست‌ها (`RefreshDataUseCaseTest`، ۸ مورد): بدون Migration ثبت‌شده (NoChange)، Migration یک‌مرحله‌ای Concept، Migration یک‌مرحله‌ای Content، چند‌مرحله‌ای به‌ترتیب صحیح (۱→۲→۳)، Idempotency در اجرای دوم، استقلال کامل نسخه Concept/Content از هم، Migration گم‌شده → Error، و نادیده‌گرفتن Conceptهای Soft-deleted.

## جزئیات فاز ۲۰ (تکمیل‌شده)

- `usecase/SettingsUseCases.kt` — پنج UseCase:
  - `GetThresholdDifficultyUseCase` — فقط خواندنی (Descriptions §۶.۳/D: در V1 بدون UI برای تغییر)؛ عمداً هیچ Set-UseCase ندارد. `SubmitReviewAnswerUseCase` طبق فاز ۱۱ مستقیماً از Repository می‌خواند (دست‌نخورده ماند، چون رفتارش از قبل صحیح بود)؛ این UseCase برای مصرف‌کننده‌های آینده (مثلاً صفحه Settings فاز ۲۸) است تا قانون «بدون UI» فقط در یک‌جا اعمال بماند.
  - `GetMaxReviewCardsUseCase` / `SetMaxReviewCardsUseCase` — سقف تعداد کارت هر Review Session (Descriptions §۶/Backlog F). فقط اندازه Session را محدود می‌کند، نه Eligibility.
  - `GetThemeUseCase` / `SetThemeUseCase` — با Enum جدید `AppTheme { LIGHT, DARK, SYSTEM }`.
- **افزونه‌ی زیرساختی:** `SettingsRepository.delete(key)` اضافه شد (+ DAO/Impl/Fake) — چون نمایندگی «هیچ مقداری تنظیم نشده» (مثلاً برداشتن سقف Max Review Cards) نیاز به حذف واقعی کلید دارد، نه یک مقدار Sentinel.
- **تصمیم‌های آگاهانه (مقادیر پیش‌فرض اختراعی، چون سند عددی/رشته‌ای مشخص نداده):**
  - `max_review_cards`: پیش‌فرض = بدون سقف (`null`)، نه یک عدد ثابت اختراعی؛ کم‌تهاجم‌ترین تفسیر ممکن.
  - `theme`: پیش‌فرض = `SYSTEM` (پیروی از تنظیم دستگاه)، چون سند فقط به «پشتیبانی کامل Light/Dark» اشاره کرده، نه پیش‌فرض مشخص.
- **عمداً پیاده‌سازی نشده:** `backup_encryption_enabled`. با بازخوانی دقیق‌تر سند مشخص شد کل زیربخش «رمزنگاری Backup» (§۱۶.۲) عملاً زیرمجموعه‌ی «توسعه آینده»ی §۱۹ است (حتی این جمله که کاربر می‌تواند آن را در Settings فعال کند هم در همان زیربخش Future آمده)؛ پس طبق همان انضباط MVP که برای Daily New Word Limit و چند‌جفت‌زبانی رعایت شده، فعلاً UseCase‌ای برایش ساخته نشد — فقط کلید و توضیحش در `AppSetting.kt`/`SettingKeys` (از فازهای قبل) باقی می‌ماند.
- تست‌ها (`SettingsUseCasesTest`، ۸ مورد): پیش‌فرض هر سه Setting، Round-trip خواندن/نوشتن هرکدام، رد شدن مقدار غیرمثبت برای Max Review Cards، و پاک‌شدن واقعی سقف با `delete`.

## جزئیات فاز ۲۱ (تکمیل‌شده)

- `data/di/DatabaseModule.kt` — می‌سازد singleton `FlashLearnRoomDatabase` را (`Room.databaseBuilder` + `ALL_MIGRATIONS` از فاز ۳/۶ + `FlashLearnDatabaseCallback` برای Index شرطی LanguagePair فعال)، و هر ۱۳ DAO را از همان یک نمونه Expose می‌کند.
- `data/di/RepositoryModule.kt` — هر ۱۴ Interface دامنه (`ConceptRepository` تا `FlashLearnDatabase`) را با `@Binds` به Implementation واقعی‌اش در `:data` وصل می‌کند. هیچ UseCase از فازهای ۲ تا ۲۰ برای این کار تغییر نکرد — همه از قبل فقط به Interfaceهای دامنه وابسته بودند؛ این فاز صرفاً همان Interfaceها را در Runtime قابل‌حل کرد.
- `data/di/RefreshDataModule.kt` — چهار پارامتر Qualified مورد نیاز `RefreshDataUseCase` (نسخه/Migration فعلی Concept و Content) را Provide می‌کند؛ هر دو نسخه ۰ و هر دو Map خالی، چون هنوز هیچ تغییر داده‌ای Ship نشده (طبق تصمیم فاز ۱۹).
- **تغییر در فایل فاز قبل (توضیح‌داده‌شده قبل از اجرا):** به ۴ پارامتر Constructor مربوط به `RefreshDataUseCase` چهار Qualifier annotation جدید (`@CurrentConceptVersion`, `@CurrentContentVersion`, `@ConceptMigrations`, `@ContentMigrations`) اضافه شد (فایل جدید `usecase/RefreshDataQualifiers.kt`) — چون Hilt نمی‌تواند دو `Int` یا دو `Map<Int,(X)->X>` بدون Qualifier را از هم تشخیص دهد. این Qualifierها فقط از `javax.inject.Qualifier` استفاده می‌کنند (نه Dagger مستقیم)، پس قانون «domain بدون وابستگی Android/Hilt» نقض نشد. تست‌های موجود `RefreshDataUseCaseTest` بدون تغییر پاس می‌مانند چون annotation روی پارامتر تأثیری در فراخوانی مستقیم Kotlin ندارد.
- **محدودیت شناخته‌شده (قابل‌تأیید فقط با Build واقعی):** درستی نهایی این Wiring (این‌که آیا Dagger واقعاً کل گراف را Compile می‌کند) فقط با اجرای واقعی KSP/Hilt روی یک پروژه‌ی Android کامل قابل تأیید است، نه با تست واحد JVM؛ این دقیقاً همان محدودیتی است که فازهای قبل هم (مثلاً فاز ۴ برای Transaction واقعی) صراحتاً ثبت کرده‌اند. بازبینی دستی کامل انجام شد: هر ۱۴ Interface Bind شده، هر Constructor UseCase در کل `domain/usecase/` فقط به Interfaceهای Bind‌شده یا UseCaseهای دیگر یا پارامترهای Qualified وابسته است.

## جزئیات فاز ۲۲ (تکمیل‌شده)

اولین فاز لایه UI/Presentation — قبل از این، پروژه فقط `app/core/domain/database/data` بود؛ چون سند `presentation`/`ui`/`navigation` را لایه‌های منطقی توصیف کرده نه لزوماً ماژول‌های Gradle جدا، و `settings.gradle.kts` از فاز ۱ فقط ۵ ماژول دارد، این سه لایه به‌صورت Package داخل همان ماژول `app` پیاده شدند (`app.navigation`, `app.presentation`, `app.ui.theme`, `app.ui.screens`) — یک تصمیم معماری آگاهانه، نه انحراف از سند.

- `navigation/Routes.kt` — ۵ مسیر ثابت: `HOME/REVIEW/ADD_WORD/PROGRESS/SETTINGS` (چهارتای اول Primary طبق سند Phase 8؛ AddWord مسیر ثانویه از Home).
- `presentation/AppUiState.kt` + `presentation/AppViewModel.kt` — دقیقاً طبق سند: فقط `currentRoute` را نگه می‌دارد، هیچ منطق تجاری/UseCase صدا نمی‌زند.
- `navigation/FlashLearnNavGraph.kt` — `NavHost` واقعی که هر Route را به Composable مربوطه وصل می‌کند و با `LaunchedEffect` به `AppViewModel` اطلاع می‌دهد کدام Route فعال شده.
- `ui/screens/PlaceholderScreens.kt` — پنج صفحه جایگزین موقت (هرکدام فقط یک Text با شماره فازی که در آن جایگزین واقعی می‌شود) — خودِ NavGraph در فازهای ۲۳ تا ۲۸ تغییر نمی‌کند، فقط این Composableها با پیاده‌سازی واقعی عوض می‌شوند.
- `ui/theme/Color.kt` + `ui/theme/Theme.kt` — پالت رنگی دقیقاً از روی مقادیر Hex سند (Descriptions §۱۲.۱/۱۲.۲: General UI روی Light/Dark جدا، Difficulty/Stage به‌عنوان لایه مکمل).
- `MainActivity.kt` بازنویسی شد تا واقعاً `FlashLearnTheme { FlashLearnNavGraph() }` را نشان دهد (به‌جای Placeholder فاز ۱ که خودش صراحتاً نوشته بود «Real navigation در فاز ۲۲ وصل می‌شود»).
- **تصمیم آگاهانه (Theme واقعی کاربر به بعد موکول شد):** این فاز فقط از `isSystemInDarkTheme()` پیروی می‌کند؛ اتصال ترجیح صریح کاربر (`GetThemeUseCase` از فاز ۲۰: LIGHT/DARK/SYSTEM) به فاز ۲۸ (صفحه Settings) موکول شد، چون آن نیاز به یک خواندن Async قبل از اولین Frame دارد که با معماری فعلی `MainActivity` (بدون ViewModel سطح Activity برای این کار) هنوز جفت‌وجور نشده.
- `app/build.gradle.kts`: افزودن `kotlinx-coroutines-test` به `testImplementation` (برای تست `StateFlow`)؛ بقیه‌ی وابستگی‌های لازم (`navigation-compose`, `hilt-navigation-compose`, `material3`, ...) از فاز ۱ از قبل موجود بودند.
- تست‌ها با **JUnit4** نوشته شدند (نه JUnit5) چون `app/build.gradle.kts` از قبل روی JUnit4 تنظیم شده بود (متفاوت از `domain`/`database` که JUnit5 دارند) — برای هماهنگی با تنظیمات موجود همین ماژول تغییر داده نشد: `RoutesTest` (۳ مورد، دقیقاً همان «Source-level contract test» که سند Phase 8 برای مسیرهای Primary خواسته) و `AppViewModelTest` (۳ مورد).
- **محدودیت شناخته‌شده:** این تست‌ها فقط منطق JVM (Routes/ViewModel State) را تأیید می‌کنند؛ درستی واقعی ترکیب Compose+Navigation+Hilt (`hiltViewModel()`, `NavHost` واقعی) فقط با یک Instrumentation Test روی دستگاه/Emulator واقعی قابل تأیید است (مثل محدودیت مشابه فاز ۲۱ برای Dagger Graph).

## جزئیات فاز ۲۳ (تکمیل‌شده)

- `presentation/home/HomeUiState.kt` + `HomeViewModel.kt` — `HomeViewModel` فقط از ۴ UseCase موجود استفاده می‌کند (`GetBasicStatisticsUseCase`، `CalculateStreakUseCase`، `CalculateProgressPercentageUseCase`، `SelectReviewQueueUseCase`)، هیچ Repository مستقیم صدا نمی‌زند.
- `ui/screens/home/HomeScreen.kt` — صفحه واقعی: کارت خلاصه (Streak + درصد پیشرفت + یادگرفته‌شده/کل در یک کارت فشرده)، ۵ کارت شروع مرور (DAILY/WEEKLY/MONTHLY/RANDOM/LEARNED با تعداد due هرکدام)، دکمه افزودن کلمه، دکمه‌های ناوبری به Progress/Settings.
- **تصمیم آگاهانه (منبع واحد شمارش):** برای جلوگیری از دقیقاً همان کلاس باگی که قوانین UI §12.3 شماره ۱ و ۵ منع کرده‌اند (پرچم/عدد «کلمات منتظر» تکراری یا ناهم‌خوان)، تعداد هر کارت مرور مستقیماً از `SelectReviewQueueUseCase(reviewType, now).size` گرفته می‌شود — یعنی همان چیزی که صفحه Review واقعی (فاز ۲۵/۲۶) بارگذاری خواهد کرد، نه یک مسیر شمارش جداگانه. هزینه: ۵ فراخوانی کامل لیست به‌جای یک Query شمارشی؛ در مقیاس فعلی پروژه پذیرفته شد، در صورت نیاز در آینده با یک `COUNT` مستقل بهینه می‌شود.
- **تغییر در فایل فاز ۲۲ (`Routes.kt`):** الگوریتم Review Scheduling صراحتاً می‌گوید `reviewType` «از انتخاب کاربر در Home Screen دریافت می‌شود»؛ برای همین یک آرگومان Query اختیاری (`reviewType`) به مسیر Review اضافه شد (`review?reviewType={reviewType}`). رشته پایه `"review"` و تست‌های فاز ۲۲ (`RoutesTest`, `AppViewModelTest`) بدون تغییر پاس می‌مانند؛ Review Screen (فاز ۲۵/۲۶) همچنان مالک State انتخاب نوع مرور در همان صفحه است — این آرگومان فقط انتخاب اولیه را منتقل می‌کند.
- **تصمیم آگاهانه (رفرش هنگام بازگشت):** چون NavHost نمونه Composable/ViewModel صفحه Home را در پشته نگه می‌دارد، `LaunchedEffect(Unit)` بعد از بازگشت از Review دوباره اجرا نمی‌شود؛ به‌جایش `HomeScreen` با `DisposableEffect` روی `ON_RESUME` چرخه حیات، `HomeViewModel.load()` را دوباره صدا می‌زند تا تعداد due/Streak بعد از پایان یک Review Session به‌روز بمانند.
- **تصمیم آگاهانه (گرد کردن درصد پیشرفت):** طبق تصریح سند («نحوه گرد کردن فقط در لایه نمایش تعیین می‌شود»)، `CalculateProgressPercentageUseCase` دست‌نخورده (Double خام) ماند و گرد کردن به Int در تابع خالص `buildHomeUiState` (لایه Presentation) انجام شد.
- **قرارداد پکیج‌بندی جدید (از این فاز به بعد):** برخلاف فاز ۲۲ که همه‌چیز در `presentation`/`ui.screens` مسطح بود، از این فاز هر صفحه زیرپکیج خودش را دارد (`presentation.home`, `ui.screens.home`) تا با اضافه‌شدن Review/AddWord/Progress/Settings در فازهای بعد این پکیج‌ها شلوغ/مبهم نشوند. `PlaceholderScreens.kt` مشترک برای صفحات هنوز-ساخته‌نشده باقی ماند.
- `PlaceholderScreens.kt`: `HomeScreenPlaceholder` حذف شد (جایگزین با `HomeScreen` واقعی)؛ `ReviewScreenPlaceholder` یک آرگومان اختیاری `reviewType: String?` گرفت تا Nav آرگومان جدید تا فاز ۲۵/۲۶ بلااستفاده نماند.
- تست‌ها: `HomeUiStateMappingTest` (۴ مورد، روی تابع خالص `buildHomeUiState` — بدون نیاز به Coroutine/Fake)، افزودن ۳ مورد به `RoutesTest` برای `reviewRoute()`/`REVIEW_ROUTE_PATTERN`.
- **محدودیت شناخته‌شده (مثل فازهای ۲۱/۲۲):** این تست‌ها فقط منطق JVM را تأیید می‌کنند؛ `HomeViewModel.load()` (که ۵ UseCase تزریقی را واقعاً فرا می‌خواند) و ترکیب Compose+Hilt+Navigation فقط با Build/Instrumentation Test واقعی قابل تأیید نهایی است — چون این پروژه در این محیط `gradle`/Android SDK ندارد، Build واقعی اینجا اجرا نشد.

## جزئیات فاز ۲۴ (تکمیل‌شده)

حلقه‌ی گم‌شده‌ی بین Parser (فاز ۱۸) و دیتابیس ساخته شد، سپس صفحه واقعی AddWord.

- `domain/usecase/ResolveConceptForParsedEntryUseCase.kt` — پیاده‌سازی «ResolveConceptForParsedEntry» (Algorithms §۷)، **با منطق Matching طبق نسخه بعدی و FROZEN شده‌ی Appendix M.3** (`computeCanonicalKey` + `ContentRepository.findByCanonicalKey` روی ایندکس (languageCode, canonicalKey))، نه Pseudocode قدیمی‌تر §۷ خودش («normalize(text) = trim().lowercase()» به‌صورت مقایسه زنده) — چون Appendix M.3 صراحتاً می‌گوید همان Pseudocodeهای قدیمی بخش‌های ۷/۸/۱۶ را جایگزین می‌کند. کاملاً Read-only؛ هرگز خودش Concept نمی‌سازد یا Merge نمی‌کند، فقط تصمیم را برمی‌گرداند (`CreateNewConcept` / `ReuseConcept` / `Conflict`).
- `domain/usecase/ImportParsedEntryUseCase.kt` — لایه بالاتر طبق §۷ («اجرای نتیجه توسط لایه بالاتر»): `CreateNewConcept` → `CreateConceptUseCase`؛ `ReuseConcept` با Pieceهای جدید → `ContentRepository.upsert` برای هرکدام؛ `ReuseConcept` بدون Piece جدید → `AlreadyExists`؛ `Conflict` → `NeedsUserChoice` (هیچ Merge خودکاری انجام نمی‌شود).
- **تصمیم آگاهانه:** `ParsedEntry.breakdowns` و `grammarNotes` در این مرحله ذخیره نمی‌شوند — مدل دیتابیس فعلی اصلاً ستونی برای Breakdown یا یادداشت گرامری جدا ندارد (این خودش بخشی از دامنه‌ی P1 پارسر است که در فاز ۱۸ عمداً پیاده نشد). فقط `notes` پارس‌شده روی ستون `Content.notes` می‌نشیند.
- **تصمیم آگاهانه (UX تناقض):** طبق الگوریتم، `Conflict` باید یا با انتخاب کاربر بین Conceptهای موجود حل شود یا لغو شود؛ ساخت یک UI کامل «انتخاب بین چند Concept» برای این فاز پیچیدگی زیادی اضافه می‌کرد، پس فعلاً `Conflict`ها فقط در خلاصه‌ی Import به‌عنوان «نیازمند بررسی دستی» گزارش می‌شوند و import نمی‌شوند (نه Merge خودکار، نه حذف) — کاربر می‌تواند بعداً آن مورد را جدا و دستی اضافه کند. ساخت UI انتخاب کامل به یک فاز بعدی (Polish) موکول شد.
- `presentation/addword/AddWordUiState.kt` + `AddWordViewModel.kt` — دو حالت: افزودن دستی (مستقیماً `CreateConceptUseCase`) و Paste Text (`VocabularyParser.parse` → پیش‌نمایش → `ImportParsedEntryUseCase` برای هر Entry). دو تابع خالص و مستقل از Coroutine برای تست‌پذیری: `applyParseResult` و `summarizeImportResults`.
- `ui/screens/addword/AddWordScreen.kt` — دو تب (افزودن دستی / چسباندن متن)؛ در حالت Paste: فیلد چندخطی → دکمه «تحلیل متن» → خلاصه‌ی تعداد Entry/تکراری/بی‌ترجمه/توضیحی → لیست پیش‌نمایش → دکمه «وارد کردن N کلمه» → کارت خلاصه نتیجه (جدید/ادغام‌شده/از‌قبل‌موجود/تناقض).
- `navigation/FlashLearnNavGraph.kt` — مسیر `ADD_WORD` به `AddWordScreen` واقعی وصل شد؛ `PlaceholderScreens.kt`: `AddWordScreenPlaceholder` حذف شد.
- تست‌ها: `ResolveConceptForParsedEntryUseCaseTest` (۵)، `ImportParsedEntryUseCaseTest` (۵)، `AddWordViewModelMappingTest` (۴؛ روی توابع خالص، بدون Coroutine/Fake) — جمعاً ۱۴ تست جدید.
- **محدودیت شناخته‌شده (مثل فازهای ۲۱ تا ۲۳):** ترکیب واقعی Compose (Tab/LazyColumn) + Hilt + دو UseCase تزریقی فقط با Build/Instrumentation واقعی قابل تأیید کامل است؛ اینجا فقط منطق JVM (UseCaseها و توابع Mapping) تست شد.

## جزئیات فاز ۲۵ (تکمیل‌شده)

حلقه‌ی Review واقعی: Queue → Session → پاسخ اتمیک → پایان Session، همه با UseCaseهای از قبل موجود (بدون تغییر Constructor هیچ‌کدام).

- `domain/usecase/ReviewCardUseCases.kt` — `GetFlashcardContentUseCase` جدید: `Content` مبدأ/مقصد یک Concept را از `ContentRepository.getAllByConceptId` می‌گیرد و front/back/notes می‌سازد.
- **تصمیم آگاهانه (مهم — نبود LanguagePair فعال):** هیچ‌جای پروژه تا این فاز یک `LanguagePair` را در دیتابیس Seed/Activate نمی‌کند (`LanguagePairRepository.getActive()` روی نصب تازه همیشه `null` برمی‌گرداند). به‌جای وابسته‌کردن Review به این متد (که همان لحظه‌ی اول Review را می‌شکست)، `GetFlashcardContentUseCase` دقیقاً همان پیش‌فرض هاردکد `sourceLanguage="es"`/`targetLanguage="fa"` را تکرار کرد که `CreateConceptUseCase` (فاز ۱۰) از قبل دارد و خودِ تست‌های `GenerateQuizQuestionUseCase` (فاز ۱۴) هم به همین شکل مستقیم یک `LanguagePair("es","fa")` می‌سازند، نه از Repository. مدیریت واقعی LanguagePair (Seed کردن، UI انتخاب) هنوز جایی در ۳۰ فاز تعریف نشده — این یک شکاف شناخته‌شده است، نه چیزی که این فاز حل کرد.
- `presentation/review/ReviewUiState.kt` — چهار حالت: `Loading` / `Empty` (خروجی خالی Queue، طبق الگوریتم «این حالت خطا محسوب نمی‌شود») / `InProgress` (شامل `feedback: AnswerFeedback?` برای غیرفعال‌کردن دکمه‌ها بعد از پاسخ، طبق §۷.۲) / `Finished`.
- `presentation/review/ReviewViewModel.kt` — `start(reviewType)` یک‌بار Queue می‌سازد (`SelectReviewQueueUseCase` + سقف اختیاری `GetMaxReviewCardsUseCase`)، `StartReviewSessionUseCase` صدا می‌زند، و برای هر کارت: `flip()` → نمایش پشت + یادداشت؛ `answer(isCorrect)` → بلافاصله دکمه‌ها را غیرفعال می‌کند (Feedback ست می‌شود) سپس `SubmitReviewAnswerUseCase` (با `reviewAttemptId` تازه هر بار) را صدا می‌زند؛ روی پاسخ غلط ۲ ثانیه مکث قرمز قبل از رفتن به کارت بعد (Descriptions §12.4)، روی پاسخ صحیح بدون مکث. آخرین کارت → `EndReviewSessionUseCase` + حالت `Finished`.
- `ui/screens/review/ReviewScreen.kt` — اگر آرگومان Nav (`reviewTypeArg`) null یا نامعتبر بود، پیش‌فرض `ReviewType.DAILY` (تصمیم آگاهانه: انتخابگر واقعی نوع مرور طبق عنوان خودِ فاز ۲۶ به آن فاز موکول شد). فیدبک صحیح/غلط از `AppColors.LightSuccess/DarkSuccess` (سبز، دستی بر اساس `isSystemInDarkTheme()`، چون Material3 ColorScheme اسلات «success» ندارد) و `MaterialTheme.colorScheme.error` (قرمز، از قبل در Theme فاز ۲۲ سیم‌کشی شده) ساخته می‌شود.
- `navigation/FlashLearnNavGraph.kt` — مسیر `REVIEW` به `ReviewScreen` واقعی وصل شد؛ `onFinished` هر دو حالت Empty/Finished را با `navController.popBackStack()` مستقیماً به Home برمی‌گرداند (که طبق تصمیم فاز ۲۳ خودش با `ON_RESUME` تعداد due را رفرش می‌کند).
- `PlaceholderScreens.kt`: `ReviewScreenPlaceholder` حذف شد.
- تست‌ها: `GetFlashcardContentUseCaseTest` (۶ مورد، دامنه — front/back صحیح، منبع notes فقط از سمت مبدأ، دو حالت `DataIntegrityException`، override زبان سفارشی).
- **محدودیت شناخته‌شده (مثل فازهای ۲۱ تا ۲۴):** برخلاف `HomeViewModel`/`AddWordViewModel`، منطق `ReviewViewModel` عمداً به یک تابع خالص جدا تجزیه نشد — هسته‌ی آن (پیمایش ترتیبی Queue، مدیریت Session، تأخیر واقعی Coroutine) ذاتاً Stateful/Sequential است، نه یک نگاشت تک‌مرحله‌ای مثل `buildHomeUiState`. بنابراین این فاز فقط در سطح UseCase دامنه تست شد؛ صحت کامل چرخه Flip→Answer→Delay→Next→Finish و رندر واقعی Compose فقط با Instrumentation Test واقعی قابل تأیید نهایی است.

## جزئیات فاز ۲۶ (تکمیل‌شده)

Quiz mode + انتخابگر واقعی نوع/حالت مرور، روی همان `ReviewViewModel`ی که فاز ۲۵ ساخت (بدون تغییر UseCaseهای دامنه‌ی قبلی، فقط ۲ افزوده‌ی کوچک زیر).

- **تغییر UX نسبت به فاز ۲۵ (آگاهانه):** قبلاً ورود به Review بلافاصله Session را شروع می‌کرد. از این فاز، صفحه Review اول یک پنل «نوع مرور» (۵ گزینه) + «حالت مرور» (فلش‌کارت/چهارگزینه‌ای) با مقدار اولیه از آرگومان Nav نشان می‌دهد و کاربر با دکمه «شروع مرور» واقعاً وارد Session می‌شود. این دقیقاً همان چیزی است که خودِ عنوان فاز («انتخاب نوع مرور») می‌خواست؛ ۵ کارت میان‌بر Home دست‌نخورده ماندند (فقط یک تپ اضافه برای تأیید اضافه شد).
- `domain/model/Language.kt` — تابع `defaultV1LanguagePair()` اضافه شد (بدون تغییر در `data class LanguagePair`). دلیل: `GenerateQuizQuestionUseCase` به یک مقدار واقعی `LanguagePair` نیاز دارد و طبق تصمیم فاز ۲۵ («هیچ LanguagePair فعالی هرگز Seed نمی‌شود») باید همان پیش‌فرض es/fa را مستقیماً بسازیم، نه از Repository بخوانیم.
- `domain/usecase/ReviewCardUseCases.kt` — `GetDifficultyStateUseCase` اضافه شد (نامی که خودِ سند Phase 6 از قبل پیشنهاد داده بود ولی تا حالا لازم نشده بود؛ اولین مصرف‌کننده‌اش دقیقاً Quiz mode است).
- `presentation/review/ReviewUiState.kt` — بازطراحی: حالت جدید `SelectingOptions(reviewType, mode)`؛ `InProgress` حالا یک `presentation: CardPresentation` دارد (`Flashcard` یا `Quiz`) به‌جای فیلدهای صریح back/isFlipped قبلی.
- `presentation/review/ReviewViewModel.kt` — `initialize()`/`changeReviewType()`/`changeMode()` پنل انتخاب را مدیریت می‌کنند؛ `start()` یک‌بار Queue+Session می‌سازد. `showCard()`: اگر mode=QUIZ، `GetDifficultyStateUseCase` + `GenerateQuizQuestionUseCase(concept, defaultV1LanguagePair(), difficultyState)` صدا زده می‌شود؛ نتیجه `FlashcardFallback` یعنی **همان یک کارت** (نه کل Session) به نمایش Flashcard برمی‌گردد — دقیقاً طبق قرارداد الگوریتم («per-card fallback»). منطق submit مشترک بین Flashcard (`answerFlashcard`) و Quiz (`selectQuizOption`) در یک تابع خصوصی `submitAndAdvance` یکی شد تا `SubmitReviewAnswerUseCase` فقط از یک جا صدا زده شود (طبق تأکید مکرر سند: «Flashcard و Quiz هر دو از یک UseCase مشترک»).
- `ui/screens/review/ReviewScreen.kt` — پنل انتخاب با `FilterChip` (نوع مرور در یک Row قابل اسکرول افقی، حالت مرور در یک Row ساده)؛ حالت Quiz چهار دکمه‌ی گزینه نشان می‌دهد که بعد از پاسخ، گزینه‌ی درست سبز و گزینه‌ی انتخابی غلط قرمز می‌شود (برای یادگیری، نه فقط تأیید/رد ساده).
- تست‌ها: `GetDifficultyStateUseCaseTest` (۲ مورد، دامنه).
- **محدودیت شناخته‌شده (بدون تغییر از فاز ۲۵):** مسیر Quiz→Fallback→Flashcard و کل چرخه‌ی Stateful ViewModel فقط با Instrumentation واقعی قابل تأیید نهایی است؛ `GenerateQuizQuestionUseCase` خودش از فاز ۱۴ تست دامنه‌ی کامل دارد و اینجا دوباره تکرار نشد.

## جزئیات فاز ۲۷ (تکمیل‌شده)

فقط لایه UI/Presentation؛ هیچ UseCase قبلی تغییر نکرد (فقط یک UseCase جدید اضافه شد).

- `domain/usecase/AchievementUseCases.kt` — `GetAllAchievementsUseCase` جدید: لیست کامل هر ۷ `AchievementType` را با وضعیت واقعی (`isUnlocked`/`unlockedAt`) برمی‌گرداند؛ نوع‌هایی که هنوز رکوردی در `AchievementRepository` ندارند به‌صورت locked (`isUnlocked=false, unlockedAt=null`) ساخته می‌شوند. کاملاً جدا از خروجی `CheckAndUnlockAchievementsUseCase` (که فقط «تازه‌بازشده‌های همین فراخوانی» است) — این دو هرگز نباید با هم اشتباه گرفته شوند (طبق قانون UI سند: تنها newlyUnlocked برای Animation/Notification مصرف شود).
- `presentation/progress/ProgressUiState.kt` + `ProgressViewModel.kt` — دقیقاً همان الگوی `HomeViewModel` (تابع خالص `buildProgressUiState` جدا از ViewModel برای تست‌پذیری بدون Coroutine). `ProgressViewModel` پنج UseCase موجود (`GetProgressSummaryUseCase`, `GetBasicStatisticsUseCase`, `CalculateStreakUseCase`, `CalculateProgressPercentageUseCase`) + یک جدید (`GetAllAchievementsUseCase`) را ترکیب می‌کند، به‌علاوه `CheckAndUnlockAchievementsUseCase`.
- **تصمیم آگاهانه مهم (نقطه‌ی تریگر Achievement Check):** Algorithms v4.20 §11.4 صراحتاً می‌گوید `CheckAndUnlockAchievements` می‌تواند «بعد از هر پاسخ Review» یا «هنگام باز شدن صفحه Statistics» اجرا شود — این دو گزینه‌ی مستقل‌اند، نه یک الزام هردو. این فاز فقط گزینه‌ی دوم را سیم‌کشی کرد: هر بار Progress Screen باز/رفرش می‌شود. **`ReviewViewModel` (فاز ۲۵/۲۶) هنوز هیچ Achievement Check صدا نمی‌زند** — یعنی اگر کاربر هرگز به Progress نرود، دستاورد هم هرگز Unlock نمی‌شود؛ عملکردی صحیح طبق سند ولی از نظر UX ایده‌آل نیست. این یک شکاف شناخته‌شده‌ی صریح است، نه چیزی که این فاز باید حل می‌کرد (عنوان فاز فقط «Progress Screen + Achievements UI» است)؛ اگر بخواهید، افزودن این صدازدن به `ReviewViewModel` یک تغییر کوچک و مجزا در فاز بعدی است.
- نمایش «تازه‌بازشده» یک‌بار‌مصرف است: `newlyUnlockedTypes` در `ProgressUiState` فقط حاصل همان یک فراخوانی `load()` است؛ با `dismissNewlyUnlocked()` (که فقط همین فیلد را خالی می‌کند، بدون Reload) پاک می‌شود تا یک `load()` دوم (مثلاً On-Resume آینده) چیزی که قبلاً دیده شده را دوباره نشان ندهد.
- `ui/screens/progress/ProgressScreen.kt` — کارت خلاصه (Streak + نوار پیشرفت٪)، کارت آمار پایه (۸ ردیف: کل فعال/تمرین‌شده/تمرین‌نشده/یادگرفته‌شده/منتظر مرور/صحیح/غلط/دقت٪)، بنر یک‌بارمصرف «دستاورد جدید» (در صورت وجود)، و لیست کامل ۷ دستاورد (باز/قفل، با برچسب فارسیِ ترجمه‌شده از جدول §۱۱.۴ سند — دو ردیف سند که کلمه انگلیسی «Concept»/«Learned» را وسط متن فارسی گذاشته بودند این‌جا کامل ترجمه شدند، نه کپی مستقیم، چون خروجی این تابع مستقیماً روی UI کاربر نهایی نمایش داده می‌شود).
- برای دکمه بازگشت از `TextButton` استفاده شد (نه آیکون) — مطابق الگوی از‌قبل‌موجود در `AddWordScreen` (فاز ۲۴)، تا وابستگی جدیدی به ماژول آیکون‌های Compose اضافه نشود.
- `navigation/FlashLearnNavGraph.kt` — مسیر `PROGRESS` به `ProgressScreen` واقعی وصل شد؛ `PlaceholderScreens.kt`: `ProgressScreenPlaceholder` حذف شد (`SettingsScreenPlaceholder` باقی ماند، فاز ۲۸).
- تست‌ها: `GetAllAchievementsUseCaseTest` (۲ مورد، دامنه) + `ProgressUiStateMappingTest` (۵ مورد، روی تابع خالص `buildProgressUiState` — بدون Coroutine/Fake).
- **محدودیت شناخته‌شده (مثل فازهای ۲۱ تا ۲۶):** ترکیب واقعی Compose+Hilt (`hiltViewModel()`, بارگذاری واقعی ۵ UseCase تزریقی) فقط با Build/Instrumentation واقعی قابل تأیید نهایی است.

## جزئیات فاز ۲۸ (تکمیل‌شده)

### اتصال واقعی Theme (رفع شکاف فاز ۲۲)
فاز ۲۲ گفته بود اتصال ترجیح صریح کاربر به Theme یک ViewModel سطح Activity لازم دارد که هنوز نبود؛ این‌جا ساخته شد:
- `presentation/ThemeViewModel.kt` (جدید، سطح اپ نه هر صفحه) — تنها منبع حقیقت Theme در کل برنامه. عمداً در `MainActivity` و قبل از ورود به `FlashLearnNavGraph` ساخته می‌شود (نه داخل خود صفحه Settings) چون `FlashLearnTheme` در ریشه درخت Compose قرار دارد و باید به تغییر Theme واکنش نشان دهد؛ یک ViewModel با Scope مسیر Settings (که هربار ورود به آن مسیر از نو ساخته می‌شود) در آن‌جا دیده نمی‌شد.
- `currentTheme`/`onThemeChange` از `MainActivity` → `FlashLearnNavGraph` → `SettingsScreen` به‌صورت پارامتر ساده (نه UseCase مجزا) پاس داده می‌شوند؛ تا فقط **یک** مقدار در حافظه برای Theme فعلی وجود داشته باشد، نه دو نسخه که ممکن است از هم عقب بیفتند.
- `MainActivity` حالا `isSystemInDarkTheme()` را فقط برای حالت `AppTheme.SYSTEM` صدا می‌زند؛ `LIGHT`/`DARK` صریح آن را override می‌کنند.

### Settings Screen
- `presentation/settings/SettingsUiState.kt` + `SettingsViewModel.kt` — `GetThresholdDifficultyUseCase` (فقط نمایش، غیرقابل ویرایش طبق §۶.۳/D)، `GetMaxReviewCardsUseCase`/`SetMaxReviewCardsUseCase` (خالی=نامحدود).
- منطق Validation عدد Max Review Cards به تابع خالص `parseMaxReviewCards` استخراج شد (الگوی همان `applyParseResult`/`summarizeImportResults` در `AddWordViewModel` فاز ۲۴) تا بدون ViewModel/Coroutine قابل تست باشد.
- `ui/screens/settings/SettingsScreen.kt` — انتخاب Theme (۳ `FilterChip`)، نمایش Threshold، فرم Max Review Cards، لینک به «مدیریت دسته‌بندی‌ها» و «درباره برنامه».
- **شکاف صریح ثبت‌شده (Backup UI):** هیچ دکمه Backup/Restore در این صفحه نیست. سند در ۳۰ فاز اصلی هیچ فاز مجزایی برای «Backup UI» تعریف نکرده بود (فاز ۱۶/۱۷ فقط مدل+اجرای منطقی، فاز ۱۷ صراحتاً I/O واقعی فایل را به «فازی هنوز شماره‌گذاری‌نشده» موکول کرده بود) و عنوان این فاز هم آن را نام نبرده؛ بنابراین به‌جای اختراع UI برای چیزی که مسیر فایل/دسترسی storage آن هنوز تصمیم‌گیری نشده، به‌عنوان شکاف باز ثبت شد.

### Category UI
- `presentation/categories/CategoriesUiState.kt` + `CategoriesViewModel.kt` + `ui/screens/categories/CategoriesScreen.kt` — لیست، افزودن، ویرایش نام. **بدون حذف** — چون `CreateCategoryUseCase` (فاز قبلی) صراحتاً گفته بود تصمیم Soft/Hard delete و برخورد با Conceptهای وابسته هنوز باز است؛ این فاز آن تصمیم را نگرفت.
- **شکاف صریح ثبت‌شده:** انتخاب Category هنگام افزودن کلمه (`AddWordScreen`، فاز ۲۴) هنوز وایر نشده — آن فرم فیلد Category ندارد. این فاز فقط مدیریت مستقل دسته‌بندی‌ها را ساخت.

### About Screen
- `ui/screens/about/AboutScreen.kt` — محتوای ایستا مطابق §۷: توضیح برنامه، نسخه (از `BuildConfig.VERSION_NAME`، با فعال‌کردن `buildFeatures.buildConfig=true` در `app/build.gradle.kts`)، نام سازنده (طبق تصریح خود سند «نباید حدس زده شود» → placeholder صریح «هنوز مشخص نشده»، نه یک نام ساختگی)، و اشاره به `docs/PROGRESS_TRACKER.md` به‌جای یک Changelog دستی و جعلی داخل اپ.
- `versionName` در `app/build.gradle.kts` از `0.1.0-phase1` به `0.1.0-phase28` به‌روزرسانی شد (فقط رشته متادیتا؛ تغییری در قرارداد UseCase/Model نبود).

### Navigation
- `Routes.kt`: دو مسیر جدید `CATEGORIES`، `ABOUT` (مقصد ثانویه، مثل رابطه `ADD_WORD` با `HOME`)؛ `Routes.ALL` و تست مربوطه (`RoutesTest`) از ۵ به ۷ به‌روزرسانی شد.
- `PlaceholderScreens.kt` کامل حذف شد — هیچ Placeholder‌ای در پروژه باقی نمانده (همه ۷ صفحه واقعی‌اند).

### تست‌ها
- `SettingsViewModelMappingTest` (۷ مورد، روی `parseMaxReviewCards`)
- `RoutesTest` به‌روزرسانی‌شده (۷ مقصد)
- `CategoriesViewModel` تست مجزا ندارد — بدون منطق خالص فراتر از UseCaseهای دامنه‌ی از‌قبل‌تست‌شده (`CategoryUseCasesTest`)، مطابق همان الگو که فرم‌های ساده Setter در `AddWordViewModel` هم مستقیماً تست نشده‌اند.
- **محدودیت شناخته‌شده (تکراری از فازهای قبل):** ترکیب واقعی Compose+Hilt (`hiltViewModel()`، DI واقعی `ThemeViewModel` در سطح Activity) فقط با Build/Instrumentation واقعی قابل تأیید نهایی است.

## جزئیات فاز ۲۹ (تکمیل‌شده)

### تصمیم آگاهانه اصلی: تست یکپارچگی واقعی، بدون Emulator
تصمیم معلق در فاز ۹ («فعلاً تست Instrumented اضافه نشد، در فاز ۲۹ بازبینی می‌شود») این‌جا نهایی شد: به‌جای `androidTest` واقعی (نیازمند Emulator که CI فعلی ندارد)، از **Robolectric** برای اجرای تست‌های واقعی روی Room واقعی (نه Fake) به‌صورت تست Unit معمولی (JVM، بدون Emulator) استفاده شد. نتیجه: هیچ تغییری در `.github/workflows/*.yml` لازم نبود — همان قدم `gradle test` موجود، این تست‌های یکپارچگی جدید را هم اجرا می‌کند. وابستگی‌های `org.robolectric:robolectric:4.11.1` + `androidx.test:core:1.5.0` + `androidx.room:room-testing:2.6.1` به `testImplementation` دو ماژول `database` و `data` اضافه شد.

### چرا این تست‌ها لازم بودن
تا این فاز، **همه** تست‌های UseCase (۲۸ فاز قبلی) روی `Fake*Repository` در حافظه اجرا می‌شدند — سریع، ولی هیچ‌کدام واقعاً SQL/Room را لمس نمی‌کردند. به‌طور مشخص:
- `FakeFlashLearnDatabase.withTransaction` طبق کامنت خودش «no real rollback semantics» دارد — یعنی Rollback واقعی تراکنش هرگز تست نشده بود، درحالی‌که Rollback اتمیک دقیقاً همان چیزی است که سند بیشترین تأکید را رویش دارد (Descriptions §۲، Algorithms v4.10 تصحیح #۵).
- ایندکس یکتای شرطی `WHERE isActive = 1` روی `language_pairs` یک SQL خام داخل `FlashLearnDatabaseCallback` است که هیچ‌وقت توسط Room Annotation Processor یا تست‌های Fake اعتبارسنجی نمی‌شد.
- منطق UUID→Database-ID در Backup/Restore فقط وقتی معنا دارد که واقعاً **دو دیتابیس فیزیکی جدا** درگیر باشند؛ یک Map مشترک در حافظه (الگوی Fake) هرگز نمی‌توانست باگ این بخش را نشان دهد.

### فایل‌های جدید
- `database/src/test/.../LanguagePairActiveIndexTest.kt` — تأیید می‌کند دومین `LanguagePair` با `isActive=true` واقعاً توسط SQLite رد می‌شود (`SQLiteConstraintException`)، و چند ردیف `isActive=false` مشکلی ندارند.
- `data/src/test/.../integration/RealRoomTestHarness.kt` — هارنس مشترک: یک `FlashLearnRoomDatabase` واقعی In-Memory + نمونه واقعی همه ۱۳ Repository Impl (بدون Hilt — سازنده‌ها همگی فقط یک DAO می‌گیرند، دستی Wire شدن ساده‌تر از راه‌انداختن Hilt Test Rule بود).
- `CreateConceptIntegrationTest.kt` — اتمیک‌بودن واقعی Concept+۲Content+LearningState+DifficultyState، و صحت واقعی `canonicalKey` روی ردیف واقعاً نوشته‌شده در دیتابیس.
- `SubmitReviewAnswerIntegrationTest.kt` — مسیر موفق (DAILY→WEEKLY + یک ردیف ReviewHistory واقعی)، Duplicate attempt (رد می‌شود و **هیچ‌چیز تغییر نمی‌کند** — تأیید‌شده با خواندن دوباره از دیتابیس واقعی)، DATA_INTEGRITY_ERROR روی Concept ناموجود، Weekly-Wrong→MEDIUM اجباری، و یک تست مجزا که مستقیماً `androidx.room.withTransaction` واقعی را صدا می‌زند تا رفتار Rollback واقعی (نه از طریق UseCase، چون مسیرهای آن قبل از نوشتن دوم همیشه Guard می‌شوند) اثبات شود.
- `BackupRestoreIntegrationTest.kt` — بین **دو** نمونه واقعی Room جدا (source/target): Backup کامل از یکی، Restore در دیگری (خالی)، تأیید تطابق کامل شناسه‌ها/وضعیت؛ و یک تست Idempotency (Restore دوباره‌ی همان Backup باید Merge کند نه Duplicate).

### باگ‌های واقعی پیدا و رفع‌شده (نه فرضی)
دو کامنت مستند که با کد واقعی تناقض داشتند، هنگام نوشتن تست‌های بالا کشف و اصلاح شدند (نه تغییر رفتار، فقط تصحیح مستندات گمراه‌کننده):
1. `LanguagePairEntity.kt` و `LanguageDaos.kt` ادعا می‌کردند قانون «حداکثر یک LanguagePair فعال» توسط «LanguagePairUseCases» در لایه دامنه enforce می‌شود — چنین فایلی اصلاً در پروژه وجود ندارد (`grep` تأیید کرد). تنها enforcement واقعی همان ایندکس یکتای شرطی در `FlashLearnDatabaseCallback` است؛ کامنت‌ها اصلاح شدند.
2. `ExportData.kt` هنوز می‌گفت «Phase 17 — not yet implemented» درحالی‌که Phase 17 از فاز ۱۷ (دو روز قبل) Done است.

### محدودیت‌های شناخته‌شده باقی‌مانده
- **Migration واقعی هنوز تست نشده** — چون `FLASHLEARN_SCHEMA_VERSION` هنوز ۱ است و `ALL_MIGRATIONS` خالی (هیچ Migration واقعی برای تست وجود ندارد). اولین بار که یک Migration واقعی اضافه شود، باید یک `MigrationTest` (با `MigrationTestHelper` رسمی Room) هم اضافه شود — این‌جا فقط زیرساخت (`room-testing`) آماده است.
- تست‌های UI (Compose+Hilt واقعی) هنوز خارج از این فاز‌اند؛ همان محدودیت تکرارشونده فازهای ۲۳ تا ۲۸.
- «رفع خطا» این فاز محدود بود به دو کامنت مستندسازی نادرست که در حین ساخت تست‌های واقعی کشف شد؛ هیچ باگ رفتاری (منطق غلط در کد اجراشونده) پیدا نشد — که خودش یک سیگنال مثبت درباره‌ی دقت فازهای قبلی است، نه نبود بررسی.

## جزئیات فاز ۳۰ (تکمیل‌شده — همه ۳۰ فاز Done)

فاز آخر؛ صرفاً پالیش، نه منطق جدید. کد دامنه/UseCase در این فاز دست نخورد.

- `README.md` کامل بازنویسی شد — نسخه قبلی فقط مال فاز ۱ بود («فاز فعلی: ۱ از ۳۰»). نسخه جدید: معماری، Build/Test، خلاصه وضعیت، و **فهرست تجمیعی همه شکاف‌های شناخته‌شده** (جمع‌آوری‌شده از تمام بخش‌های «شکاف باز» پراکنده در فازهای ۲۴/۲۷/۲۸/۲۹ این سند، به‌علاوه یک مورد جدید: Import/Export چندفرمتی CSV/XLSX/SQLite طبق Algorithms §9 هرگز در برنامه ۳۰ فاز این بازنویسی نبود — قبلاً جایی صریحاً به این عنوان ثبت نشده بود).
- آیکون واقعی اپ: `mipmap-anydpi-v26/ic_launcher.xml` (Adaptive Icon، Vector — سازگار با minSdk 26) جایگزین `@android:drawable/sym_def_app_icon` (آیکون placeholder سیستمی که تا فاز ۲۹ در Manifest بود) شد. پس‌زمینه از رنگ Primary برند (`#4F46E5`، همان `Color.kt`) و فورگراند یک گلیف ساده Vector از استعاره «دسته فلش‌کارت». `AndroidManifest.xml` به `@mipmap/ic_launcher`/`ic_launcher_round` وصل شد.
- `versionName` در `app/build.gradle.kts` از `0.1.0-phase28` به `1.0.0` تغییر کرد — نشان‌دهنده تکمیل هر ۳۰ فاز برنامه‌ریزی‌شده (نه ادعای «Spec کامل ۱۰۰٪»؛ README بخش «شکاف‌های شناخته‌شده» صریحاً محدودیت‌های واقعی را فهرست می‌کند).
- بازبینی نهایی مستندات: هیچ `TODO`/`FIXME` باقی‌مانده در کد پیدا نشد (فاز ۲۹ همان جست‌وجو را انجام داد و دو کامنت واقعاً نادرست را همان‌جا اصلاح کرد؛ این فاز فقط دوباره تأیید کرد چیز جدیدی از قلم نیفتاده).

**نتیجه‌گیری برای هوش مصنوعی/انسان بعدی:** اگر قرار است کار روی این پروژه ادامه پیدا کند، اولین قدم منطقی احتمالاً یکی از پنج مورد بخش «شکاف‌های شناخته‌شده» در README است — مخصوصاً Seed کردن یک `LanguagePair` واقعی (مورد ۴)، چون بدون آن هیچ‌کدام از فازهای آینده که واقعاً از `LanguagePairRepository.getActive()` استفاده کنند قابل تست دستی نیستند.

## فاز ۳۱ (بعد از v1.0.0 — تکمیل‌شده): Seed کردن LanguagePair پیش‌فرض

**رفع شکاف #۴ از فهرست «شکاف‌های شناخته‌شده» README/فاز ۳۰.** این پروژه رسماً در فاز ۳۰ به‌عنوان «۳۰ فاز کامل» بسته شد؛ این یک افزودنی بعد از آن است و عمداً به‌جای دستکاری شماره فازهای قبلی، شماره جدید گرفته.

**راه‌حل انتخاب‌شده و چرا:** یک UseCase جدید idempotent (`EnsureDefaultLanguagePairUseCase`) که هر بار قبل از insert، اول `getActive()` را چک می‌کند. سه گزینه دیگر رد شدند:
- **Room `onCreate` callback** (همان محلی که ایندکس یکتای LanguagePair ساخته می‌شود): رد شد چون `onCreate` برای *هر* دیتابیس In-Memory تازه هم اجرا می‌شود — یعنی تست‌های فاز ۲۹ (`LanguagePairActiveIndexTest`) که خودشان دستی یک `isActive=true` insert می‌کنند، با یک ردیف seed‌شده‌ی از‌قبل‌موجود به تناقض ایندکس یکتا می‌خوردند و می‌شکستند.
- **گذاشتن فراخوانی داخل `FlashLearnApplication.onCreate()`**: رد شد چون کامنت خود آن کلاس صریحاً می‌گوید «No other logic belongs here — all business logic lives in domain/data».
- **گذاشتن فراخوانی داخل `AppViewModel`**: رد شد چون KDoc خود همان کلاس از فاز ۲۲ صراحتاً می‌گوید «handles route selection only... never calls into domain at all» — این یک مرز معماری آگاهانه‌ی قبلی بود، نه یک محدودیت اتفاقی.
- **راه‌حل نهایی:** یک `StartupViewModel` جدید و مستقل، دقیقاً با همون الگوی `ThemeViewModel` (Scope سطح Activity، ساخته‌شده در `MainActivity` قبل از ورود به NavGraph) — چون این هم یک «باید دقیقاً یک‌بار برای کل پردازش اپ اجرا شود» است، نه منطق تعیین Route.

**فایل‌های جدید:**
- `domain/usecase/LanguagePairUseCases.kt` (`EnsureDefaultLanguagePairUseCase`) + تست دامنه با Fake (۳ مورد)
- `data/.../integration/EnsureDefaultLanguagePairIntegrationTest.kt` — تأیید روی Room واقعی که Insert واقعاً به ایندکس یکتای فاز ۲۹ برمی‌خورد نه فقط به یک Map حافظه (۲ مورد)
- `app/.../presentation/StartupViewModel.kt` — بدون UI State (هیچ صفحه‌ای فعلاً از `getActive()` نمی‌خواند)؛ فقط یک‌بار UseCase را در `init` صدا می‌زند
- `MainActivity.kt`: یک خط `hiltViewModel<StartupViewModel>()` قبل از `ThemeViewModel`، برای فعال‌سازی همان `init`

**عمداً تغییر نکرد:** هیچ Consumer فعلی (`GenerateQuizQuestionUseCase`، Flashcard Content) به `LanguagePairRepository.getActive()` وصل نشد — همه هنوز از `defaultV1LanguagePair()` هاردکد استفاده می‌کنند. این فاز فقط داده را آماده کرد؛ وصل‌کردن واقعی Consumerها (که یعنی تغییر Constructor چند UseCase موجود) یک تصمیم/تغییر جدا و بزرگ‌تر است که عمداً اینجا انجام نشد.

**شکاف باز باقی‌مانده (تازه):** جدول `languages` (نام نمایشی es/fa/en) هنوز Seed نمی‌شود — چون `LanguagePairEntity.sourceLanguage/targetLanguage` رشته کد خام هستند (نه Foreign Key به `Language.id`، طبق تصمیم فاز ۱۶/۱۷)، این گپ فعلاً هیچ Consumer واقعی را مسدود نمی‌کند و لازم نبود در همین قدم حل شود.

## فاز ۳۲ (بعد از v1.1.0 — تکمیل‌شده): رفع ۳ باگ واقعی Build + یک باگ در فرآیند تحویل خودم

یک ممیزی خارجی (نه توسط من) روی یکی از zipهای تحویل‌داده‌شده اجرا شد و
۳ مشکل قطعی Build را در کد پروژه پیدا کرد — هر سه تأیید و رفع شدند. در
حین بررسی، یک چهارمین مشکل هم پیدا شد که مال خود این چت بود، نه پروژه.

### ۱. `ksp { }` به‌اشتباه داخل `android { }` در `database/build.gradle.kts`
KSP یک Extension سطح-بالای Gradle است، نه عضوی از `LibraryExtension`
(بلاک `android {}`). این خطا در فاز Configuration خودِ Gradle رخ می‌داد
(«Unresolved reference: ksp») و چون همه ماژول‌ها به `:database` وابسته‌اند،
کل Build حتی وارد Configuration هم نمی‌شد. **این باگ از همون فازی که
`room.schemaLocation` اضافه شد وجود داشته و توسط هیچ‌کدام از بررسی‌های
من (که فقط تعادل براکت‌ها را چک می‌کردند، نه معنای واقعی Gradle DSL) قابل
کشف نبود** — چون من در این محیط sandbox امکان اجرای واقعی Gradle را ندارم.
رفع: `ksp {}` به سطح top-level (هم‌ردیف `android{}` و `dependencies{}`) منتقل شد.

### ۲. عدم وجود Dependency واقعی برای `Theme.Material3.DayNight.NoActionBar`
این Style از کتابخانه کلاسیک `com.google.android.material:material` می‌آید،
نه از `androidx.compose.material3` (که فقط Compose است و هیچ XML Style‌ای
منتشر نمی‌کند). بدون این Dependency، AAPT2 هنگام Resource Linking با خطای
«style not found» شکست می‌خورد. رفع: `implementation("com.google.android.material:material:1.11.0")` به `app/build.gradle.kts` اضافه شد.

### ۳. `androidTestImplementation("androidx.compose.ui:ui-test-junit4")` بدون نسخه
`androidTestImplementation`، برخلاف `debugImplementation`، از `implementation`
ارث‌بری Constraint نمی‌کند؛ پس BOM ایمپورت‌شده در `implementation(platform(...))`
روی آن اعمال نمی‌شد و این Dependency بدون نسخه Resolve نمی‌شد. رفع:
`androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))`
جدا اضافه شد.

### ۴. باگ در فرآیند تحویل خودِ من (نه در کد پروژه): `.github` و `.gitignore` از هر zip حذف می‌شدند
ممیزی خارجی همچنین ادعا کرده بود `.github/workflows/android-ci.yml` و
`.gitignore` در پروژه وجود ندارند — این ادعا درباره‌ی **کد پروژه غلط**
بود (هر دو فایل همیشه در `docs/PROGRESS_TRACKER.md`/کد موجود بودند)، اما
ریشه‌ی واقعی گزارش درست بود: من در دستور ساخت هر zip از
`zip -r ... FlashLearn -x "*.git*"` استفاده می‌کردم تا پوشه `.git` را
حذف کنم (که این پروژه اصلاً هیچ‌وقت نداشت — هرگز `git init` نشده). پترن
`*.git*` علاوه بر `.git`، با `.github` هم مچ می‌شود (چون `.github` با
`.git` شروع می‌شود)، پس **هر زیپی که از فاز ۲۷ تا ۳۱ تحویل داده شد،
`.github/workflows/android-ci.yml` و `.gitignore` را نداشت** — با اینکه
هر دو در کدِ روی دیسک همیشه درست و کامل بودند. رفع: exclude اشتباه از
دستور zip کاملاً حذف شد (چون اصلاً `.git` وجود ندارد، نیازی به exclude
هم نیست). **از این zip به بعد، `.github` و `.gitignore` واقعاً داخل
فایل تحویلی هستند — لطفاً محتوای zip قبلی را با این جایگزین کنید.**

### چیزی که رفع نشد چون تأیید نشد
ممیزی همچنین «مشکوک» بودن نسخه Hilt+KSP (۲.۴۸) و ریسک شبکه Robolectric
در CI را مطرح کرد؛ این‌ها بدون اجرای واقعی Gradle قابل تأیید/رد نیستند
(خودِ گزارش هم آن‌ها را «نیازمند تست» برچسب زده بود، نه «قطعی»)، پس
دست‌نخورده باقی ماندند — منتظر یک اجرای واقعی CI روی همین commit.

## فاز ۳۳ (بعد از v1.1.1): اولین اجرای واقعی CI — تأیید فازهای ۳۲ + یک باگ کامپایل جدید

کاربر واقعاً CI را روی GitHub اجرا کرد (اولین اجرای واقعی Gradle در کل
تاریخ این پروژه — تا این لحظه هرچه «تست» می‌شد فقط منطق داخلی بود، نه
واقعاً build شدن). نتیجه لاگ کامل (`assembleDebug`) را آپلود کرد.

**تأیید مثبت مهم:** هر سه رفع فاز ۳۲ در عمل کار کردند —
`:app:processDebugResources` (AAPT2) موفق شد (یعنی رفع Material Theme
درست بود)، `:database:kspDebugKotlin`/`compileDebugKotlin` موفق شدند
(یعنی رفع جای `ksp{}` درست بود)، و `:app:kspDebugKotlin` (Hilt) هم بدون
مشکل اجرا شد (یعنی نگرانی «مشکوک» ممیزی قبلی درباره Hilt+KSP ۲.۴۸ بی‌مورد
بود — این‌جا با اجرای واقعی رد شد، نه فقط با استدلال).

**باگ واقعی جدید (جدا از فاز ۳۲، مال فاز ۲۸):** `:app:compileDebugKotlin`
با این خطا شکست خورد:
```
CategoriesScreen.kt:11:43 Cannot access 'weight': it is internal in 'androidx.compose.foundation.layout'
```
علت: خط `import androidx.compose.foundation.layout.weight` در
`CategoriesScreen.kt` (فاز ۲۸) اشتباه بود. `Modifier.weight(...)` یک
Extension Function روی `RowScope`/`ColumnScope` است، نه یک تابع
top-level قابل import — داخل `Row{}`/`Column{}` خودکار در دسترس است.
این import اشتباه به یک سیمبل `internal` نامرتبط در همان پکیج resolve
می‌شد. رفع: خط import حذف شد (هر دو استفاده از `.weight(1f)` در این
فایل از قبل درست داخل `Row{}` بودند، پس نیازی به تغییر دیگری نبود).
`grep` تأیید کرد این اشتباه در هیچ فایل دیگری تکرار نشده بود.

**نتیجه‌گیری کلی:** این اولین بار بود که خروجی واقعی Gradle در دسترس
بود، نه فقط استدلال دستی. هر خطای واقعی که پیدا شد (فاز ۳۲ + این فاز)
از همون دسته چیزهایی بود که هیچ بررسی متنی/براکت‌شماری نمی‌توانست پیدا
کند — یعنی از این به بعد، بعد از هر تغییر در فایل‌های UI/Gradle، بهترین
راه تأیید واقعی همچنان «واقعاً در CI اجرا کن» است، نه استدلال از روی کد.

## فاز ۳۴ (بعد از v1.1.2): دومین اجرای واقعی CI — `assembleDebug` موفق، ۳ باگ در `gradle test`

خبر خیلی خوب: **`assembleDebug` این‌بار کامل موفق شد** — یعنی هر سه رفع
فاز ۳۲ (`ksp{}`، Material، BOM) و رفع فاز ۳۳ (`weight` import) قطعاً
درست بودند و اپ واقعاً build می‌شود. مرحله بعدی (`gradle test`) روی
`:domain:compileTestKotlin` شکست خورد — این هم اولین باری بود که این
فایل‌های تست دامنه (نوشته‌شده در فازهای خیلی قبل‌تر، قبل از این چت)
واقعاً توسط یک کامپایلر Kotlin واقعی خوانده می‌شدند.

**باگ ۱ — Smart-cast بین ماژول (`FakeRepositories.kt`):**
`it.stage == stage && it.nextReviewAt != null && !it.nextReviewAt.isAfter(now)`
— چون `nextReviewAt` روی `LearningState` در `domain/main` تعریف شده و این
کد در `domain/test` است، کامپایلر Kotlin این دو را «ماژول‌های متفاوت»
می‌بیند و اجازه Smart-cast بعد از چک `!= null` را روی یک Property
(نه یک `val` محلی) نمی‌دهد. رفع: یک `val dueAt = it.nextReviewAt` محلی
قبل از استفاده، در هر دو متد (`getDueByStage`, `getAllDueNonLearned`).

**باگ ۲ و ۳ — `class Fixture` باید `inner` باشد (`SelectReviewQueueUseCaseTest.kt`، `CreateBackupUseCaseTest.kt`):**
هر دو فایل یک `private val now = Instant.parse(...)` سطح کلاس دارند و
یک `private class Fixture { ... }` تودرتو که از داخل توابعش به همون
`now` رجوع می‌کند. یک Nested Class معمولی (بدون `inner`) هیچ ارجاع
ضمنی به نمونه کلاس بیرونی ندارد — پس `now` از داخلش اصلاً «دیده» نمی‌شود
(«Unresolved reference: now»، نه یک Typo). رفع: هر دو به
`private inner class Fixture` تغییر کردند.

**بررسی گسترده انجام‌شده تا باگ مشابه جای دیگه نمونده باشه:**
- ۹ فایل تست دیگر هم دقیقاً همین الگوی `private class Fixture` را دارند؛ تک‌تک بدنه‌شان به‌صورت برنامه‌ای (نه فقط چشمی) بررسی شد که آیا به `now`/`zone`/`pair` بیرونی ارجاع می‌دهند یا نه — هیچ‌کدام ارجاع نمی‌دهند، پس نیازی به `inner` ندارند و دست‌نخورده ماندند.
- کل `domain/src/test`، `app/src/test`، `data/src/test`، `database/src/test` برای الگوی مشابه Smart-cast (`.x != null && ...x.`) جست‌وجو شد — هیچ نمونه دیگری پیدا نشد.

**نتیجه:** این سومین دور اجرای واقعی CI بود؛ هر دور دقیقاً یک لایه جدید
از خطاها را آشکار کرد که فقط با کامپایل واقعی قابل کشف بودند (نه با
هیچ بررسی متنی من). الگوی روشنی شکل گرفته: کد نوشته‌شده توسط جلسات
قبلی (قبل از دسترسی به CI واقعی) به‌طور سیستماتیک هرگز واقعاً کامپایل
نشده بود.

## فاز ۳۵ (بعد از v1.1.3): تحلیل دقیق ۳ Failure واقعی تست (توسط ابزار دیگر) + رفع

یک تحلیل مستقل دیگر (نه توسط من) کامیت `v1.1.3` را واقعاً روی GitHub Actions اجرا کرد؛ `assembleDebug` این‌بار **کامل موفق شد**، ولی `gradle test` با ۳ Failure واقعی (نه خطای Cache/زیرساخت که همزمان در همان Run رخ داده بود) شکست خورد. هر سه با کد واقعی تطبیق داده و تأیید شدند:

### ۱. `VocabularyParserTest` — باگ واقعی در `VocabularyParser.kt` (تنها موردی که واقعاً باگ Parser بود)
تست شماره‌گذاری با Gap (`24. palabra` / `29. otra palabra` / `47. tercera palabra`) انتظار داشت خروجی Orphan برابر با متن Strip‌شده (`palabra`) باشد، اما کد واقعی `orphanLines.add(entry.originalLines.joinToString("\n"))` را صدا می‌زد — یعنی متن خام هنوز شماره‌دار (`24. palabra`) را برمی‌گرداند، نه نسخه Strip‌شده که یک Entry موفق هم استفاده می‌کند (`entry.sourceText`). **تشخیص Boundary سه Entry (۲۴/۲۹/۴۷) از اول درست بود** — فقط متن ذخیره‌شده برای Orphan اشتباه بود. رفع: تغییر به `orphanLines.add(entry.sourceText)`.

### ۲. `RestoreBackupUseCaseTest` («ConceptTag counted as new only the first time») — باگ در Fixture تست، نه در `RestoreBackupUseCase`
تست یک `Tag` مستقیماً در دیتابیس مقصد insert می‌کرد ولی هیچ‌وقت آن را داخل خودِ `ExportData` (`tags`) نمی‌گذاشت — و تابع کمکی `fullExport()` این تست اصلاً پارامتر `tags` نداشت. `validateBackup()` به‌درستی هر Backup‌ای که یک `ConceptTag` به `tagId` خارج از لیست `tags` همان Backup ارجاع دهد را نامعتبر می‌داند (سازگاری ارجاعی داخلیِ خودِ Export، نه وابسته به این‌که مقصد از قبل آن UUID را دارد یا نه) — پس `restoreBackup` یک `RestoreResult.Error` برمی‌گرداند، و `as RestoreResult.Success` با `ClassCastException` می‌ترکید. رفع: پارامتر `tags` به `fullExport()` اضافه شد و همین تست حالا `tags = listOf(tag)` هم می‌فرستد. منطق شمارش ConceptTag در خودِ UseCase از اول درست بوده.

### ۳. `BackupRestoreIntegrationTest` («full backup ... restores completely») — باگ در همین فایل تست، نه در Backup/Restore واقعی
تست یک `SubmitReviewAnswerRequest` با یک `sessionId` تصادفی می‌فرستاد بدون این‌که هرگز یک `ReviewSession` واقعی با همان id در دیتابیس Source بسازد. `validateBackup()` به‌درستی هر `ReviewHistory.sessionId` خارج از `reviewSessions` همان Backup را نامعتبر می‌داند؛ پس FULL Backup تولیدشده نامعتبر بود و Restore با `Error` برمی‌گشت، نه `Success`. رفع: قبل از فراخوانی `submitAnswerInSource`، یک `ReviewSession` واقعی با همون `sessionId` در `source.reviewSessionRepository` insert می‌شود؛ یک assertion جدید (`backup.reviewSessions.size == 1`) هم اضافه شد تا این پیش‌شرط دیگر خاموش نماند.

### جمع‌بندی
از ۳ Failure، فقط مورد ۱ واقعاً باگ در کد تولیدی (Parser) بود. موارد ۲ و ۳ نشان دادند خودِ `validateBackup()` دقیقاً طبق قرارداد Algorithms §۹ عمل می‌کند (سازگاری ارجاعی داخلی Backup را جدی می‌گیرد)؛ Fixtureهای تست فازهای ۲۹ بودند که این قرارداد را رعایت نکرده بودند. این الگو خودش دلگرم‌کننده است: هرچه لایه‌های عمیق‌تر (Validation، Atomicity) بیشتر با CI واقعی محک می‌خورند، باگ کمتری در منطق اصلی و بیشتر در Fixtureهای ساده‌انگارانه‌ی تست پیدا می‌شود.

## نکات فنی مهم برای مراحل بعد

- الگوریتم‌های Pure (`Learning Transition`, `Difficulty Calculation`) باید در `domain` بدون وابستگی به Room/Android نوشته شوند. `GenerateQuizQuestion` (فاز ۱۴) برخلاف این دو، برای ساخت Pool Distractor به Repositoryها نیاز دارد (مثل `SelectReviewQueue`) — پس به‌صورت UseCase در `domain` نوشته شد، نه تابع Pure مستقل؛ اما همچنان کاملاً Read-only است و هیچ Stateای تغییر نمی‌دهد.
- `LearningState` هرگز نباید فیلد `difficulty` داشته باشد — این فیلد فقط در `DifficultyState` است.
- `threshold_difficulty` پیش‌فرض 3، در V1 بدون UI تغییر.
- Random Review فقط DAILY/WEEKLY/MONTHLY واجد‌شرایط؛ LEARNED مستقل.
- کلید `canonicalKey` = trim + lowercase + collapse space؛ Accent/علامت حفظ می‌شود.
- از فاز ۲۳: هر صفحه زیرپکیج خودش را دارد (`presentation.<screen>`, `ui.screens.<screen>`)؛ فازهای ۲۴ تا ۲۸ همین الگو را ادامه دهند.
- فاز ۲۵/۲۶ (Review Screen): نوع مرور اولیه از `backStackEntry.arguments?.getString(Routes.REVIEW_TYPE_ARG)` خوانده می‌شود (رشته نام Enum `ReviewType`، nullable — اگر null بود یعنی کاربر مستقیم/بدون انتخاب از Home وارد شده و صفحه Review باید خودش پیش‌فرض/انتخاب‌گر نشان دهد؛ فاز ۲۵ فعلاً پیش‌فرض DAILY گذاشت).
- **شکاف باز (Achievement Check):** `CheckAndUnlockAchievementsUseCase` فقط از `ProgressViewModel` (فاز ۲۷) صدا زده می‌شود، نه از `ReviewViewModel`؛ طبق سند هردو مجازند اما فقط یکی سیم‌کشی شده — رفتار صحیح، تجربه کاربری ناقص (کاربری که هیچ‌وقت Progress را باز نکند دستاوردش هم دیده نمی‌شود).
- **شکاف باز (Backup/Restore UI):** فاز ۲۸ عمداً هیچ دکمه Backup/Restore به Settings اضافه نکرد — جزئیات کامل در بخش «جزئیات فاز ۲۸» بالا.
- **شکاف باز (انتخاب Category در AddWord):** `AddWordScreen` (فاز ۲۴) فیلد Category ندارد؛ صفحه مدیریت دسته‌بندی‌ها (فاز ۲۸) کاملاً مستقل از فرم افزودن کلمه است.
- **شکاف باز برای فاز ۲۷+ (LanguagePair):** هیچ کد فعلی یک `LanguagePair` را Seed یا Active نمی‌کند؛ هرجا به جفت‌زبان فعال نیاز بود (`GetFlashcardContentUseCase`, `GenerateQuizQuestionUseCase` از طریق `defaultV1LanguagePair()`) پیش‌فرض هاردکد es/fa استفاده شده. اگر فازی بعدی بخواهد واقعاً از `LanguagePairRepository.getActive()` استفاده کند، اول باید یک مسیر Seed/Onboarding برای همان مشخص و پیاده شود. **[بخش Seed در فاز ۳۱ حل شد — یک ردیف واقعی حالا در `language_pairs` وجود دارد؛ وصل‌کردن Consumerها به `getActive()` هنوز باز است، جزئیات در «فاز ۳۱».]**

## تصمیم‌های ثبت‌شده این بازنویسی

| تاریخ | تصمیم | دلیل |
|-------|--------|------|
| 2026-09-13 | بازنویسی کامل از صفر به‌جای ادامه v4.36 | درخواست صریح کاربر؛ Scope دقیقاً منطبق با Descriptions/Algorithms v4.20 |
| 2026-09-13 | CI بدون Gradle wrapper باینری (`gradle/actions/setup-gradle`) | جلوگیری از نیاز به تولید/commit فایل باینری wrapper در محیط بدون Android Studio |
| 2026-09-13 | آیکون launcher موقت (`sym_def_app_icon`) | جلوگیری از خطای resource-linking تا فاز پالیش (۳۰) که آیکون واقعی اضافه می‌شود |

---

**پایان سند فاز ۱ — بعد از هر فاز به‌روزرسانی شود.**
