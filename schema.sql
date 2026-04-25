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

CREATE FUNCTION fts_car_brand(v car_brand) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE v
    WHEN 'TOYOTA'         THEN 'Toyota Тойота'
    WHEN 'VOLKSWAGEN'     THEN 'Volkswagen Фольксваген'
    WHEN 'BMW'            THEN 'BMW'
    WHEN 'MERCEDES_BENZ'  THEN 'Mercedes-Benz Мерседес'
    WHEN 'AUDI'           THEN 'Audi Ауді'
    WHEN 'SKODA'          THEN 'Skoda Шкода'
    WHEN 'HYUNDAI'        THEN 'Hyundai Хюндай'
    WHEN 'KIA'            THEN 'Kia Кіа'
    WHEN 'FORD'           THEN 'Ford Форд'
    WHEN 'OPEL'           THEN 'Opel Опель'
    WHEN 'RENAULT'        THEN 'Renault Рено'
    WHEN 'PEUGEOT'        THEN 'Peugeot Пежо'
    WHEN 'CITROEN'        THEN 'Citroen Сітроен'
    WHEN 'HONDA'          THEN 'Honda Хонда'
    WHEN 'MAZDA'          THEN 'Mazda Мазда'
    WHEN 'NISSAN'         THEN 'Nissan Ніссан'
    WHEN 'MITSUBISHI'     THEN 'Mitsubishi Міцубісі'
    WHEN 'SUBARU'         THEN 'Subaru Субару'
    WHEN 'SUZUKI'         THEN 'Suzuki Сузукі'
    WHEN 'LEXUS'          THEN 'Lexus Лексус'
    WHEN 'LAND_ROVER'     THEN 'Land Rover Ленд Ровер'
    WHEN 'JEEP'           THEN 'Jeep Джип'
    WHEN 'CHEVROLET'      THEN 'Chevrolet Шевроле'
    WHEN 'FIAT'           THEN 'Fiat Фіат'
    WHEN 'VOLVO'          THEN 'Volvo Вольво'
    WHEN 'SEAT'           THEN 'SEAT Сеат'
    WHEN 'DACIA'          THEN 'Dacia Дачія'
    WHEN 'ALFA_ROMEO'     THEN 'Alfa Romeo Альфа Ромео'
    WHEN 'PORSCHE'        THEN 'Porsche Порше'
    WHEN 'LADA'           THEN 'Lada Лада'
    WHEN 'ZAZ'            THEN 'ЗАЗ ZAZ'
    WHEN 'CUSTOM'         THEN 'інша марка'
    ELSE NULL
  END;
$$;

CREATE FUNCTION fts_car_condition(v car_condition) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE v
    WHEN 'NEW'  THEN 'новий нова нове'
    WHEN 'USED' THEN 'вживаний вживана вживане б/у'
    ELSE NULL
  END;
$$;

CREATE FUNCTION fts_city(v city) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE v
    WHEN 'KYIV'            THEN 'Київ'
    WHEN 'KHARKIV'         THEN 'Харків'
    WHEN 'ODESA'           THEN 'Одеса'
    WHEN 'DNIPRO'          THEN 'Дніпро'
    WHEN 'ZAPORIZHZHIA'    THEN 'Запоріжжя'
    WHEN 'LVIV'            THEN 'Львів'
    WHEN 'KRYVYI_RIH'      THEN 'Кривий Ріг'
    WHEN 'MYKOLAIV'        THEN 'Миколаїв'
    WHEN 'MARIUPOL'        THEN 'Маріуполь'
    WHEN 'VINNYTSIA'       THEN 'Вінниця'
    WHEN 'KHERSON'         THEN 'Херсон'
    WHEN 'POLTAVA'         THEN 'Полтава'
    WHEN 'CHERNIHIV'       THEN 'Чернігів'
    WHEN 'CHERKASY'        THEN 'Черкаси'
    WHEN 'SUMY'            THEN 'Суми'
    WHEN 'KHMELNYTSKYI'    THEN 'Хмельницький'
    WHEN 'IVANO_FRANKIVSK' THEN 'Івано-Франківськ'
    WHEN 'RIVNE'           THEN 'Рівне'
    WHEN 'ZHYTOMYR'        THEN 'Житомир'
    WHEN 'TERNOPIL'        THEN 'Тернопіль'
    WHEN 'LUTSK'           THEN 'Луцьк'
    WHEN 'UZHHOROD'        THEN 'Ужгород'
    WHEN 'CHERNIVTSI'      THEN 'Чернівці'
    WHEN 'KREMENCHUK'      THEN 'Кременчук'
    WHEN 'BILA_TSERKVA'    THEN 'Біла Церква'
    WHEN 'MELITOPOL'       THEN 'Мелітополь'
    WHEN 'MUKACHEVO'       THEN 'Мукачево'
    WHEN 'DROHOBYCH'       THEN 'Дрогобич'
    ELSE NULL
  END;
