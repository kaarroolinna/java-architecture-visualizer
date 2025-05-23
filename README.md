# Java Architecture Visualizer

## Що робить програма?

Java Architecture Visualizer аналізує ваш Java-проект (пакети, класи, інтерфейси та їх залежності) і виводить інтерактивну діаграму:

**Пакети** розташовуються по колу, розмальовані та з підписами.

**Метрики** в правій верхній панелі:
гістограма `outgoing` vs `incoming` залежностей, кругова діаграма «Hot Spots» (топ-3 за сумою in+out), максимальна глибина ієрархії пакетів

**Детальна структура** праворуч унизу: дерево пакети→класи→методи/поля із контекстним меню: **Open Source File** — відкрити файл у зовнішньому редакторі, **Go to Definition** — підсвітити пакет на діаграмі, **Copy Full Name** — скопіювати повне ім’я

**Крім того**, програма підтримує **Export**: у PDF та GraphML через меню File → Export

---

## Як користуватись програмою?

1. **Відкрити проект**  
   При запуску виберіть у діалоговому вікні кореневу директорію вашого Java-проекту (або JAR).

2. **Інтерактивність**
    - Клацніть по пакету, щоб побачити коротку інформацію
    - У дереві структури клацніть правою кнопкою на метод/поле/клас для дій
   
3. **Експорт**  
   Menu → File → Export → (PDF / GraphML)
---

## 3. Як запускати?

### Вимоги

- JDK 17+
- Gradle 8+

### Інструкція

1. Клонуйте репозиторій:
   ```
   git clone https://github.com/Lamarrrk/java-arch-visualizer.git
   cd java-arch-visualizer
   
2. Введіть команди:
   ```
   ./gradlew clean build
   ./gradlew run