CREATE TABLE IF NOT EXISTS sms_verification_codes
(
    phone_number VARCHAR(13) NOT NULL,
    code         VARCHAR(6) NOT NULL,
    created_at   BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS sms_verification_codes_phone_number_idx ON sms_verification_codes (phone_number);

CREATE TABLE IF NOT EXISTS users
(
    id            BIGSERIAL PRIMARY KEY,
    phone_number  VARCHAR(13) NOT NULL UNIQUE,
    display_name  VARCHAR(100),
    created_at    BIGINT NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TYPE listing_status AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED');

CREATE TYPE car_brand AS ENUM (
    'TOYOTA', 'VOLKSWAGEN', 'BMW', 'MERCEDES_BENZ', 'AUDI', 'SKODA',
    'HYUNDAI', 'KIA', 'FORD', 'OPEL', 'RENAULT', 'PEUGEOT', 'CITROEN',
    'HONDA', 'MAZDA', 'NISSAN', 'MITSUBISHI', 'SUBARU', 'SUZUKI',
    'LEXUS', 'LAND_ROVER', 'JEEP', 'CHEVROLET', 'FIAT', 'VOLVO',
    'SEAT', 'DACIA', 'ALFA_ROMEO', 'PORSCHE', 'LADA', 'ZAZ', 'CUSTOM'
);

CREATE TYPE car_condition AS ENUM ('NEW', 'USED');

CREATE TYPE city AS ENUM (
    'KYIV', 'KHARKIV', 'ODESA', 'DNIPRO', 'ZAPORIZHZHIA', 'LVIV',
    'KRYVYI_RIH', 'MYKOLAIV', 'MARIUPOL', 'VINNYTSIA', 'KHERSON',
    'POLTAVA', 'CHERNIHIV', 'CHERKASY', 'SUMY', 'KHMELNYTSKYI',
    'IVANO_FRANKIVSK', 'RIVNE', 'ZHYTOMYR', 'TERNOPIL', 'LUTSK',
    'UZHHOROD', 'CHERNIVTSI', 'KREMENCHUK', 'BILA_TSERKVA', 'MELITOPOL',
    'MUKACHEVO', 'DROHOBYCH'
);

CREATE TYPE car_color AS ENUM (
    'WHITE', 'BLACK', 'SILVER', 'GRAY', 'RED', 'BLUE', 'DARK_BLUE',
    'GREEN', 'DARK_GREEN', 'YELLOW', 'ORANGE', 'BROWN', 'BEIGE',
    'PURPLE', 'GOLDEN', 'BURGUNDY'
);

CREATE TYPE transmission_type AS ENUM ('MANUAL', 'AUTOMATIC', 'CVT', 'SEMI_AUTOMATIC');

CREATE TYPE fuel_type AS ENUM ('PETROL', 'DIESEL', 'LPG', 'ELECTRIC', 'HYBRID', 'PLUG_IN_HYBRID');

CREATE TYPE drive_type AS ENUM ('FWD', 'RWD', 'AWD', 'FOUR_WD');

CREATE TYPE body_type AS ENUM (
    'SEDAN', 'HATCHBACK', 'WAGON', 'COUPE', 'CONVERTIBLE',
    'SUV', 'CROSSOVER', 'MINIVAN', 'PICKUP', 'VAN'
);

CREATE TABLE IF NOT EXISTS car_listings
(
    id                BIGSERIAL PRIMARY KEY,
    author_user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status            listing_status NOT NULL DEFAULT 'DRAFT',
    title             VARCHAR(200),
    description       TEXT,
    brand             car_brand,
    custom_brand_name VARCHAR(100),
    model             VARCHAR(100),
    license_plate     VARCHAR(20),
    condition         car_condition,
    mileage           INTEGER,
    price             BIGINT,
    city              city,
    color             car_color,
    transmission      transmission_type,
    fuel_type         fuel_type,
    tank_volume       NUMERIC(5, 1),
    drive_type        drive_type,
    body_type         body_type,
    year              SMALLINT,
    engine_volume     NUMERIC(4, 1),
    owners_count      SMALLINT,
    created_at        BIGINT NOT NULL,
    updated_at        BIGINT NOT NULL,
    published_at      BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS car_listing_analytics
(
    listing_id           BIGINT NOT NULL REFERENCES car_listings (id) ON DELETE CASCADE,
    impressions_count    INT NOT NULL DEFAULT 0,
    views_count          INT NOT NULL DEFAULT 0,
    phone_requests_count INT NOT NULL DEFAULT 0,
    favourites_count     INT NOT NULL DEFAULT 0,
    ts                   TIMESTAMP NOT NULL,
    PRIMARY KEY (listing_id, ts)
);
