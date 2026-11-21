КАК СОБРАТЬ APK С ТЕЛЕФОНА
1. Создай аккаунт на github.com, новый репозиторий, загрузи туда все файлы из этого архива (Add file > Upload files; папку .github тоже).
2. Вкладка Actions > Build APK > Run workflow. Через ~5 минут внизу страницы появится артефакт FazerMobile-apk. Скачай и установи.

ПЕРЕД СБОРКОЙ:
- В MainActivity.kt замени YOUR_HOSTING на адрес своего хостинга.
- server-files/launcher.json залей на хостинг по FTP, поправь ip, port, client_package (пакет твоего SA-MP клиента), client_url.
