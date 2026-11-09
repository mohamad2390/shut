# SHUT — Creative Services Marketplace

زیرساخت مارکت‌پلیس خدمات عکاسی، فیلم‌برداری و تولید محتوای تصویری.

## پیش‌نیازها
- JDK 21
- Maven 3.9+
- Docker Desktop و Docker Compose

## راه‌اندازی محلی
1. فایل `.env.example` را به `.env` کپی کن و مقادیر امن خودت را وارد کن.
2. اجرا کن: `docker compose up -d`
3. پس از healthy شدن PostgreSQL، متغیرهای `.env` را در Run Configuration برنامه قرار بده و Spring Boot را اجرا کن.

پورت: PostgreSQL روی `localhost:5433`.

در PowerShell نمونه: 
```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:DB_PASSWORD="your-db-password"
mvn spring-boot:run
```

## API محلی کاربران
برای فعال‌کردن این APIها در توسعه، پروفایل `local` را صریحاً فعال کن (مثلاً `SPRING_PROFILES_ACTIVE=local`). این APIها فقط با پروفایل `local` فعال‌اند و برای تست توسعه هستند؛ تا قبل از اضافه شدن احراز هویت نباید در محیط عمومی فعال شوند.

- `POST /api/v1/users/onboarding` — ایجاد حساب یا به‌روزرسانی اطلاعات پایه تلگرام
- `GET /api/v1/users/{id}` — دریافت کاربر با شناسه داخلی
- `PATCH /api/v1/users/{id}` — ویرایش اطلاعات پایه
- `GET /actuator/health` — سلامت برنامه
- `GET /swagger-ui/index.html` — مستندات Swagger UI

نمونه درخواست onboarding:
```json
{
  "telegramId": 123456789,
  "telegramUsername": "sample_user",
  "firstName": "Ali",
  "lastName": "Ahmadi"
}
```

## نقش‌ها
نقش‌ها چندمقداری‌اند و یک حساب می‌تواند هم‌زمان `CUSTOMER` و `PROVIDER` باشد. نقش `ADMIN` فقط باید از مسیر مدیریتی امن و پس از پیاده‌سازی احراز هویت اعطا شود.

## ساختار فعلی
```text
com.shut
  ShutApplication.java
  user/
    controller/
    dto/
    entity/
    exception/
    repository/
    service/
src/main/resources/db/migration/
  V1__create_users.sql
  V2__replace_user_type_with_roles.sql
```

## نکات امنیتی
- رمزها فقط از متغیرهای محیطی خوانده می‌شوند؛ `.env` را commit نکن.
- API پروفایل `local` عمداً فاقد احراز هویت است و صرفاً برای توسعه محلی است.
- فایل‌های رسانه‌ای روی سرور ذخیره نمی‌شوند؛ فقط شناسه‌ی فایل تلگرام (`file_id`) و metadata در دیتابیس نگه داشته می‌شود.


## Phase 2 - Professional profile

The local-only professional profile API is available after setting `SPRING_PROFILES_ACTIVE=local`.

- `PUT /api/v1/users/{userId}/professional-profile` creates or updates a professional profile.
- `GET /api/v1/users/{userId}/professional-profile` retrieves the profile.

Example request body:

```json
{
  "displayName": "Ali Ahmadi Photography",
  "headline": "Photographer and video editor",
  "bio": "I work in portrait, product and commercial photography.",
  "yearsExperience": 5,
  "baseCity": "Tehran",
  "baseCountry": "Iran",
  "websiteUrl": "https://example.com",
  "instagramUsername": "ali.photo",
  "equipmentDescription": "Sony mirrorless camera, 24-70mm lens, lighting kit",
  "skills": ["Portrait photography", "Product photography", "Video editing"],
  "serviceCities": ["Tehran", "Karaj"],
  "collaborators": ["Example Studio", "Example Production"]
}
```

Saving a professional profile automatically adds the `PROVIDER` role while preserving existing roles such as `CUSTOMER`. This API is local-only and is not yet protected by Telegram authentication; do not expose it publicly.

## ربات تلگرام
رابط کاربری برنامه خود ربات است (دکمه‌های شیشه‌ای و گفتگو). برای فعال‌کردن:

1. از BotFather توکن بگیر و در `.env` بگذار: `TELEGRAM_BOT_TOKEN`.
2. ربات را ادمین کانال کن و `TELEGRAM_CHANNEL_ID` و `TELEGRAM_CHANNEL_LINK` را بگذار. اگر `TELEGRAM_CHANNEL_ID` خالی باشد، بررسی عضویت خاموش است.
   برای پشتیبان‌گیری از مدیای ویترین، یک کانال **خصوصی** بساز، ربات را ادمین (با اجازه‌ی ارسال پیام) کن و شناسه‌اش را در `TELEGRAM_STORAGE_CHANNEL_ID` بگذار. خالی بودن آن یعنی بدون پشتیبان.
