#!/usr/bin/env python3
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
RU_PATH = ROOT / "composeApp/src/commonMain/composeResources/values/strings.xml"
UZ_PATH = ROOT / "composeApp/src/commonMain/composeResources/values-uz/strings.xml"
KT_PATH = ROOT / "composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/ui/i18n/StringCatalog.kt"

FULL = {
    "Нет соединения с интернетом.": "Internetga ulanish yo‘q.",
    "Не удалось подключиться к серверу.": "Serverga ulanib bo‘lmadi.",
    "Ошибка защищённого соединения.": "Himoyalangan ulanish xatosi.",
    "Ошибка запроса. Попробуйте позже.": "So‘rov xatosi. Keyinroq urinib ko‘ring.",
    "Не удалось отправить фото": "Fotosuratni yuborib bo‘lmadi",
    "Ошибка сервера. Повторите позже": "Server xatosi. Keyinroq qayta urinib ko‘ring",
    "Можно загрузить ещё %1$s фото": "Yana %1$s ta foto yuklash mumkin",
    "В документе уже %1$s из %2$s": "Hujjatda allaqachon %1$s / %2$s",
    "Лимит фото для документа исчерпан (%1$s).": "Hujjat uchun foto limiti tugagan (%1$s).",
    "Папка документа недоступна. Проверьте номер или попробуйте позже.": "Hujjat papkasi mavjud emas. Raqamni tekshiring yoki keyinroq urinib ko‘ring.",
    "Загрузка сейчас недоступна.": "Hozir yuklash mavjud emas.",
    "Можно выбрать ещё не больше %1$s фото.": "Yana ko‘pi bilan %1$s ta foto tanlash mumkin.",
    "Сервис временно недоступен. Попробуйте позже.": "Xizmat vaqtincha mavjud emas. Keyinroq urinib ko‘ring.",

    "В папке документа: %1$s из %2$s": "Hujjat papkasida: %1$s / %2$s",
    "В этом запуске приложения: %1$s из %2$s": "Ushbu ilova ishga tushishida: %1$s / %2$s",
    "Можно загрузить сейчас: %1$s": "Hozir yuklash mumkin: %1$s",
    "В этой сессии уже загружено максимум %1$s фото для этого документа. Перезапустите приложение, чтобы начать новую сессию.": "Ushbu sessiyada bu hujjat uchun maksimal %1$s ta foto yuklangan. Yangi sessiyani boshlash uchun ilovani qayta ishga tushiring.",
    "Конфликт повторной отправки. Попробуйте ещё раз.": "Qayta yuborish konflikti. Qayta urinib ko‘ring.",
    "Клиент несовместим с API ImageMediator. Обновите приложение.": "Mijoz ImageMediator API bilan mos emas. Ilovani yangilang.",
    "Не удалось однозначно найти папку документа. Передайте полный номер и период.": "Hujjat papkasini aniq topib bo‘lmadi. To‘liq raqam va davrni yuboring.",
    "Недостаточно данных документа. Укажите полный номер, тип и период.": "Hujjat ma’lumotlari yetarli emas. To‘liq raqam, tur va davrni ko‘rsating.",
    "Превышен лимит запросов. Подождите и повторите позже.": "So‘rovlar limiti oshib ketdi. Kuting va keyinroq qayta urinib ko‘ring.",
    "Тип документа": "Hujjat turi",

    "Загрузка недоступна. Осталось фото: %1$s": "Yuklash mavjud emas. Qolgan foto: %1$s",
    "Загрузка недоступна для этого документа": "Bu hujjat uchun yuklash mavjud emas",
    "Загрузка недоступна": "Yuklash mavjud emas",
    "Превышено время ожидания. Попробуйте ещё раз.": "Kutish vaqti tugadi. Qayta urinib ko‘ring.",
    "Доступ запрещён. Войдите заново.": "Kirish taqiqlangan. Qaytadan kiring.",
    "Сервер недоступен. Проверьте подключение.": "Server mavjud emas. Ulanishni tekshiring.",
    "Соединение потеряно. Повторите попытку.": "Aloqa uzildi. Qayta urinib ko‘ring.",
    "Не найдено.": "Topilmadi.",
    "Сервер временно недоступен.": "Server vaqtincha mavjud emas.",
    "Произошла ошибка": "Xatolik yuz berdi",
    "Не удалось принять файл. Проверьте формат и размер.": "Faylni qabul qilib bo‘lmadi. Format va hajmni tekshiring.",
    "Сессия истекла. Войдите заново": "Sessiya muddati tugadi. Qaytadan kiring",
    "Папка документа недоступна. Проверьте номер или попробуйте позже.": "Hujjat papkasi mavjud emas. Raqamni tekshiring yoki keyinroq urinib ko‘ring.",
    "Файл или запрос слишком большой.": "Fayl yoki so‘rov juda katta.",
    "Сервер занят. Повторите через несколько секунд": "Server band. Bir necha soniyadan so‘ng qayta urinib ko‘ring",
    "Вверх": "Yuqoriga",
    "Вниз": "Pastga",
    "Влево": "Chapga",
    "Вправо": "O‘ngga",
    "Ошибка входа": "Kirish xatosi",
    "Ошибка соединения": "Ulanish xatosi",
    "Загрузка ...": "Yuklanmoqda ...",
    "Пустой токен от сервера": "Serverdan bo‘sh token",
    "Ошибка авторизации": "Avtorizatsiya xatosi",
    "Пустой токен": "Bo‘sh token",
    "Не удалось загрузить права доступа": "Kirish huquqlarini yuklab bo‘lmadi",
    "Скрыть": "Yashirish",
    "Показать": "Ko‘rsatish",
    "Войти": "Kirish",
    "Повторить": "Qayta urinish",
    "ОК": "OK",
    "Сервер недоступен или превышено время ожидания": "Server mavjud emas yoki kutish vaqti tugadi",
    "Логин": "Login",
    "Пароль": "Parol",
    "Версия: %1$s": "Versiya: %1$s",
    "Код ошибки не известен": "Xato kodi noma’lum",
    "Меню": "Menyu",
    "Обновить": "Yangilash",
    "Настройки": "Sozlamalar",
    "Доступна версия %1$s": "Mavjud versiya %1$s",
    "Проверить": "Tekshirish",
    "Установить": "O‘rnatish",
    "Отменить": "Bekor qilish",
    "Позже": "Keyinroq",
    "Каталог": "Katalog",
    "Обновления Desktop": "Desktop yangilanishlari",
    "Текущая версия: %1$s": "Joriy versiya: %1$s",
    "Последняя версия: %1$s": "So‘nggi versiya: %1$s",
    "Установлена актуальная версия. Обновление не требуется.": "Joriy versiya o‘rnatilgan. Yangilanish shart emas.",
    "(обязательное)": "(majburiy)",
    "Размер: %1$s МБ": "Hajmi: %1$s MB",
    "События": "Hodisalar",
    "Заказ-Наряды": "Buyurtma-naryadlar",
    "Комплектация": "Komplektatsiya",
    "Доставки": "Yetkazib berishlar",
    "Заказы покуп.": "Xarid buyurtmalari",
    "Заказы пост.": "Ta’minotchi buyurtmalari",
    "Рекламации": "Reklamatsiyalar",
    "Внутр. заказы": "Ichki buyurtmalar",
    "Входящие Заявки": "Kiruvchi arizalar",
    "Калькуляция": "Kalkulyatsiya",
    "Заявки расход ДС": "Pul sarfi arizalari",
    "QR Сканер": "QR skaner",
    "Язык": "Til",
    "Русский": "Русский",
    "Oʻzbekcha": "Oʻzbekcha",
    "Тема приложения": "Ilova mavzusi",
    "Панель навигации": "Navigatsiya paneli",
    "Порядок и видимость иконок в нижнем меню": "Pastki menyudagi ikonkalar tartibi va ko‘rinishi",
    "Уведомления": "Bildirishnomalar",
    "Push-уведомления и mute по типам документов": "Push-bildirishnomalar va hujjat turlari bo‘yicha ovozsizlash",
    "Очистить кэш фотографий": "Foto keshini tozalash",
    "Загруженные фото документов на устройстве": "Qurilmadagi yuklangan hujjat fotolari",
    "Инструменты разработчика": "Dasturchi vositalari",
    "Выйти": "Chiqish",
    "Завершить сессию": "Sessiyani tugatish",
    "Выход из аккаунта": "Akkauntdan chiqish",
    "Вы уверены, что хотите выйти из аккаунта?": "Akkauntdan chiqishni xohlaysizmi?",
    "Да": "Ha",
    "Отмена": "Bekor qilish",
    "Очистить": "Tozalash",
    "Удалить загруженные фото документов с устройства?": "Qurilmadan yuklangan hujjat fotolarini o‘chirish?",
    "Назад": "Orqaga",
    "Сохранить": "Saqlash",
    "Главная": "Asosiy",
    "Документ не найден": "Hujjat topilmadi",
    "Требуется обновление": "Yangilanish talab qilinadi",
    "Подождите...": "Kuting...",
    "Developer режим включён": "Developer rejimi yoqilgan",
    "Developer режим выключен": "Developer rejimi o‘chirilgan",
    "Подразделение: %1$s": "Bo‘lim: %1$s",
    "Нет активной сессии. Перезайдите в приложение.": "Faol sessiya yo‘q. Ilovaga qayta kiring.",
    "Не удалось загрузить настройки уведомлений": "Bildirishnoma sozlamalarini yuklab bo‘lmadi",
    "Не удалось обновить общий mute": "Umumiy ovozsizlashni yangilab bo‘lmadi",
    "Не удалось обновить mute по типу": "Tur bo‘yicha ovozsizlashni yangilab bo‘lmadi",
    "Не удалось очистить кэш фотографий": "Foto keshini tozalab bo‘lmadi",
    "Ошибка авторизации. Перезайдите в приложение.": "Avtorizatsiya xatosi. Ilovaga qayta kiring.",
    "Не удалось обновить настройки уведомлений": "Bildirishnoma sozlamalarini yangilab bo‘lmadi",
    "Камера фиксатор": "Kamera fiksator",
    "Обновить настройки уведомлений": "Bildirishnoma sozlamalarini yangilash",
    "Скрыть из меню": "Menyudan yashirish",
    "Показать в меню": "Menyuda ko‘rsatish",
    "Сессия истекла. Перезайдите в приложение.": "Sessiya muddati tugadi. Ilovaga qayta kiring.",
    "Нет токена авторизации. Войдите заново.": "Avtorizatsiya tokeni yo‘q. Qaytadan kiring.",
    "Некорректный номер документа": "Hujjat raqami noto‘g‘ri",
    "Не удалось проверить возможность загрузки": "Yuklash imkoniyatini tekshirib bo‘lmadi",
    "Номер документа: 6–12 цифр": "Hujjat raqami: 6–12 ta raqam",
    "Нет фотографий для отправки": "Yuborish uchun fotosuratlar yo‘q",
    "Поиск": "Qidiruv",
    "Фильтр": "Filtr",
    "Сбросить": "Tozalash",
    "Применить": "Qo‘llash",
    "Закрыть": "Yopish",
    "Отправить": "Yuborish",
    "Загрузка": "Yuklanmoqda",
    "Ошибка": "Xato",
    "Успешно": "Muvaffaqiyatli",
    "Сообщение": "Xabar",
    "Комментарий": "Izoh",
    "Номер": "Raqam",
    "Дата": "Sana",
    "Статус": "Holat",
    "Клиент": "Mijoz",
    "Документ": "Hujjat",
    "Фото": "Foto",
    "Камера": "Kamera",
    "Галерея": "Galereya",
    "Разрешить": "Ruxsat berish",
    "Нет данных": "Ma’lumot yo‘q",
    "Выберите": "Tanlang",
    "Добавить": "Qo‘shish",
    "Удалить": "O‘chirish",
    "Изменить": "O‘zgartirish",
    "Копировать": "Nusxalash",
    "OK": "OK",
    "System": "System",
    "Light": "Light",
    "Dark": "Dark",
    "Автомобиль": "Avtomobil",
    "Хар. комплекта": "Komplekt xarakteristikasi",
    "Госномер": "Davlat raqami",
    "Вид ремонта": "Ta’mir turi",
    "Заказчик": "Buyurtmachi",
    "Маршрут": "Marshrut",
    "Перевозчик": "Tashuvchi",
    "Телефон": "Telefon",
    "Пользователи": "Foydalanuvchilar",
    "ответственный": "mas’ul",
    "Задачи (кол-во: %1$s)": "Vazifalar (soni: %1$s)",
    "Товары (Шт: %1$s, Сумма: %2$s руб.)": "Tovarlar (dona: %1$s, summa: %2$s so‘m)",
    " (я)": " (men)",
    "Роль: %1$s": "Rol: %1$s",
    "Задача": "Vazifa",
    "Автор: %1$s": "Muallif: %1$s",
    "Цена: %1$s": "Narx: %1$s",
    "Товар %1$s": "Tovar %1$s",
    "Сумма: %1$s": "Summa: %1$s",
    "Комментарий сохранён, уведомление не отправлено": "Izoh saqlandi, bildirishnoma yuborilmadi",
    "Подождите, выполняем поиск.": "Kuting, qidiruv bajarilmoqda.",
    "сумма": "summa",
    "Ячейка: %1$s": "Katak: %1$s",
    "Работа": "Ish",
    "Исполнитель": "Bajaruvchi",
    "Количество: ": "Miqdor: ",
    "Норма вр. (ч.): ": "Vaqt me’yori (soat): ",
    "Цена: ": "Narx: ",
    "Сумма: ": "Summa: ",
    "Стр. %1$s": "Qator %1$s",
    "Название комплекта…": "Komplekt nomi…",
    "Номер документа…": "Hujjat raqami…",
    "Мастер…": "Usta…",
    "С/Н…": "S/N…",
    "По с/н": "S/N bo‘yicha",
    "%1$s шт": "%1$s dona",
    "%1$s (%2$s ч)": "%1$s (%2$s soat)",
    "Продолжительность: %1$s ч": "Davomiyligi: %1$s soat",
    "Показаны все %1$s записей": "Barcha %1$s yozuv ko‘rsatilgan",
    "Показаны последние 10 из %1$s": "So‘nggi 10 ta ko‘rsatilgan, jami %1$s",
    "Фотографий не загружено": "Fotolar yuklanmagan",
    "Товары (Сумма: %1$s руб.)": "Tovarlar (summa: %1$s so‘m)",
    "Событие": "Hodisa",
    "Заказ-наряд": "Buyurtma-naryad",
    "Рекламация": "Reklamatsiya",
    "Заказ внутренний": "Ichki buyurtma",
    "Заказ покупателя": "Xarid buyurtmasi",
    "Заказ поставщику": "Ta’minotchiga buyurtma",
    "Груз": "Yuk",
    "Заявка на расход ДС": "Pul sarfi arizasi",
}