$$;

CREATE FUNCTION fts_car_color(v car_color) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE v
    WHEN 'WHITE'      THEN 'білий біла біле'
    WHEN 'BLACK'      THEN 'чорний чорна чорне'
    WHEN 'SILVER'     THEN 'срібний срібна срібне'
    WHEN 'GRAY'       THEN 'сірий сіра сіре'
    WHEN 'RED'        THEN 'червоний червона червоне'
    WHEN 'BLUE'       THEN 'синій синя сине блакитний'
    WHEN 'DARK_BLUE'  THEN 'темно-синій темно-синя'
    WHEN 'GREEN'      THEN 'зелений зелена зелене'
    WHEN 'DARK_GREEN' THEN 'темно-зелений темно-зелена'
    WHEN 'YELLOW'     THEN 'жовтий жовта жовте'
    WHEN 'ORANGE'     THEN 'помаранчевий помаранчева'
    WHEN 'BROWN'      THEN 'коричневий коричнева коричневе'
    WHEN 'BEIGE'      THEN 'бежевий бежева бежеве'
    WHEN 'PURPLE'     THEN 'фіолетовий фіолетова фіолетове'
    WHEN 'GOLDEN'     THEN 'золотий золота золоте'
    WHEN 'BURGUNDY'   THEN 'бордовий бордова бордове'
    ELSE NULL
  END;
$$;

CREATE FUNCTION fts_transmission_type(v transmission_type) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE v
    WHEN 'MANUAL'         THEN 'механіка механічна МКПП'
    WHEN 'AUTOMATIC'      THEN 'автомат автоматична АКПП'
    WHEN 'CVT'            THEN 'варіатор CVT'
    WHEN 'SEMI_AUTOMATIC' THEN 'робот роботизована'
    ELSE NULL
  END;
$$;

CREATE FUNCTION fts_fuel_type(v fuel_type) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE v
    WHEN 'PETROL'         THEN 'бензин бензиновий'
    WHEN 'DIESEL'         THEN 'дизель дизельний'
    WHEN 'LPG'            THEN 'газ LPG газовий'
    WHEN 'ELECTRIC'       THEN 'електро електричний електромобіль'
    WHEN 'HYBRID'         THEN 'гібрид гібридний'
    WHEN 'PLUG_IN_HYBRID' THEN 'гібрид plug-in підзарядний'
    ELSE NULL
  END;
$$;

CREATE FUNCTION fts_drive_type(v drive_type) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE v
    WHEN 'FWD'     THEN 'передній привід FWD'
    WHEN 'RWD'     THEN 'задній привід RWD'
    WHEN 'AWD'     THEN 'повний привід AWD'
    WHEN 'FOUR_WD' THEN 'повний привід 4WD 4x4'
    ELSE NULL
  END;
$$;

CREATE FUNCTION fts_body_type(v body_type) RETURNS text LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE v
    WHEN 'SEDAN'       THEN 'седан'
    WHEN 'HATCHBACK'   THEN 'хетчбек'
    WHEN 'WAGON'       THEN 'універсал'
    WHEN 'COUPE'       THEN 'купе'
    WHEN 'CONVERTIBLE' THEN 'кабріолет'
    WHEN 'SUV'         THEN 'позашляховик SUV'
    WHEN 'CROSSOVER'   THEN 'кросовер'
    WHEN 'MINIVAN'     THEN 'мінівен'
    WHEN 'PICKUP'      THEN 'пікап'
    WHEN 'VAN'         THEN 'мікроавтобус фургон'
    ELSE NULL
  END;
$$;

CREATE TABLE IF NOT EXISTS car_listings
(
    id                BIGSERIAL PRIMARY KEY,
    author_user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status            listing_status NOT NULL DEFAULT 'DRAFT',
    title             VARCHAR(200),
    description       TEXT,
    image_keys        TEXT[] NOT NULL DEFAULT '{}',
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
    published_at      BIGINT NOT NULL DEFAULT 0,
    search_vector     tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(fts_car_brand(brand), '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(custom_brand_name, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(model, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(fts_city(city), '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(fts_car_condition(condition), '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(fts_car_color(color), '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(fts_transmission_type(transmission), '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(fts_fuel_type(fuel_type), '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(fts_drive_type(drive_type), '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(fts_body_type(body_type), '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(description, '')), 'D')
    ) STORED
);

CREATE INDEX IF NOT EXISTS car_listings_search_idx ON car_listings USING GIN (search_vector);

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

CREATE TABLE IF NOT EXISTS pending_files_uploads
(
    key           TEXT NOT NULL,
    owner_user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    uploaded_at   BIGINT NOT NULL,
    PRIMARY KEY (key)
);