3. در `application.yml` مقدار `telegram.bot.enabled` را `true` کن.

دستورها: `/start` (ساخت حساب و منوی اصلی)، `/menu` (منوی اصلی)، `/cancel` (لغو کار نیمه‌تمام).

ساختار پکیج `com.shut.telegram`:

| پکیج | کار |
|---|---|
| `client`, `model`, `config` | اتصال به Bot API |
| `polling`, `dispatch` | دریافت آپدیت‌ها و رساندن به handler مناسب |
| `handler` | کلاس‌های پایه‌ی دستور و دکمه، و دستورهای `/start` `/menu` `/cancel` |
| `membership` | بررسی عضویت در کانال |
| `conversation` | ذخیره‌ی مرحله‌ی گفتگوی هر کاربر در دیتابیس |
| `messaging` | `Screen` (متن و کیبورد)، ساخت کیبورد و ارسال پیام |
| `menu`, `profile`, `showcase`, `pricing`, `calendar` | بخش‌های ربات؛ هر بخش جدید یک پکیج مثل این‌ها |

برای افزودن یک بخش جدید: یک `CallbackHandler` با prefix مخصوص خودش و یک `Screen` برایش بنویس و دکمه‌اش را به منوی اصلی اضافه کن.

## ویترین
هر کاربر نمونه‌کارهایش را در دسته‌ها نگه می‌دارد: عکاسی، فیلمبرداری، ادیت عکس، تدوین، تصویربرداری هوایی، تیزر و سایر. حداکثر ۲۰ مورد در هر دسته.

- عکس یا ویدیو (یا فایل تصویر/ویدیو برای حفظ کیفیت) مستقیم در ربات فرستاده می‌شود و فقط `file_id` تلگرام در جدول `showcase_items` ذخیره می‌شود.
- اگر `TELEGRAM_STORAGE_CHANNEL_ID` تنظیم شده باشد، از هر فایل یک نسخه در آن کانال کپی می‌شود (`copyMessage`) و با حذف نمونه‌کار، آن نسخه هم پاک می‌شود. این موضوع در پیام افزودن به کاربر گفته می‌شود.
- `file_id` فقط برای همین ربات معتبر است؛ با عوض‌شدن توکن یا حذف ربات از کار می‌افتد. نسخه‌ی پشتیبان برای همین است.

## قیمت‌ها
هر کاربر یک منوی قیمت دارد، مثلاً «عکاسی: ۱٬۰۰۰٬۰۰۰ تومان، هر ساعت» یا «ادیت عکس: ۵۰۰٬۰۰۰ تومان، هر عکس». حداکثر ۲۰ خدمت.

- افزودن در سه مرحله است: اسم خدمت، انتخاب واحد با دکمه (هر ساعت، هر روز، هر عکس، هر دقیقه ویدیو، هر پروژه) و قیمت به تومان. قیمت با ارقام فارسی و جداکننده‌ی هزارگان هم قبول می‌شود.
- حذف از دکمه‌ی «حذف یک خدمت» انجام می‌شود. ویرایش قیمت فعلاً نیست؛ خدمت را حذف و دوباره اضافه کن.
- جدول `price_items`، مایگریشن `V6`.

## تقویم
هر کاربر روزهای آماده‌به‌کار و روزهای پر را روی تقویم شمسی علامت می‌زند.

- تقویم ماهانه با دکمه است. روی هر روز که بزنی وضعیتش می‌چرخد: نامشخص، 🟢 آزاد، 🔴 پر، و دوباره نامشخص. روزهای گذشته و بیشتر از ۳۶۶ روز آینده قابل تغییر نیستند.
- ماه جاری و ۱۱ ماه بعد قابل نمایش است. هفته از شنبه شروع می‌شود و ستون شنبه سمت راست قرار می‌گیرد (ثابت `RIGHT_TO_LEFT` در `CalendarScreens`؛ اگر روی دستگاهت وارونه دیدی، `false` کن).
- «امروز» به وقت تهران حساب می‌شود (`ClockConfig`).
- تبدیل تاریخ در `JalaliDate` است و با قاعده‌ی ۳۳ساله کار می‌کند که برای سال‌های ۱۲۴۴ تا ۱۴۷۲ با تقویم رسمی ایران می‌خواند. خروجی آن برای همه‌ی روزهای ۱۹۹۰ تا ۲۰۶۰ با ICU مقایسه شده است.
- جدول `calendar_days`، مایگریشن `V7`. فقط روزهای علامت‌خورده ذخیره می‌شوند.