PHRASES = [
    ("Не удалось", "Amalga oshmadi"),
    ("не удалось", "amalga oshmadi"),
    ("Ошибка", "Xato"),
    ("ошибка", "xato"),
    ("Документ", "Hujjat"),
    ("документ", "hujjat"),
    ("Фотографи", "Fotosurat"),
    ("фотографи", "fotosurat"),
    ("Фото", "Foto"),
    ("фото", "foto"),
    ("Камера", "Kamera"),
    ("камера", "kamera"),
    ("Сохранить", "Saqlash"),
    ("сохранить", "saqlash"),
    ("Отмена", "Bekor qilish"),
    ("Отменить", "Bekor qilish"),
    ("Повторить", "Qayta urinish"),
    ("Сервер", "Server"),
    ("сервер", "server"),
    ("Сессия", "Sessiya"),
    ("сессия", "sessiya"),
    ("Войти", "Kirish"),
    ("войти", "kirish"),
    ("Выйти", "Chiqish"),
    ("Поиск", "Qidiruv"),
    ("поиск", "qidiruv"),
    ("Фильтр", "Filtr"),
    ("фильтр", "filtr"),
    ("Сообщение", "Xabar"),
    ("сообщение", "xabar"),
    ("Успешно", "Muvaffaqiyatli"),
    ("успешно", "muvaffaqiyatli"),
    ("Попробуйте", "Urinib ko‘ring"),
    ("попробуйте", "urinib ko‘ring"),
    ("Позже", "Keyinroq"),
    ("позже", "keyinroq"),
    ("Назад", "Orqaga"),
    ("Закрыть", "Yopish"),
    ("Удалить", "O‘chirish"),
    ("Очистить", "Tozalash"),
    ("Обновить", "Yangilash"),
    ("Отправить", "Yuborish"),
    ("Выберите", "Tanlang"),
    ("заново", "qaytadan"),
    ("приложение", "ilova"),
    ("Приложение", "Ilova"),
    ("уведомлен", "bildirishnoma"),
    ("Уведомлен", "Bildirishnoma"),
    ("настройк", "sozlama"),
    ("Настройк", "Sozlama"),
    ("Загрузка", "Yuklash"),
    ("загрузка", "yuklash"),
    ("доступ", "kirish"),
    ("Доступ", "Kirish"),
]


