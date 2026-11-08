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
| `menu`, `profile` | بخش‌های ربات؛ هر بخش جدید یک پکیج مثل این‌ها |

برای افزودن یک بخش جدید: یک `CallbackHandler` با prefix مخصوص خودش و یک `Screen` برایش بنویس و دکمه‌اش را به منوی اصلی اضافه کن.