def translate(ru: str) -> str:
    if ru in FULL:
        return FULL[ru]
    out = ru
    for a, b in sorted(PHRASES, key=lambda x: -len(x[0])):
        out = out.replace(a, b)
    return out


def unescape(s: str) -> str:
    return (
        s.replace("\\'", "'")
        .replace('\\"', '"')
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
    )


def escape_xml(s: str) -> str:
    return (
        s.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace('"', '\\"')
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )


def kt_escape(s: str) -> str:
    dollar = "${'$'}"
    return (
        s.replace("\\", "\\\\")
        .replace('"', '\\"')
        .replace("$", dollar)
        .replace("\n", "\\n")
    )



def parse_strings_xml(path: pathlib.Path) -> list[tuple[str, str]]:
    text = path.read_text(encoding="utf-8")
    items: list[tuple[str, str]] = []
    for m in re.finditer(
        r'<string\s+name="([a-zA-Z_][a-zA-Z0-9_]*)">(.*?)</string>',
        text,
        flags=re.DOTALL,
    ):
        items.append((m.group(1), unescape(m.group(2))))
    return items


def main() -> None:
    items = []
    for key, ru in parse_strings_xml(RU_PATH):
        uz = translate(ru)
        items.append((key, ru, uz))

    lines = ['<?xml version="1.0" encoding="UTF-8" ?>', "<resources>"]
    for key, _, uz in items:
        lines.append(f'    <string name="{key}">{escape_xml(uz)}</string>')
    lines.append("</resources>")
    UZ_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")

    out = [
        "package com.tagaev.trrcrm.ui.i18n",
        "",
        "/** Generated from composeResources strings. Used outside Compose. */",
        "object StringCatalog {",
        "    private val ru: Map<String, String> = mapOf(",
    ]
    for key, ru, _ in items:
        out.append(f'        "{key}" to "{kt_escape(ru)}",')
    out.append("    )")
    out.append("    private val uz: Map<String, String> = mapOf(")
    for key, _, uz in items:
        out.append(f'        "{key}" to "{kt_escape(uz)}",')
    out.append("    )")
    out.extend(
        [
            "",
            "    fun get(key: String, language: AppLanguage, vararg args: Any): String {",
            "        val raw = when (language) {",
            "            AppLanguage.Uzbek -> uz[key] ?: ru[key] ?: key",
            "            AppLanguage.Russian -> ru[key] ?: key",
            "        }",
            "        if (args.isEmpty()) return raw",
            "        var result = raw",
            "        args.forEachIndexed { index, arg ->",
            '            val token = "%${index + 1}$s"',
            "            result = result.replace(token, arg.toString())",
            "        }",
            "        return result",
            "    }",
            "}",
            "",
        ]
    )
    # Fix the token line - Python f-string ate $
    out = [
        line.replace('val token = "%${index + 1}$s"', 'val token = "%" + (index + 1) + "\\$s"')
        for line in out
    ]
    KT_PATH.write_text("\n".join(out), encoding="utf-8")
    print(f"wrote {len(items)} strings to {UZ_PATH.name} and {KT_PATH.name}")


if __name__ == "__main__":
    main()
